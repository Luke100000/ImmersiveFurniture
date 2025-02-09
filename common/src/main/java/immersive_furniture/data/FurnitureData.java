package immersive_furniture.data;

import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

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
    }

    public static class Material {
        public ResourceLocation texture = new ResourceLocation("minecraft:block/oak_log");

        public int offsetX;
        public int offsetY;
        public int offsetZ;

        public int margin = 4;
        public WrapMode wrap = WrapMode.EXPAND;
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }
}
