package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class FurnitureGeometryAnalyzer {
    private static final float Z_FIGHT_MARGIN = 0.01f;

    private final Map<FurnitureData.Element, Map<Direction, FaceGeometry>> faceGeometry = new IdentityHashMap<>();
    private final Map<FurnitureData.Element, Bounds> elementBounds = new IdentityHashMap<>();
    private final SpatialIndex spatialIndex;

    FurnitureGeometryAnalyzer(List<FurnitureData.Element> elements) {
        for (FurnitureData.Element element : elements) {
            if (!hasFaces(element)) continue;

            float[] shape = ClientModelUtils.getShapeData(element);
            Map<Direction, FaceGeometry> faces = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                Vector3f[] vertices = ClientModelUtils.getVertices(element, direction, shape, null);
                faces.put(direction, new FaceGeometry(vertices, Bounds.of(vertices)));
            }
            faceGeometry.put(element, faces);
            elementBounds.put(element, Bounds.ofVoxelSpace(ModelUtils.getCorners(element)));
        }

        spatialIndex = new SpatialIndex(elements, elementBounds);
    }

    Direction getCullDirection(FurnitureData.Element element, Direction direction) {
        Vector3f[] vertices = getFace(element, direction).vertices;
        if (onPlane(vertices, Direction.Axis.X, 0.0f)) return Direction.WEST;
        if (onPlane(vertices, Direction.Axis.X, 1.0f)) return Direction.EAST;
        if (onPlane(vertices, Direction.Axis.Y, 0.0f)) return Direction.DOWN;
        if (onPlane(vertices, Direction.Axis.Y, 1.0f)) return Direction.UP;
        if (onPlane(vertices, Direction.Axis.Z, 0.0f)) return Direction.NORTH;
        if (onPlane(vertices, Direction.Axis.Z, 1.0f)) return Direction.SOUTH;
        return null;
    }

    boolean isFaceFullyContained(FurnitureData.Element element, Direction direction, int state) {
        FaceGeometry face = getFace(element, direction);
        for (FurnitureData.Element other : spatialIndex.query(face.bounds, 0.0f)) {
            if (other == element || !other.isMasked(state)) continue;
            if (other.type != FurnitureData.ElementType.ELEMENT) continue;
            if (other.material.transparency != TransparencyType.SOLID) continue;
            if (sameGeometry(element, other) && other.hashCode() < element.hashCode()) continue;
            if (!elementBounds.get(other).contains(face.bounds)) continue;
            if (fullyContained(other, face.vertices)) return true;
        }
        return false;
    }

    boolean mightZFight(
            FurnitureData.Element element,
            Map<Direction, BlockElementFace> elementFaces,
            Map<FurnitureData.Element, Map<Direction, BlockElementFace>> stateFaces
    ) {
        Vector3f firstVertex = new Vector3f();
        Vector3f oppositeVertex = new Vector3f();

        for (Direction direction : elementFaces.keySet()) {
            FaceGeometry face = getFace(element, direction);
            for (FurnitureData.Element other : spatialIndex.query(face.bounds, Z_FIGHT_MARGIN)) {
                if (other == element || other.getVolume() < element.getVolume()) continue;

                Map<Direction, BlockElementFace> otherFaces = stateFaces.getOrDefault(other, Collections.emptyMap());
                if (otherFaces.isEmpty() || !elementBounds.get(other).intersects(face.bounds, Z_FIGHT_MARGIN)) continue;

                ElementRotation rotation = other.getRotation();
                Quaternionf inverseRotation = ModelUtils.getElementRotation(rotation).conjugate();
                toLocalVoxelSpace(firstVertex, face.vertices[0], rotation, inverseRotation);
                toLocalVoxelSpace(oppositeVertex, face.vertices[2], rotation, inverseRotation);
                Bounds localFace = Bounds.between(firstVertex, oppositeVertex);

                if (overlaps(localFace.minY, localFace.maxY, other.from.y, other.to.y) &&
                    overlaps(localFace.minZ, localFace.maxZ, other.from.z, other.to.z) &&
                    coplanar(localFace.minX, localFace.maxX, other.from.x, other.to.x,
                            Direction.WEST, Direction.EAST, otherFaces)) {
                    return true;
                }
                if (overlaps(localFace.minX, localFace.maxX, other.from.x, other.to.x) &&
                    overlaps(localFace.minZ, localFace.maxZ, other.from.z, other.to.z) &&
                    coplanar(localFace.minY, localFace.maxY, other.from.y, other.to.y,
                            Direction.DOWN, Direction.UP, otherFaces)) {
                    return true;
                }
                if (overlaps(localFace.minX, localFace.maxX, other.from.x, other.to.x) &&
                    overlaps(localFace.minY, localFace.maxY, other.from.y, other.to.y) &&
                    coplanar(localFace.minZ, localFace.maxZ, other.from.z, other.to.z,
                            Direction.NORTH, Direction.SOUTH, otherFaces)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasFaces(FurnitureData.Element element) {
        return element.type == FurnitureData.ElementType.ELEMENT || element.type == FurnitureData.ElementType.SPRITE;
    }

    private FaceGeometry getFace(FurnitureData.Element element, Direction direction) {
        return faceGeometry.get(element).get(direction);
    }

    private static boolean fullyContained(FurnitureData.Element element, Vector3f[] vertices) {
        ElementRotation rotation = element.getRotation();
        Quaternionf inverseRotation = ModelUtils.getElementRotation(rotation).conjugate();
        Vector3f localVertex = new Vector3f();
        for (Vector3f vertex : vertices) {
            toLocalVoxelSpace(localVertex, vertex, rotation, inverseRotation);
            if (!element.contains(localVertex)) return false;
        }
        return true;
    }

    private static boolean sameGeometry(FurnitureData.Element first, FurnitureData.Element second) {
        return first.from.equals(second.from) &&
               first.to.equals(second.to) &&
               first.getRotation().equals(second.getRotation());
    }

    private static boolean onPlane(Vector3f[] vertices, Direction.Axis axis, float coordinate) {
        for (Vector3f vertex : vertices) {
            if (vertex.get(axis.ordinal()) != coordinate) return false;
        }
        return true;
    }

    private static boolean overlaps(float firstMin, float firstMax, float secondMin, float secondMax) {
        return firstMin < secondMax - Z_FIGHT_MARGIN && firstMax > secondMin + Z_FIGHT_MARGIN;
    }

    private static boolean coplanar(
            float faceMin,
            float faceMax,
            float elementMin,
            float elementMax,
            Direction minFace,
            Direction maxFace,
            Map<Direction, BlockElementFace> faces
    ) {
        if (Math.abs(faceMin - faceMax) >= Z_FIGHT_MARGIN) return false;
        return faces.containsKey(minFace) && Math.abs(faceMin - elementMin) < Z_FIGHT_MARGIN ||
               faces.containsKey(maxFace) && Math.abs(faceMax - elementMax) < Z_FIGHT_MARGIN;
    }

    private static void toLocalVoxelSpace(
            Vector3f target,
            Vector3f source,
            ElementRotation rotation,
            Quaternionf inverseRotation
    ) {
        target.set(source).sub(rotation.origin());
        inverseRotation.transform(target);
        target.add(rotation.origin()).mul(16.0f);
    }

    private record FaceGeometry(Vector3f[] vertices, Bounds bounds) {
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        private static Bounds of(Vector3f[] vertices) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (Vector3f vertex : vertices) {
                minX = Math.min(minX, vertex.x);
                minY = Math.min(minY, vertex.y);
                minZ = Math.min(minZ, vertex.z);
                maxX = Math.max(maxX, vertex.x);
                maxY = Math.max(maxY, vertex.y);
                maxZ = Math.max(maxZ, vertex.z);
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private static Bounds ofVoxelSpace(Vector3f[] vertices) {
            Bounds bounds = of(vertices);
            return new Bounds(bounds.minX / 16.0f, bounds.minY / 16.0f, bounds.minZ / 16.0f,
                    bounds.maxX / 16.0f, bounds.maxY / 16.0f, bounds.maxZ / 16.0f);
        }

        private static Bounds between(Vector3f first, Vector3f second) {
            return new Bounds(
                    Math.min(first.x, second.x), Math.min(first.y, second.y), Math.min(first.z, second.z),
                    Math.max(first.x, second.x), Math.max(first.y, second.y), Math.max(first.z, second.z)
            );
        }

        private boolean contains(Bounds other) {
            float epsilon = 0.0001f;
            return minX <= other.minX + epsilon && minY <= other.minY + epsilon && minZ <= other.minZ + epsilon &&
                   maxX >= other.maxX - epsilon && maxY >= other.maxY - epsilon && maxZ >= other.maxZ - epsilon;
        }

        private boolean intersects(Bounds other, float margin) {
            return maxX + margin >= other.minX && minX - margin <= other.maxX &&
                   maxY + margin >= other.minY && minY - margin <= other.maxY &&
                   maxZ + margin >= other.minZ && minZ - margin <= other.maxZ;
        }
    }

    private static final class SpatialIndex {
        private static final float BUCKET_SIZE = 0.25f;

        private final Map<BucketKey, List<IndexedElement>> buckets = new HashMap<>();
        private final List<FurnitureData.Element> queryResult = new ArrayList<>();
        private final int[] visitStamp;
        private int queryStamp;

        private SpatialIndex(List<FurnitureData.Element> elements, Map<FurnitureData.Element, Bounds> bounds) {
            visitStamp = new int[elements.size()];
            int index = 0;
            for (FurnitureData.Element element : elements) {
                Bounds elementBounds = bounds.get(element);
                int elementIndex = index++;
                if (elementBounds == null) continue;
                IndexedElement indexedElement = new IndexedElement(elementIndex, element);

                for (int x = bucket(elementBounds.minX); x <= bucket(elementBounds.maxX); x++) {
                    for (int y = bucket(elementBounds.minY); y <= bucket(elementBounds.maxY); y++) {
                        for (int z = bucket(elementBounds.minZ); z <= bucket(elementBounds.maxZ); z++) {
                            buckets.computeIfAbsent(new BucketKey(x, y, z), ignored -> new ArrayList<>()).add(indexedElement);
                        }
                    }
                }
            }
        }

        private List<FurnitureData.Element> query(Bounds bounds, float margin) {
            queryResult.clear();
            if (++queryStamp == 0) {
                Arrays.fill(visitStamp, 0);
                queryStamp = 1;
            }
            for (int x = bucket(bounds.minX - margin); x <= bucket(bounds.maxX + margin); x++) {
                for (int y = bucket(bounds.minY - margin); y <= bucket(bounds.maxY + margin); y++) {
                    for (int z = bucket(bounds.minZ - margin); z <= bucket(bounds.maxZ + margin); z++) {
                        List<IndexedElement> candidates = buckets.get(new BucketKey(x, y, z));
                        if (candidates == null) continue;
                        for (IndexedElement candidate : candidates) {
                            if (visitStamp[candidate.index] != queryStamp) {
                                visitStamp[candidate.index] = queryStamp;
                                queryResult.add(candidate.element);
                            }
                        }
                    }
                }
            }
            return queryResult;
        }

        private static int bucket(float coordinate) {
            return (int) Math.floor(coordinate / BUCKET_SIZE);
        }
    }

    private record BucketKey(int x, int y, int z) {
    }

    private record IndexedElement(int index, FurnitureData.Element element) {
    }
}
