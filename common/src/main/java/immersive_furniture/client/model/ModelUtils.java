package immersive_furniture.client.model;

import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
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
                        (int) element.from.z + y,
                        (int) element.to.y
                );
            }
            case DOWN -> {
                return new Vector3i(
                        (int) element.from.x + x,
                        (int) element.from.z + y,
                        (int) element.from.y
                );
            }
            case NORTH -> {
                return new Vector3i(
                        (int) element.from.x + x,
                        (int) element.from.y + y,
                        (int) element.from.z
                );
            }
            case SOUTH -> {
                return new Vector3i(
                        (int) element.from.x + x,
                        (int) element.from.y + y,
                        (int) element.to.z
                );
            }
            case WEST -> {
                return new Vector3i(
                        (int) element.from.z + x,
                        (int) element.from.y + y,
                        (int) element.from.x
                );
            }
            case EAST -> {
                return new Vector3i(
                        (int) element.to.z - x,
                        (int) element.from.y + y,
                        (int) element.from.x
                );
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }

    private static final float RESCALE_22_5 = 1.0f / (float) Math.cos(0.3926991f) - 1.0f;
    private static final float RESCALE_45 = 1.0f / (float) Math.cos(0.7853981852531433) - 1.0f;

    public static Vector3f getVertex(Direction facing, int vertex, float[] fs, BlockElementRotation rotation, Matrix4f pose) {
        FaceInfo.VertexInfo vertexInfo = FaceInfo.fromFacing(facing).getVertexInfo(vertex);
        Vector3f vec = new Vector3f(fs[vertexInfo.xFace], fs[vertexInfo.yFace], fs[vertexInfo.zFace]);
        applyElementRotation(vec, rotation);
        pose.transformPosition(vec);
        return vec;
    }

    public static void applyElementRotation(Vector3f vec, BlockElementRotation partRotation) {
        if (partRotation == null) return;

        Vector3f axis;
        Vector3f normal = switch (partRotation.axis()) {
            case X -> {
                axis = new Vector3f(1.0f, 0.0f, 0.0f);
                yield new Vector3f(0.0f, 1.0f, 1.0f);
            }
            case Y -> {
                axis = new Vector3f(0.0f, 1.0f, 0.0f);
                yield new Vector3f(1.0f, 0.0f, 1.0f);
            }
            case Z -> {
                axis = new Vector3f(0.0f, 0.0f, 1.0f);
                yield new Vector3f(1.0f, 1.0f, 0.0f);
            }
        };

        if (partRotation.rescale()) {
            if (Math.abs(partRotation.angle()) == 22.5f) {
                normal.mul(RESCALE_22_5);
            } else {
                normal.mul(RESCALE_45);
            }
            normal.add(1.0f, 1.0f, 1.0f);
        } else {
            normal.set(1.0f, 1.0f, 1.0f);
        }

        Quaternionf quaternionf = new Quaternionf().rotationAxis(partRotation.angle() * ((float) Math.PI / 180), axis);
        vec.sub(partRotation.origin());
        quaternionf.transform(vec);
        vec.add(partRotation.origin());
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

    public static Vector3f[] getVertices(FurnitureData.Element element, Direction facing, float[] fs, Matrix4f pose) {
        return new Vector3f[]{
                ModelUtils.getVertex(facing, 0, fs, element.getRotation(), pose),
                ModelUtils.getVertex(facing, 1, fs, element.getRotation(), pose),
                ModelUtils.getVertex(facing, 2, fs, element.getRotation(), pose),
                ModelUtils.getVertex(facing, 3, fs, element.getRotation(), pose)
        };
    }
}
