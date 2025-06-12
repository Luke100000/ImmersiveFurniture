package net.conczin.immersive_furniture.data;

import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    public String name = "Empty";
    public String tag = "Miscellaneous";
    public int lightLevel;
    public int inventorySize;

    public int contentid = -1;
    public String author = "Unknown";
    public String originalAuthor = "";
    public Set<String> sources = new HashSet<>();
    public Set<String> dependencies = new HashSet<>();
    public TransparencyType transparency = TransparencyType.SOLID;

    public final List<Element> elements = new LinkedList<>();

    private String hash;
    private Map<Direction, VoxelShape> cachedShapes = new ConcurrentHashMap<>();
    public long lastTick = 0;

    public FurnitureData() {
        elements.add(new Element());
    }

    public FurnitureData(CompoundTag tag) {
        this.name = tag.getString("Name");
        this.tag = tag.getString("Tag");
        this.lightLevel = tag.getInt("LightLevel");
        this.inventorySize = tag.getInt("InventorySize");
        this.contentid = tag.getInt("ContentID");
        this.author = tag.getString("Author");
        this.originalAuthor = tag.getString("OriginalAuthor");
        this.sources = Utils.fromNbt(tag.getList("Sources", 8));
        this.dependencies = Utils.fromNbt(tag.getList("Dependencies", 8));
        this.transparency = tag.contains("Transparency") ? TransparencyType.valueOf(tag.getString("Transparency")) : TransparencyType.SOLID;

        ListTag elementsTag = tag.getList("Elements", 10);
        for (int i = 0; i < elementsTag.size(); i++) {
            this.elements.add(new Element(elementsTag.getCompound(i)));
        }
    }

    public FurnitureData(FurnitureData data) {
        this.name = data.name;
        this.tag = data.tag;
        this.lightLevel = data.lightLevel;
        this.inventorySize = data.inventorySize;
        this.contentid = data.contentid;
        this.author = data.author;
        this.originalAuthor = data.originalAuthor.isEmpty() ? data.author : data.originalAuthor;
        this.sources.addAll(data.sources);
        this.dependencies.addAll(data.dependencies);
        this.transparency = data.transparency;

        this.hash = null;
        this.cachedShapes = new HashMap<>();
        this.lastTick = 0;

        for (Element element : data.elements) {
            this.elements.add(new Element(element));
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Tag", this.tag);
        tag.putInt("LightLevel", lightLevel);
        tag.putInt("InventorySize", inventorySize);
        tag.putInt("ContentID", contentid);
        tag.putString("Author", author);
        tag.putString("OriginalAuthor", originalAuthor);
        tag.put("Sources", Utils.toNbt(sources));
        tag.put("Dependencies", Utils.toNbt(dependencies));
        tag.putString("Transparency", this.transparency.name());

        ListTag elementsTag = new ListTag();
        for (Element element : elements) {
            elementsTag.add(element.toTag());
        }
        tag.put("Elements", elementsTag);

        return tag;
    }

    public int getCost() {
        double volume = Math.sqrt(getVolume()) / 32.0;
        double cost = volume + inventorySize * 0.5 + lightLevel / 15.0 + elements.size() / 4.0;
        return (int) Math.ceil(cost * Config.getInstance().costMultiplier);
    }

    public AABB boundingBox() {
        if (elements.isEmpty()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;

        for (Element element : elements) {
            Vector3f from = element.from;
            Vector3f to = element.to;

            minX = Math.min(minX, from.x);
            minY = Math.min(minY, from.y);
            minZ = Math.min(minZ, from.z);
            maxX = Math.max(maxX, to.x);
            maxY = Math.max(maxY, to.y);
            maxZ = Math.max(maxZ, to.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int getVolume() {
        int volume = 0;
        for (Element element : elements) {
            if (element.type != ElementType.ELEMENT) continue;
            Vector3i size = element.getSize();
            volume += size.x * size.y * size.z;
        }
        return volume;
    }

    public double getSize() {
        // High-quality size estimation
        AABB boundingBox = boundingBox();
        return Math.max(
                Math.max(
                        Math.abs(boundingBox.minX - 8.0f),
                        Math.abs(boundingBox.minY - 8.0f)
                ),
                Math.max(
                        Math.max(
                                Math.abs(boundingBox.minZ - 8.0f),
                                Math.abs(boundingBox.maxX - 8.0f)
                        ),
                        Math.max(
                                Math.abs(boundingBox.maxY - 8.0f),
                                Math.abs(boundingBox.maxZ - 8.0f)
                        )
                )
        );
    }

    public boolean requiresBlockEntity() {
        return inventorySize > 0;
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
        for (Element element : elements) {
            element.rotationAxes = null;
            element.bakedTexture.clear();
        }
    }

    public void playInteractSound(Level level, BlockPos pos, Player player) {
        for (Element element : elements) {
            if (element.type == ElementType.SOUND_EMITTER && element.soundEmitter.onInteract) {
                playSound(level, pos, player.getRandom(), element);
            }
        }
    }

    public boolean hasParticles() {
        return elements.stream().anyMatch(e -> e.type == ElementType.PARTICLE_EMITTER);
    }

    public boolean hasSounds() {
        return elements.stream().anyMatch(e -> e.type == ElementType.SOUND_EMITTER);
    }

    public boolean canSit() {
        return elements.stream().anyMatch(e -> e.type == ElementType.PLAYER_POSE && e.playerPose.pose == Pose.SITTING);
    }

    public boolean canSleep() {
        return elements.stream().anyMatch(e -> e.type == ElementType.PLAYER_POSE && e.playerPose.pose == Pose.SLEEPING);
    }

    public List<Component> getTooltip(boolean advanced) {
        List<Component> tooltip = new LinkedList<>();
        tooltip.add(Component.translatable("gui.immersive_furniture.author", author).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        if (!originalAuthor.isEmpty() && !originalAuthor.equals(author)) {
            tooltip.add(Component.translatable("gui.immersive_furniture.original_author", originalAuthor).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("gui.immersive_furniture.tag." + tag.toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD));
        if (lightLevel > 0) {
            tooltip.add(Component.translatable("gui.immersive_furniture.light_level", lightLevel).withStyle(ChatFormatting.YELLOW));
        }
        if (inventorySize > 0) {
            tooltip.add(Component.translatable("gui.immersive_furniture.inventory", inventorySize).withStyle(ChatFormatting.YELLOW));
        }
        if (hasParticles()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.has_particles").withStyle(ChatFormatting.YELLOW));
        }
        if (hasSounds()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.has_sounds").withStyle(ChatFormatting.YELLOW));
        }
        if (canSit()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.can_sit").withStyle(ChatFormatting.YELLOW));
        }
        if (canSleep()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.can_sleep").withStyle(ChatFormatting.YELLOW));
        }
        boolean hasAdvanced = false;
        if (!sources.isEmpty()) {
            hasAdvanced = true;
            if (advanced) {
                tooltip.add(Component.translatable("gui.immersive_furniture.sources").withStyle(ChatFormatting.GRAY));
                for (String source : sources) {
                    tooltip.add(Component.literal("- " + source).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        if (!dependencies.isEmpty()) {
            hasAdvanced = true;
            if (advanced) {
                tooltip.add(Component.translatable("gui.immersive_furniture.dependencies").withStyle(ChatFormatting.GRAY));
                for (String dependency : dependencies) {
                    tooltip.add(Component.literal("- " + dependency).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        if (hasAdvanced && !advanced) {
            tooltip.add(Component.translatable("gui.immersive_furniture.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return tooltip;
    }

    public record PoseOffset(Vector3f offset, Pose pose, float rotation) {

    }

    public PoseOffset getClosestPose(Vec3 location, Direction direction) {
        PoseOffset found = null;
        for (Element element : elements) {
            if (element.type == ElementType.PLAYER_POSE) {
                Vector3f center = rotate(element.getRotationAxes().center(), direction).mul(1.0f / 16.0f);
                if (found == null || location.distanceToSqr(center.x, center.y, center.z) < location.distanceToSqr(found.offset.x, found.offset.y, found.offset.z)) {
                    Vector3f forward = rotate(element.getRotationAxes().forward(), direction).mul(1.0f / 16.0f);
                    if (element.playerPose.pose == Pose.SLEEPING) {
                        center.add(forward.mul(0.5f));
                    } else {
                        center.add(forward.mul(0.125f));

                        Vector3f up = rotate(element.getRotationAxes().up(), direction).mul(1.0f / 16.0f);
                        center.sub(up.mul(0.0625f));
                    }
                    found = new PoseOffset(center, element.playerPose.pose, element.rotation + direction.toYRot() % 360.0f);
                }
            }
        }
        return found;
    }

    public interface ParticleConsumer {
        void addParticle(SimpleParticleType particle, float x, float y, float z, float vx, float vy, float vz);
    }

    public void tick(Level level, BlockPos pos, RandomSource random, ParticleConsumer particleConsumer, boolean inEditor) {
        for (Element element : elements) {
            if (element.type == ElementType.PARTICLE_EMITTER) {
                SimpleParticleType particle = element.particleEmitter.getParticle();
                if (particle == null) continue;

                float c = element.particleEmitter.amount - random.nextFloat();
                while (c > 0.0f) {
                    c--;

                    Vector3f sampledPos = element.sampleRandomPosition(random).mul(1.0f / 16.0f);
                    Vector3f up = element.getRotationAxes().up().div(Math.abs(element.to.y - element.from.y) + 0.001f);

                    float vr = element.particleEmitter.velocityRandom / 16.0f;
                    float vd = element.particleEmitter.velocityDirectional / 16.0f;

                    particleConsumer.addParticle(
                            particle,
                            sampledPos.x() + (inEditor ? 0.0f : pos.getX()),
                            sampledPos.y() + (inEditor ? 1024.0f : pos.getY()),
                            sampledPos.z() + (inEditor ? 0.0f : pos.getZ()),
                            (random.nextFloat() - 0.5f) * vr + up.x() * vd,
                            (random.nextFloat() - 0.5f) * vr + up.y() * vd,
                            (random.nextFloat() - 0.5f) * vr + up.z() * vd
                    );
                }
            } else if (element.type == ElementType.SOUND_EMITTER) {
                if (random.nextFloat() < element.soundEmitter.frequency) {
                    playSound(level, pos, random, element);
                }
            }
        }
    }

    private static void playSound(Level level, BlockPos pos, RandomSource random, Element element) {
        SoundEvent soundEvent = element.soundEmitter.getSoundEvent();
        if (soundEvent == null) return;
        level.playLocalSound(
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5,
                soundEvent,
                SoundSource.BLOCKS,
                (0.75f + random.nextFloat()) * element.soundEmitter.volume,
                (0.75f + random.nextFloat()) * element.soundEmitter.pitch,
                false
        );
    }

    public VoxelShape getShape(Direction rotation) {
        return cachedShapes.computeIfAbsent(rotation, this::computeShape);
    }

    private VoxelShape computeShape(Direction r) {
        return elements.stream()
                .filter(e -> e.type == ElementType.ELEMENT)
                .map(element -> getBox(element, r))
                .reduce(Shapes::or)
                .orElse(Block.box(2, 2, 2, 14, 14, 14));
    }

    private static Vector3f rotate(Vector3f vec, Direction direction) {
        return switch (direction) {
            case SOUTH -> new Vector3f(16 - vec.x, vec.y, 16 - vec.z);
            case EAST -> new Vector3f(16 - vec.z, vec.y, vec.x);
            case WEST -> new Vector3f(vec.z, vec.y, 16 - vec.x);
            default -> new Vector3f(vec);
        };
    }

    private static VoxelShape getBox(Element element, Direction rotation) {
        Vector3f from = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f to = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        Vector3f[] corners = ModelUtils.getCorners(element);
        for (Vector3f corner : corners) {
            from.x = Math.min(from.x, corner.x);
            from.y = Math.min(from.y, corner.y);
            from.z = Math.min(from.z, corner.z);
            to.x = Math.max(to.x, corner.x);
            to.y = Math.max(to.y, corner.y);
            to.z = Math.max(to.z, corner.z);
        }

        Vector3f rotatedFrom = rotate(from, rotation);
        Vector3f rotatedTo = rotate(to, rotation);

        return Block.box(
                Math.min(rotatedFrom.x, rotatedTo.x),
                Math.min(rotatedFrom.y, rotatedTo.y),
                Math.min(rotatedFrom.z, rotatedTo.z),
                Math.max(rotatedFrom.x, rotatedTo.x),
                Math.max(rotatedFrom.y, rotatedTo.y),
                Math.max(rotatedFrom.z, rotatedTo.z)
        );
    }

    public enum ElementType {
        ELEMENT,
        PARTICLE_EMITTER,
        SOUND_EMITTER,
        PLAYER_POSE,
        SPRITE,
    }

    public record ElementRotationAxes(Vector3f center, Vector3f right, Vector3f up, Vector3f forward) {
        public ElementRotationAxes(Vector3f center, Vector3i size) {
            this(center, new Vector3f(size.x(), 0, 0), new Vector3f(0, size.y(), 0), new Vector3f(0, 0, size.z()));
        }
    }

    public static class Element {
        public Vector3f from;
        public Vector3f to;
        public Direction.Axis axis;
        public float rotation;
        public ElementType type;
        public int color;
        public Material material;
        public ParticleEmitter particleEmitter;
        public SoundEmitter soundEmitter;
        public PlayerPose playerPose;
        public Sprite sprite;

        public Map<Direction, int[]> bakedTexture = new HashMap<>();
        public ElementRotationAxes rotationAxes;

        public Element() {
            from = new Vector3f(2, 0, 2);
            to = new Vector3f(14, 12, 14);
            axis = Direction.Axis.X;
            rotation = 0.0f;
            type = ElementType.ELEMENT;
            color = -1;
            material = new Material();
            particleEmitter = new ParticleEmitter();
            soundEmitter = new SoundEmitter();
            playerPose = new PlayerPose();
            sprite = new Sprite();
        }

        public Element(CompoundTag tag) {
            this.from = Utils.fromFloatList(tag.getList("From", 5));
            this.to = Utils.fromFloatList(tag.getList("To", 5));
            this.axis = Direction.Axis.byName(tag.getString("Axis"));
            this.rotation = tag.getFloat("Rotation");
            this.type = Utils.parseEnum(ElementType.class, tag.getString("Type"), ElementType.ELEMENT);
            this.color = tag.contains("Color") ? tag.getInt("Color") : -1;
            this.material = new Material(tag.getCompound("Material"));
            this.particleEmitter = new ParticleEmitter(tag.getCompound("ParticleEmitter"));
            this.soundEmitter = new SoundEmitter(tag.getCompound("SoundEmitter"));
            this.playerPose = new PlayerPose(tag.getCompound("PlayerPose"));
            this.sprite = new Sprite(tag.getCompound("Sprite"));

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
            this.type = element.type;
            this.color = element.color;
            this.material = new Material(element.material);
            this.particleEmitter = new ParticleEmitter(element.particleEmitter);
            this.soundEmitter = new SoundEmitter(element.soundEmitter);
            this.playerPose = new PlayerPose(element.playerPose);
            this.sprite = new Sprite(element.sprite);
            this.bakedTexture = new HashMap<>();
            this.rotationAxes = null;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("From", Utils.toFloatList(from));
            tag.put("To", Utils.toFloatList(to));
            tag.putString("Axis", axis.getSerializedName());
            tag.putFloat("Rotation", rotation);
            tag.putString("Type", type.name().toLowerCase());
            tag.putInt("Color", color);

            if (type == ElementType.ELEMENT) {
                tag.put("Material", material.toTag());

                // Textures
                CompoundTag bakedTextureTag = new CompoundTag();
                for (Map.Entry<Direction, int[]> entry : bakedTexture.entrySet()) {
                    bakedTextureTag.putIntArray(entry.getKey().getSerializedName(), entry.getValue());
                }
                tag.put("BakedTexture", bakedTextureTag);
            } else if (type == ElementType.PARTICLE_EMITTER) {
                tag.put("ParticleEmitter", particleEmitter.toTag());
            } else if (type == ElementType.SOUND_EMITTER) {
                tag.put("SoundEmitter", soundEmitter.toTag());
            } else if (type == ElementType.PLAYER_POSE) {
                tag.put("PlayerPose", playerPose.toTag());
            } else if (type == ElementType.SPRITE) {
                tag.put("Sprite", sprite.toTag());
            }

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

        public ElementRotation getRotation() {
            return new ElementRotation(
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
            // TODO: Math is wrong for rotated elements
            float maxSize = 64.0f;
            to.x = Math.max(-maxSize, Math.min(16.0f + maxSize, to.x));
            to.y = Math.max(-maxSize, Math.min(16.0f + maxSize, to.y));
            to.z = Math.max(-maxSize, Math.min(16.0f + maxSize, to.z));
            from.x = Math.max(-maxSize, Math.min(16.0f + maxSize, from.x));
            from.y = Math.max(-maxSize, Math.min(16.0f + maxSize, from.y));
            from.z = Math.max(-maxSize, Math.min(16.0f + maxSize, from.z));

            // Pose anchors are the shape of the players' butt
            if (type == ElementType.PLAYER_POSE) {
                Vector3f center = getCenter();
                from.x = center.x - 4.0f;
                from.y = center.y - 1.0f;
                from.z = center.z - (playerPose.pose == Pose.SLEEPING ? 14.0f : 4.0f);
                to.x = center.x + 4.0f;
                to.y = center.y + 1.0f;
                to.z = center.z + (playerPose.pose == Pose.SLEEPING ? 14.0f : 4.0f);
                rotation = 0.0f;
                axis = Direction.Axis.Y;
            } else if (type == ElementType.SPRITE) {
                // Sprites are forced to be 16x16x0
                Vector3f center = getCenter();
                from.x = center.x - 8.0f;
                from.y = center.y - 8.0f;
                from.z = center.z;
                to.x = center.x + 8.0f;
                to.y = center.y + 8.0f;
                to.z = center.z;
            }
        }

        public boolean contains(Vector3f pos) {
            return contains(pos, 0.0001f);
        }

        public boolean contains(Vector3f pos, float margin) {
            return pos.x >= from.x - margin && pos.x <= to.x + margin &&
                   pos.y >= from.y - margin && pos.y <= to.y + margin &&
                   pos.z >= from.z - margin && pos.z <= to.z + margin;
        }

        public ElementRotationAxes getRotationAxes() {
            if (rotationAxes == null) {
                rotationAxes = new ElementRotationAxes(getCenter(), getSize());

                ElementRotation elementRotation = getRotation();
                Quaternionf quaternion = ModelUtils.getElementRotation(elementRotation);
                quaternion.transform(rotationAxes.up);
                quaternion.transform(rotationAxes.right);
                quaternion.transform(rotationAxes.forward);

                ModelUtils.applyElementRotation(rotationAxes.center, elementRotation);
            }
            return rotationAxes;
        }

        public Vector3f sampleRandomPosition(RandomSource random) {
            ElementRotationAxes axes = getRotationAxes();
            float x = random.nextFloat() - 0.5f;
            float y = random.nextFloat() - 0.5f;
            float z = random.nextFloat() - 0.5f;
            return new Vector3f(
                    axes.center.x + x * axes.right.x + y * axes.up.x + z * axes.forward.x,
                    axes.center.y + x * axes.right.y + y * axes.up.y + z * axes.forward.y,
                    axes.center.z + x * axes.right.z + y * axes.up.z + z * axes.forward.z
            );
        }
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }

    public static class Material {
        public ResourceLocation source = new ResourceLocation("minecraft:oak_log");
        public int margin = 4;
        public WrapMode wrap = WrapMode.EXPAND;
        public boolean rotate = false;
        public boolean flip = false;

        public LightMaterialEffect lightEffect = new LightMaterialEffect();

        public Material() {

        }

        public Material(CompoundTag tag) {
            source = new ResourceLocation(tag.getString("Source"));
            wrap = Utils.parseEnum(WrapMode.class, tag.getString("Wrap"), WrapMode.EXPAND);
            rotate = tag.getBoolean("Rotate");
            flip = tag.getBoolean("Flip");
            lightEffect.load(tag.getCompound("LightEffect"));
        }

        public Material(Material material) {
            source = material.source;
            margin = material.margin;
            wrap = material.wrap;
            rotate = material.rotate;
            flip = material.flip;
            lightEffect = new LightMaterialEffect(material.lightEffect);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Source", source.toString());
            tag.putInt("Margin", margin);
            tag.putString("Wrap", wrap.name());
            tag.putBoolean("Rotate", rotate);
            tag.putBoolean("Flip", flip);
            tag.put("LightEffect", lightEffect.save());
            return tag;
        }
    }

    public static class ParticleEmitter {
        public ResourceLocation particle = new ResourceLocation("minecraft:smoke");
        public float velocityDirectional = 0.0f;
        public float velocityRandom = 0.1f;
        public float amount = 0.5f;

        public ParticleEmitter() {

        }

        public ParticleEmitter(CompoundTag tag) {
            this.particle = new ResourceLocation(tag.getString("Particle"));
            this.velocityDirectional = tag.getFloat("VelocityDirectional");
            this.velocityRandom = tag.getFloat("VelocityRandom");
            this.amount = tag.getFloat("Amount");
        }

        public ParticleEmitter(ParticleEmitter particleEmitter) {
            this.particle = particleEmitter.particle;
            this.velocityDirectional = particleEmitter.velocityDirectional;
            this.velocityRandom = particleEmitter.velocityRandom;
            this.amount = particleEmitter.amount;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Particle", particle.toString());
            tag.putFloat("VelocityDirectional", velocityDirectional);
            tag.putFloat("VelocityRandom", velocityRandom);
            tag.putFloat("VelocityRandom", velocityRandom);
            tag.putFloat("Amount", amount);
            return tag;
        }

        public SimpleParticleType getParticle() {
            if (BuiltInRegistries.PARTICLE_TYPE.get(particle) instanceof SimpleParticleType simpleParticleType) {
                return simpleParticleType;
            }
            return null;
        }
    }

    public static class SoundEmitter {
        public ResourceLocation sound = new ResourceLocation("minecraft:entity.item.pickup");
        public float volume = 1.0f;
        public float pitch = 1.0f;
        public float frequency = 0.1f;
        public boolean onInteract = false;

        public SoundEmitter() {

        }

        public SoundEmitter(CompoundTag tag) {
            this.sound = new ResourceLocation(tag.getString("Sound"));
            this.volume = tag.getFloat("Volume");
            this.pitch = tag.getFloat("Pitch");
            this.frequency = tag.getFloat("Frequency");
            this.onInteract = tag.getBoolean("OnInteract");
        }

        public SoundEmitter(SoundEmitter soundEmitter) {
            this.sound = soundEmitter.sound;
            this.volume = soundEmitter.volume;
            this.pitch = soundEmitter.pitch;
            this.frequency = soundEmitter.frequency;
            this.onInteract = soundEmitter.onInteract;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Sound", sound.toString());
            tag.putFloat("Volume", volume);
            tag.putFloat("Pitch", pitch);
            tag.putFloat("Frequency", frequency);
            tag.putBoolean("OnInteract", onInteract);
            return tag;
        }

        public SoundEvent getSoundEvent() {
            return BuiltInRegistries.SOUND_EVENT.get(sound);
        }
    }

    public static class PlayerPose {
        public Pose pose = Pose.SITTING;

        public PlayerPose() {
        }

        public PlayerPose(CompoundTag tag) {
            this.pose = Utils.parseEnum(Pose.class, tag.getString("Pose"), Pose.SITTING);
        }

        public PlayerPose(PlayerPose playerPose) {
            this.pose = playerPose.pose;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Pose", pose.name());
            return tag;
        }
    }

    public static class Sprite {
        public ResourceLocation sprite = new ResourceLocation("minecraft:block/water_still");

        public Sprite() {
        }

        public Sprite(CompoundTag tag) {
            this.sprite = new ResourceLocation(tag.getString("Sprite"));
        }

        public Sprite(Sprite sprite) {
            this.sprite = sprite.sprite;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Sprite", sprite.toString());
            return tag;
        }
    }

    public static class LightMaterialEffect {
        public float roundness = 0.0f;
        public float brightness = 0.0f;
        public float contrast = 0.0f;

        public LightMaterialEffect() {

        }

        public LightMaterialEffect(LightMaterialEffect lightEffect) {
            this.roundness = lightEffect.roundness;
            this.brightness = lightEffect.brightness;
            this.contrast = lightEffect.contrast;
        }

        public void load(CompoundTag tag) {
            roundness = tag.getFloat("Roundness");
            brightness = tag.getFloat("Brightness");
            contrast = tag.getFloat("Contrast");
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("Roundness", roundness);
            tag.putFloat("Brightness", brightness);
            tag.putFloat("Contrast", contrast);
            return tag;
        }
    }
}
