package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import org.joml.*;

public class ClientModelUtils {
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

    public static Vector3f getVertex(Direction facing, int vertex, float[] fs, ElementRotation rotation, Matrix4f transform) {
        FaceInfo.VertexInfo vertexInfo = FaceInfo.fromFacing(facing).getVertexInfo(vertex);
        Vector3f vec = new Vector3f(fs[vertexInfo.xFace], fs[vertexInfo.yFace], fs[vertexInfo.zFace]);

        if (rotation != null) {
            Quaternionf quaternionf = ModelUtils.getElementRotation(rotation);
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
                ClientModelUtils.getVertex(facing, 0, fs, element.getRotation(), transform),
                ClientModelUtils.getVertex(facing, 1, fs, element.getRotation(), transform),
                ClientModelUtils.getVertex(facing, 2, fs, element.getRotation(), transform),
                ClientModelUtils.getVertex(facing, 3, fs, element.getRotation(), transform)
        };
    }

    public static BlockElementRotation toBlockElementRotation(ElementRotation rotation) {
        return new BlockElementRotation(
                rotation.origin(),
                rotation.axis(),
                rotation.angle(),
                rotation.rescale()
        );
    }
}
