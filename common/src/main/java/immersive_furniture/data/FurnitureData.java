package immersive_furniture.data;

import immersive_furniture.client.model.MaterialRegistry;
import immersive_furniture.client.model.MaterialSource;
import immersive_furniture.client.model.effects.LightMaterialEffect;
import immersive_furniture.config.Config;
import immersive_furniture.utils.Utils;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    public String name = "Empty";
    public String tag = "Misc";
    public String material = "default";
    public int lightLevel;
    public int inventorySize;

    public int contentid = -1;
    public String author = "";

    public final List<Element> elements = new LinkedList<>();

    private String hash;
    private Map<Direction, VoxelShape> cachedShapes = new HashMap<>();

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
        this.author = tag.getString("Author");

        ListTag elementsTag = tag.getList("Elements", 10);
        for (int i = 0; i < elementsTag.size(); i++) {
            this.elements.add(new Element(elementsTag.getCompound(i)));
        }
    }

    public FurnitureData(FurnitureData data) {
        this.name = data.name;
        this.tag = data.tag;
        this.material = data.material;
        this.lightLevel = data.lightLevel;
        this.inventorySize = data.inventorySize;
        this.contentid = data.contentid;
        this.author = data.author;
        this.hash = null;
        this.cachedShapes = new HashMap<>();

        for (Element element : data.elements) {
            this.elements.add(new Element(element));
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
        tag.putString("Author", author);

        ListTag elementsTag = new ListTag();
        for (Element element : elements) {
            elementsTag.add(element.toTag());
        }
        tag.put("Elements", elementsTag);

        return tag;
    }

    public int getCost() {
        float volume = getVolume() / 4096.0f;
        BoundingBox bb = boundingBox();
        float size = bb.getXSpan() * bb.getYSpan() * bb.getZSpan() / 4096.0f;
        float cost = volume + size + inventorySize + lightLevel / 15.0f + elements.size() / 4.0f;
        return (int) Math.ceil(cost * Config.getInstance().costMultiplier);
    }

    public BoundingBox boundingBox() {
        if (elements.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }

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

    public int getVolume() {
        int volume = 0;
        for (Element element : elements) {
            Vector3i size = element.getSize();
            volume += size.x * size.y * size.z;
        }
        return volume;
    }

    public double getSize() {
        // High-quality size estimation
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

    public boolean isTranslucent() {
        return false;
    }

    public boolean requiresBlockEntity() {
        BoundingBox b = boundingBox();
        return inventorySize > 0 || b.minX() < 0 || b.minY() < 0 || b.minZ() < 0 || b.maxX() > 16 || b.maxY() > 16 || b.maxZ() > 16;
    }

    public String getHash() {
        if (hash == null) {
            hash = Utils.hashNbt(toTag());
        }
        return hash;
    }

    public void dirty() {
        hash = null;
        cachedShapes.clear();
    }

    public VoxelShape getShape(Direction rotation) {
        return cachedShapes.computeIfAbsent(rotation, this::computeShape);
    }

    private VoxelShape computeShape(Direction r) {
        return elements.stream().map(element -> getBox(element, r)).reduce(Shapes::or).orElse(Shapes.empty());
    }

    private static VoxelShape getBox(Element element, Direction rotation) {
        switch (rotation) {
            case NORTH -> {
                return Block.box(
                        element.from.x, element.from.y, element.from.z,
                        element.to.x, element.to.y, element.to.z
                );
            }
            case SOUTH -> {
                return Block.box(
                        16 - element.to.x, element.from.y, 16 - element.to.z,
                        16 - element.from.x, element.to.y, 16 - element.from.z
                );
            }
            case WEST -> {
                return Block.box(
                        element.from.z, element.from.y, 16 - element.to.x,
                        element.to.z, element.to.y, 16 - element.from.x
                );
            }
            case EAST -> {
                return Block.box(
                        16 - element.to.z, element.from.y, element.from.x,
                        16 - element.from.z, element.to.y, element.to.x
                );
            }
        }
        return Shapes.empty();
    }

    public static class Element {
        public Vector3f from;
        public Vector3f to;
        public Direction.Axis axis;
        public float rotation;
        public Material material;
        public Map<Direction, int[]> bakedTexture = new HashMap<>();

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

            this.bakedTexture = new HashMap<>();
            CompoundTag bakedTextureTag = tag.getCompound("BakedTexture");
            for (String key : bakedTextureTag.getAllKeys()) {
                bakedTexture.put(Direction.CODEC.byName(key), bakedTextureTag.getIntArray(key));
            }
        }

        public Element(Element element) {
            this.from = new Vector3f(element.from);
            this.to = new Vector3f(element.to);
            this.axis = element.axis;
            this.rotation = element.rotation;
            this.material = element.material;
            this.bakedTexture = new HashMap<>();
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("From", Utils.toFloatList(from));
            tag.put("To", Utils.toFloatList(to));
            tag.putString("Axis", axis.getSerializedName());
            tag.putFloat("Rotation", rotation);
            tag.put("Material", material.toTag());

            CompoundTag bakedTextureTag = new CompoundTag();
            for (Map.Entry<Direction, int[]> entry : bakedTexture.entrySet()) {
                bakedTextureTag.putIntArray(entry.getKey().getSerializedName(), entry.getValue());
            }
            tag.put("BakedTexture", bakedTextureTag);

            return tag;
        }

        public Vector3i getSize() {
            return new Vector3i(
                    Math.abs((int) (to.x - from.x)),
                    Math.abs((int) (to.y - from.y)),
                    Math.abs((int) (to.z - from.z))
            );
        }

        public Vector3f getCenter() {
            return new Vector3f(
                    (from.x + to.x) / 2.0f,
                    (from.y + to.y) / 2.0f,
                    (from.z + to.z) / 2.0f
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

        public void sanityCheck() {
            float maxSize = 16.0f;
            to.x = Math.max(-maxSize, Math.min(16.0f + maxSize, to.x));
            to.y = Math.max(-maxSize, Math.min(16.0f + maxSize, to.y));
            to.z = Math.max(-maxSize, Math.min(16.0f + maxSize, to.z));
            from.x = Math.max(-maxSize, Math.min(16.0f + maxSize, from.x));
            from.y = Math.max(-maxSize, Math.min(16.0f + maxSize, from.y));
            from.z = Math.max(-maxSize, Math.min(16.0f + maxSize, from.z));
        }
    }

    public static class Material {
        public MaterialSource source = MaterialSource.DEFAULT;
        public int margin = 4;
        public WrapMode wrap = WrapMode.EXPAND;
        public boolean rotate = false;
        public boolean flip = false;

        public LightMaterialEffect lightEffect = new LightMaterialEffect();

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
            lightEffect.load(tag.getCompound("LightEffect"));
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Source", source.location().toString());
            tag.putInt("Margin", margin);
            tag.putString("Wrap", wrap.name());
            tag.putBoolean("Rotate", rotate);
            tag.putBoolean("Flip", flip);
            tag.put("LightEffect", lightEffect.save());
            return tag;
        }
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }
}
