package immersive_furniture.data;

import immersive_furniture.client.model.MaterialSource;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.LinkedList;
import java.util.List;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    static {
        // TODO: Debug
        EMPTY.elements.add(new Element());
    }

    private final String name;

    public final List<Element> elements = new LinkedList<>();

    public FurnitureData() {
        name = "Empty";
    }

    public FurnitureData(CompoundTag tag) {
        name = tag.getString("Name");
    }

    public String getName() {
        return name;
    }

    public Tag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }

    public static class Element {
        public Vector3f from = new Vector3f(2, 2, 2);
        public Vector3f to = new Vector3f(14, 14, 14);
        public Direction.Axis axis = Direction.Axis.X;
        public float rotation = 0.0f;
        public Material material = new Material();

        public Vector3i getSize() {
            return new Vector3i(
                    Math.abs((int) (to.x - from.x)),
                    Math.abs((int) (to.y - from.y)),
                    Math.abs((int) (to.z - from.z))
            );
        }

        public BlockElementRotation getRotation() {
            return new BlockElementRotation(
                    getOrigin(),
                    axis,
                    rotation,
                    false
            );
        }

        public Vector3f getOrigin() {
            return new Vector3f(
                    (from.x + to.x) / 32.0f,
                    (from.y + to.y) / 32.0f,
                    (from.z + to.z) / 32.0f
            );
        }
    }

    public static class Material {
        public MaterialSource source = MaterialSource.DEFAULT;
        public int margin = 4;
        public WrapMode wrap = WrapMode.EXPAND;
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }
}
