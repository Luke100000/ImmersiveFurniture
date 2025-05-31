package immersive_furniture.client.model;

import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import org.joml.*;

import java.lang.Math;

public class ModelUtils {
    public static Vector2i getFaceDimensions(FurnitureData.Element element, Direction direction) {
        switch (direction) {
            case UP, DOWN -> {
                return new Vector2i(
                        (int) (element.to.x - element.from.x),
                        (int) (element.to.z - element.from.z)
                );
            }
            case NORTH, SOUTH -> {
                return new Vector2i(
                        (int) (element.to.x - element.from.x),
                        (int) (element.to.y - element.from.y)
                );
            }
            case WEST, EAST -> {
                return new Vector2i(
                        (int) (element.to.z - element.from.z),
                        (int) (element.to.y - element.from.y)
                );
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }

    public static Vector3i to3D(FurnitureData.Element element, Direction direction, int x, int y) {
        switch (direction) {
            case UP -> {
                return new Vector3i(
                        (int) element.from.x + x,
                        (int) element.to.y,
                        (int) element.from.z + y
                );
            }
            case DOWN -> {
                return new Vector3i(
                        (int) element.from.x + x,
                        (int) element.from.y,
                        (int) element.to.z - y
                );
            }
            case NORTH -> {
                return new Vector3i(
                        (int) element.to.x - x,
                        (int) element.to.y - y,
                        (int) element.from.z
                );
            }
            case SOUTH -> {
                return new Vector3i(
                        (int) element.from.x + x,
                        (int) element.to.y - y,
                        (int) element.to.z
                );
            }
            case WEST -> {
                return new Vector3i(
                        (int) element.from.x,
                        (int) element.to.y - y,
                        (int) element.from.z + x
                );
            }
            case EAST -> {
                return new Vector3i(
                        (int) element.to.x,
                        (int) element.to.y - y,
                        (int) element.to.z - x
                );
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }

    public static Quaternionf getElementRotation(BlockElementRotation rotation) {
        Vector3f axis = switch (rotation.axis()) {
            case X -> new Vector3f(1.0f, 0.0f, 0.0f);
            case Y -> new Vector3f(0.0f, 1.0f, 0.0f);
            case Z -> new Vector3f(0.0f, 0.0f, 1.0f);
        };

        return new Quaternionf().rotationAxis(rotation.angle() * ((float) Math.PI / 180), axis);
    }

    // Expects pos to be in voxel space (16 units per block)
    public static void applyElementRotation(Vector3f pos, BlockElementRotation rotation) {
        if (rotation == null) return;
        Quaternionf quaternionf = getElementRotation(rotation);
        pos.mul(1.0f / 16.0f);
        pos.sub(rotation.origin());
        quaternionf.transform(pos);
        pos.add(rotation.origin());
        pos.mul(16.0f);
    }

    // Expects pos to be in voxel space (16 units per block)
    public static void applyInverseElementRotation(Vector3f pos, BlockElementRotation rotation) {
        if (rotation == null) return;
        Quaternionf quaternionf = getElementRotation(rotation).conjugate();
        pos.mul(1.0f / 16.0f);
        pos.sub(rotation.origin());
        quaternionf.transform(pos);
        pos.add(rotation.origin());
        pos.mul(16.0f);
    }

    public static float[] getShapeData(FurnitureData.Element element) {
        float[] fs = new float[Direction.values().length];
        fs[FaceInfo.Constants.MIN_X] = element.from.x() / 16.0f;
        fs[FaceInfo.Constants.MIN_Y] = element.from.y() / 16.0f;
        fs[FaceInfo.Constants.MIN_Z] = element.from.z() / 16.0f;
        fs[FaceInfo.Constants.MAX_X] = element.to.x() / 16.0f;
        fs[FaceInfo.Constants.MAX_Y] = element.to.y() / 16.0f;
        fs[FaceInfo.Constants.MAX_Z] = element.to.z() / 16.0f;
        return fs;
    }

    public static Vector3f getVertex(Direction facing, int vertex, float[] fs, BlockElementRotation rotation, Matrix4f transform) {
        FaceInfo.VertexInfo vertexInfo = FaceInfo.fromFacing(facing).getVertexInfo(vertex);
        Vector3f vec = new Vector3f(fs[vertexInfo.xFace], fs[vertexInfo.yFace], fs[vertexInfo.zFace]);

        if (rotation != null) {
            Quaternionf quaternionf = getElementRotation(rotation);
            vec.sub(rotation.origin());
            quaternionf.transform(vec);
            vec.add(rotation.origin());
        }

        if (transform != null) {
            transform.transformPosition(vec);
        }

        return vec;
    }

    public static Vector3f[] getVertices(FurnitureData.Element element, Direction facing, float[] fs, Matrix4f transform) {
        return new Vector3f[]{
                ModelUtils.getVertex(facing, 0, fs, element.getRotation(), transform),
                ModelUtils.getVertex(facing, 1, fs, element.getRotation(), transform),
                ModelUtils.getVertex(facing, 2, fs, element.getRotation(), transform),
                ModelUtils.getVertex(facing, 3, fs, element.getRotation(), transform)
        };
    }

    public static Vector3f[] getCorners(FurnitureData.Element element) {
        Vector3f[] corners = new Vector3f[8];
        corners[0] = new Vector3f(element.from.x(), element.from.y(), element.from.z());
        corners[1] = new Vector3f(element.from.x(), element.from.y(), element.to.z());
        corners[2] = new Vector3f(element.from.x(), element.to.y(), element.from.z());
        corners[3] = new Vector3f(element.from.x(), element.to.y(), element.to.z());
        corners[4] = new Vector3f(element.to.x(), element.from.y(), element.from.z());
        corners[5] = new Vector3f(element.to.x(), element.from.y(), element.to.z());
        corners[6] = new Vector3f(element.to.x(), element.to.y(), element.from.z());
        corners[7] = new Vector3f(element.to.x(), element.to.y(), element.to.z());

        BlockElementRotation rotation = element.getRotation();
        for (int i = 0; i < 8; i++) {
            ModelUtils.applyElementRotation(corners[i], rotation);
        }

        return corners;
    }
}
