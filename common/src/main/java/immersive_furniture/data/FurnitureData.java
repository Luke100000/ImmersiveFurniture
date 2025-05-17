package immersive_furniture.data;

import com.mojang.math.Axis;
import immersive_furniture.client.model.MaterialSource;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.LinkedList;
import java.util.List;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    public int contentid;

    public String name;
    public String tag;
    public String material;
    public int lightLevel;

    public final List<Element> elements = new LinkedList<>();

    public FurnitureData() {
        name = "Empty";
        elements.add(new Element());
    }

    public FurnitureData(CompoundTag tag) {
        name = tag.getString("Name");
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }

    public int getCost() {
        return 1;
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
            Vector3f origin = new Vector3f(
                    (from.x + to.x) / 32.0f,
                    (from.y + to.y) / 32.0f,
                    (from.z + to.z) / 32.0f
            );

            switch (axis) {
                case X -> Axis.XP.rotationDegrees(rotation).transform(origin);
                case Y -> Axis.YP.rotationDegrees(rotation).transform(origin);
                case Z -> Axis.ZP.rotationDegrees(rotation).transform(origin);
            }

            return origin;
        }
    }

    public static class Material {
        public MaterialSource source = MaterialSource.DEFAULT;
        public int margin = 4;
        public WrapMode wrap = WrapMode.EXPAND;
        public boolean rotate = false;
        public boolean flip = false;
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }
}
