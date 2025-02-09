package immersive_furniture.client.model;

import immersive_furniture.data.FurnitureData;
import net.minecraft.core.Direction;
import org.joml.Vector2i;
import org.joml.Vector3i;

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
}
