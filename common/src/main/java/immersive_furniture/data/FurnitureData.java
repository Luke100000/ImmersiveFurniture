package immersive_furniture.data;

import com.mojang.math.Axis;
import immersive_furniture.Utils;
import immersive_furniture.client.model.MaterialRegistry;
import immersive_furniture.client.model.MaterialSource;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.LinkedList;
import java.util.List;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    public String name = "Empty";
    public String tag = "Misc";
    public String material = "default";
    public int lightLevel;
    public int inventorySize;

    public int contentid = -1;

    public final List<Element> elements = new LinkedList<>();

    public FurnitureData() {
        elements.add(new Element());
    }

    public FurnitureData(CompoundTag tag) {
        this.name = tag.getString("Name");
        this.tag = tag.getString("Tag");
        this.material = tag.getString("Material");
        this.lightLevel = tag.getInt("LightLevel");
        this.inventorySize = tag.getInt("InventorySize");
        this.contentid = tag.getInt("ContentID");

        ListTag elementsTag = tag.getList("Elements", 10);
        for (int i = 0; i < elementsTag.size(); i++) {
            this.elements.add(new Element(elementsTag.getCompound(i)));
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Tag", this.tag);
        tag.putString("Material", material);
        tag.putInt("LightLevel", lightLevel);
        tag.putInt("InventorySize", inventorySize);
        tag.putInt("ContentID", contentid);

        ListTag elementsTag = new ListTag();
        for (Element element : elements) {
            elementsTag.add(element.toTag());
        }
        tag.put("Elements", elementsTag);

        return tag;
    }

    public int getCost() {
        return 1;
    }

    public BoundingBox boundingBox() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Element element : elements) {
            Vector3f from = element.from;
            Vector3f to = element.to;

            minX = Math.min(minX, (int) from.x);
            minY = Math.min(minY, (int) from.y);
            minZ = Math.min(minZ, (int) from.z);
            maxX = Math.max(maxX, (int) to.x);
            maxY = Math.max(maxY, (int) to.y);
            maxZ = Math.max(maxZ, (int) to.z);
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public double getSize() {
        // High quality size estimation
        BoundingBox boundingBox = boundingBox();
        return Math.max(
                Math.max(
                        Math.abs(boundingBox.minX() - 8.0f),
                        Math.abs(boundingBox.minY() - 8.0f)
                ),
                Math.max(
                        Math.max(
                                Math.abs(boundingBox.minZ() - 8.0f),
                                Math.abs(boundingBox.maxX() - 8.0f)
                        ),
                        Math.max(
                                Math.abs(boundingBox.maxY() - 8.0f),
                                Math.abs(boundingBox.maxZ() - 8.0f)
                        )
                )
        );
    }

    public static class Element {
        public Vector3f from;
        public Vector3f to;
        public Direction.Axis axis;
        public float rotation;
        public Material material;

        public Element() {
            from = new Vector3f(2, 2, 2);
            to = new Vector3f(14, 14, 14);
            axis = Direction.Axis.X;
            rotation = 0.0f;
            material = new Material();
        }

        public Element(CompoundTag tag) {
            this.from = Utils.fromFloatList(tag.getList("From", 5));
            this.to = Utils.fromFloatList(tag.getList("To", 5));
            this.axis = Direction.Axis.byName(tag.getString("Axis"));
            this.rotation = tag.getFloat("Rotation");
            this.material = new Material(tag.getCompound("Material"));
        }

        public Element(Element element) {
            this.from = new Vector3f(element.from);
            this.to = new Vector3f(element.to);
            this.axis = element.axis;
            this.rotation = element.rotation;
            this.material = element.material;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("From", Utils.toFloatList(from));
            tag.put("To", Utils.toFloatList(to));
            tag.putString("Axis", axis.getSerializedName());
            tag.putFloat("Rotation", rotation);
            tag.put("Material", material.toTag());
            return tag;
        }

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

        public Material() {
        }

        public Material(CompoundTag tag) {
            source = MaterialRegistry.INSTANCE.materials.getOrDefault(
                    new ResourceLocation(tag.getString("Source")),
                    MaterialSource.DEFAULT
            );
            wrap = WrapMode.valueOf(tag.getString("Wrap"));
            rotate = tag.getBoolean("Rotate");
            flip = tag.getBoolean("Flip");
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Source", source.location().toString());
            tag.putInt("Margin", margin);
            tag.putString("Wrap", wrap.name());
            tag.putBoolean("Rotate", rotate);
            tag.putBoolean("Flip", flip);
            return tag;
        }
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }
}
