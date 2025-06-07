package immersive_furniture.data;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

public record ElementRotation(Vector3f origin, Direction.Axis axis, float angle, boolean rescale) {
}
