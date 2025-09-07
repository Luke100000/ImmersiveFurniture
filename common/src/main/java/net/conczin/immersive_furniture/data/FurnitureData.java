package net.conczin.immersive_furniture.data;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.utils.NBTHelper;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    public String name = "Empty";
    public String tag = "miscellaneous";
    public int lightLevel;
    public int inventorySize;
    public boolean toggleWithRightClick;
    public boolean toggleLight;

    public int contentid = -1;
    public String author = "Unknown";
    public String originalAuthor = "";
    public Set<String> sources = new HashSet<>();
    public Set<String> dependencies = new HashSet<>();

    public final List<Element> elements = new LinkedList<>();

    public Vector3i size = new Vector3i(1, 1, 1);

    private String hash;
    private final Map<Integer, VoxelShape> cachedFullShapes = new ConcurrentHashMap<>();
    private final Map<Integer, VoxelShape> cachedSubShapes = new ConcurrentHashMap<>();
    private final Set<Integer> requestedShapes = new ConcurrentSkipListSet<>();
    public long lastTick = 0;

    public FurnitureData() {
        elements.add(new Element());
    }

    public FurnitureData(CompoundTag tag) {
        this.name = NBTHelper.getString(tag, "Name", name);
        this.tag = NBTHelper.getString(tag, "Tag", this.tag);
        this.lightLevel = NBTHelper.getInt(tag, "LightLevel", lightLevel);
        this.inventorySize = NBTHelper.getInt(tag, "InventorySize", inventorySize);
        this.toggleWithRightClick = NBTHelper.getBoolean(tag, "ToggleWithRightClick", toggleWithRightClick);
        this.toggleLight = NBTHelper.getBoolean(tag, "ToggleLight", toggleLight);
        this.contentid = NBTHelper.getInt(tag, "ContentID", contentid);
        this.author = NBTHelper.getString(tag, "Author", author);
        this.originalAuthor = NBTHelper.getString(tag, "OriginalAuthor", originalAuthor);
        this.sources = NBTHelper.getStringSet(tag.getList("Sources", 8));
        this.dependencies = NBTHelper.getStringSet(tag.getList("Dependencies", 8));

        this.size = new Vector3i(
                NBTHelper.getInt(tag, "SizeX", 1),
                NBTHelper.getInt(tag, "SizeY", 1),
                NBTHelper.getInt(tag, "SizeZ", 1)
        );

        ListTag elementsTag = tag.getList("Elements", 10);
        for (int i = 0; i < elementsTag.size(); i++) {
            this.elements.add(new Element(elementsTag.getCompound(i)));
        }
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public FurnitureData(FurnitureData data) {
        this.name = data.name;
        this.tag = data.tag;
        this.lightLevel = data.lightLevel;
        this.inventorySize = data.inventorySize;
        this.toggleWithRightClick = data.toggleWithRightClick;
        this.toggleLight = data.toggleLight;
        this.author = data.author;
        this.originalAuthor = data.originalAuthor.isEmpty() ? data.author : data.originalAuthor;
        this.sources.addAll(data.sources);
        this.dependencies.addAll(data.dependencies);

        this.hash = null;
        this.lastTick = 0;

        for (Element element : data.elements) {
            this.elements.add(new Element(element));
        }

        this.size = new Vector3i(data.size.x, data.size.y, data.size.z);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Tag", this.tag);
        tag.putInt("LightLevel", lightLevel);
        tag.putInt("InventorySize", inventorySize);
        tag.putBoolean("ToggleWithRightClick", toggleWithRightClick);
        tag.putBoolean("ToggleLight", toggleLight);
        tag.putInt("ContentID", contentid);
        tag.putString("Author", author);
        tag.putString("OriginalAuthor", originalAuthor);
        tag.put("Sources", NBTHelper.getStringList(sources));
        tag.put("Dependencies", NBTHelper.getStringList(dependencies));

        tag.putInt("SizeX", size.x);
        tag.putInt("SizeY", size.y);
        tag.putInt("SizeZ", size.z);

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
            if (element.type != ElementType.ELEMENT && element.type != ElementType.SPRITE) continue;

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
            hash = Utils.hashNbt(new FurnitureData(this).toTag());
        }
        return hash;
    }

    public void dirty() {
        hash = null;
        cachedFullShapes.clear();
        cachedSubShapes.clear();
        requestedShapes.clear();
        for (Element element : elements) {
            element.rotationAxes = null;
            element.bakedTextures.clear();
        }
    }

    public void playInteractSound(Level level, BlockPos pos, int state, Player player) {
        for (Element element : elements) {
            if (element.isMasked(state) && element.type == ElementType.SOUND_EMITTER && element.soundEmitter.onInteract) {
                playSound(level, pos, player.getRandom(), element);
            }
        }
    }

    public void emitInteractParticles(BlockPos pos, Direction direction, int state, Player player, ParticleConsumer particleConsumer, boolean inScreen) {
        for (Element element : elements) {
            if (element.isMasked(state) && element.type == ElementType.PARTICLE_EMITTER && element.particleEmitter.onInteract) {
                emitParticles(pos, direction, player.getRandom(), element, particleConsumer, inScreen, 10.0f);
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

    public boolean hasDisplayItems() {
        return elements.stream().anyMatch(e -> e.type == ElementType.SPRITE && e.sprite.item);
    }

    /**
     * @return A set of unique solid states for the furniture, e.g.: a door would return (0, 1)
     */
    public Set<Integer> getUniqueSolidStates() {
        Map<Integer, Long> maskCounts = elements.stream()
                .filter(e -> e.type == ElementType.ELEMENT || e.type == ElementType.SPRITE)
                .collect(Collectors.groupingBy(
                        e -> e.mask,
                        Collectors.counting()
                ));

        Set<Integer> states = new HashSet<>();
        long firstHash = -1;
        for (int state = 0; state < 2; state++) {
            long hash = 0;
            for (Map.Entry<Integer, Long> entry : maskCounts.entrySet()) {
                if ((entry.getKey() & (1 << state)) != 0) {
                    hash += 1024L * entry.getKey() + entry.getValue();
                }
            }
            if (hash != firstHash) states.add(state);
            if (state == 0) firstHash = hash;
        }
        return states;
    }

    public List<Component> getTooltip(boolean advanced) {
        List<Component> tooltip = new LinkedList<>();
        tooltip.add(Component.translatable("gui.immersive_furniture.author", author).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        if (!originalAuthor.isEmpty() && !originalAuthor.equals(author)) {
            tooltip.add(Component.translatable("gui.immersive_furniture.original_author", originalAuthor).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("gui.immersive_furniture.tag." + tag.toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD).append(getPrefixedSizeTooltip()));
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
        if (hasDisplayItems()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.has_display_items").withStyle(ChatFormatting.YELLOW));
        }
        if (getUniqueSolidStates().size() > 1) {
            tooltip.add(Component.translatable("gui.immersive_furniture.has_states").withStyle(ChatFormatting.YELLOW));
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
        if (advanced) {
            int pixels = 0;
            for (Element element : elements) {
                for (Map<Integer, int[]> texture : element.bakedTextures.textures.values()) {
                    for (Map.Entry<Integer, int[]> states : texture.entrySet()) {
                        pixels += states.getValue().length;
                    }
                }
            }
            double usage = pixels / Math.pow(DynamicAtlas.BAKED.getSize(), 2);
            tooltip.add(Component.translatable("gui.immersive_furniture.atlas_usage", String.format("%.1f%%", usage * 100)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (hasAdvanced && !advanced) {
            tooltip.add(Component.translatable("gui.immersive_furniture.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return tooltip;
    }

    private Component getPrefixedSizeTooltip() {
        Component sizeTooltip = getSizeTooltip();
        if (sizeTooltip == null) {
            return Component.literal("");
        } else {
            return Component.literal(" - ").append(sizeTooltip).withStyle(ChatFormatting.GRAY);
        }
    }

    private Component getSizeTooltip() {
        if (size.x == 1 && size.y == 1 && size.z == 1) {
            return null; // No size tooltip for single block elements
        } else if (size.x == 1 && size.y == 1) {
            return Component.translatable("gui.immersive_furniture.n_deep", size.z);
        } else if (size.x == 1 && size.z == 1) {
            return Component.translatable("gui.immersive_furniture.n_tall", size.y);
        } else if (size.y == 1 && size.z == 1) {
            return Component.translatable("gui.immersive_furniture.n_wide", size.x);
        }
        return Component.literal(String.format("%sx%sx%s", size.x, size.y, size.z));
    }

    public record PoseOffset(Vector3f offset, Pose pose, float rotation) {

    }

    public PoseOffset getClosestPose(Vec3 location, Direction direction) {
        float bestDistance = Float.MAX_VALUE;
        PoseOffset found = null;
        for (Element element : elements) {
            if (element.type == ElementType.PLAYER_POSE) {
                Vector3f center = rotate(element.getRotationAxes().center(), direction).mul(1.0f / 16.0f);
                double distance = location.distanceToSqr(center.x, center.y, center.z);
                if (found == null || distance < bestDistance) {
                    bestDistance = (float) distance;

                    Vector3f forward = rotateVector(element.getRotationAxes().forward(), direction).normalize();
                    Vector3f up = rotateVector(element.getRotationAxes().up(), direction).normalize();

                    if (element.playerPose.pose == Pose.SLEEPING) {
                        center.add(forward.mul(0.5625f));
                        center.sub(up.mul(-0.0625f));
                    } else {
                        center.add(forward.mul(0.125f));
                        center.sub(up.mul(0.03125f));
                    }
                    found = new PoseOffset(center, element.playerPose.pose, -element.rotation + direction.toYRot() % 360.0f);
                }
            }
        }
        return found;
    }

    public interface ParticleConsumer {
        void addParticle(SimpleParticleType particle, float x, float y, float z, float vx, float vy, float vz);
    }

    private void emitParticles(BlockPos pos, Direction direction, RandomSource random, Element element, ParticleConsumer particleConsumer, boolean inScreen, float amountMultiplier) {
        SimpleParticleType particle = element.particleEmitter.getParticle();
        if (particle == null) return;

        float c = element.particleEmitter.amount * amountMultiplier - random.nextFloat();
        while (c > 0.0f) {
            c--;

            Vector3f sampledPos = element.sampleRandomPosition(random, direction).mul(1.0f / 16.0f);
            Vector3f up = new Vector3f(element.getRotationAxes().up()).div(Math.abs(element.to.y - element.from.y) + 0.001f);

            float vr = element.particleEmitter.velocityRandom / 16.0f;
            float vd = element.particleEmitter.velocityDirectional / 16.0f;

            particleConsumer.addParticle(
                    particle,
                    sampledPos.x() + (inScreen ? 0.0f : pos.getX()),
                    sampledPos.y() + (inScreen ? 1024.0f : pos.getY()),
                    sampledPos.z() + (inScreen ? 0.0f : pos.getZ()),
                    (random.nextFloat() - 0.5f) * vr + up.x() * vd,
                    (random.nextFloat() - 0.5f) * vr + up.y() * vd,
                    (random.nextFloat() - 0.5f) * vr + up.z() * vd
            );
        }
    }

    public void tick(Level level, BlockPos pos, int state, Direction direction, RandomSource random, ParticleConsumer particleConsumer, boolean inScreen, boolean inEditor) {
        for (Element element : elements) {
            if (!element.isMasked(state)) continue;
            if (element.type == ElementType.PARTICLE_EMITTER && !element.particleEmitter.onInteract) {
                emitParticles(pos, direction, random, element, particleConsumer, inScreen, 1.0f);
            } else if (element.type == ElementType.SOUND_EMITTER && (!inScreen || inEditor) && element.soundEmitter.frequency > 0 && random.nextFloat() < element.soundEmitter.frequency) {
                playSound(level, pos, random, element);
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

    public VoxelShape getShape(Direction rotation, int state) {
        return cachedFullShapes.computeIfAbsent(rotation.ordinal() * 31 + state, key -> computeShape(rotation, state));
    }

    public VoxelShape getShape(Direction rotation, int state, int offsetX, int offsetY, int offsetZ) {
        return cachedSubShapes.computeIfAbsent(
                ((((rotation.ordinal() * 31) + offsetX) * 31 + offsetY) * 31 + offsetZ) * 31 + state,
                key -> computeShape(rotation, state, offsetX, offsetY, offsetZ)
        );
    }

    public VoxelShape getShapeLazy(Direction rotation) {
        int id = rotation.ordinal() * 31;
        if (cachedFullShapes.containsKey(id)) {
            return cachedFullShapes.get(id);
        }
        if (!requestedShapes.contains(id)) {
            requestedShapes.add(id);
            Common.EXECUTOR.execute(() -> getShape(rotation, 0));
        }
        return null;
    }

    // Computes the entire shape for visualization
    private VoxelShape computeShape(Direction rotation, int state) {
        return elements.stream()
                .filter(e -> e.type == ElementType.ELEMENT && !e.isFlat() && e.isMasked(state))
                .map(element -> getBox(element, rotation))
                .reduce((a, b) -> Shapes.joinUnoptimized(a, b, BooleanOp.OR))
                .map(VoxelShape::optimize)
                .orElse(Block.box(2, 2, 2, 14, 14, 14));
    }

    public int getRotatedX(Direction facing, int x, int z) {
        return switch (facing) {
            case SOUTH -> -x;
            case EAST -> -z;
            case WEST -> z;
            default -> x;
        };
    }

    public int getRotatedZ(Direction facing, int x, int z) {
        return switch (facing) {
            case SOUTH -> -z;
            case EAST -> x;
            case WEST -> -x;
            default -> z;
        };
    }

    // Computes a fraction of a shape for collision
    public VoxelShape computeShape(Direction rotation, int state, int offsetX, int offsetY, int offsetZ) {
        Vector3f start = rotate(new Vector3f(
                offsetX == 0 ? -8 : 0,
                offsetY == 0 ? -8 : 0,
                offsetZ == 0 ? -8 : 0
        ), rotation);

        Vector3f stop = rotate(new Vector3f(
                offsetX == size.x - 1 ? 24 : 16,
                offsetY == size.y - 1 ? 24 : 16,
                offsetZ == size.z - 1 ? 24 : 16
        ), rotation);

        return Shapes.join(
                getShape(rotation, state).move(
                        -getRotatedX(rotation, offsetX, offsetZ),
                        -offsetY,
                        -getRotatedZ(rotation, offsetX, offsetZ)
                ),
                Block.box(
                        Math.min(start.x, stop.x),
                        Math.min(start.y, stop.y),
                        Math.min(start.z, stop.z),
                        Math.max(start.x, stop.x),
                        Math.max(start.y, stop.y),
                        Math.max(start.z, stop.z)
                ),
                BooleanOp.AND
        );
    }

    private static Vector3f rotate(Vector3f vec, Direction direction) {
        return switch (direction) {
            case SOUTH -> new Vector3f(16 - vec.x, vec.y, 16 - vec.z);
            case EAST -> new Vector3f(16 - vec.z, vec.y, vec.x);
            case WEST -> new Vector3f(vec.z, vec.y, 16 - vec.x);
            default -> new Vector3f(vec);
        };
    }

    private static Vector3f rotateVector(Vector3f vec, Direction direction) {
        return switch (direction) {
            case SOUTH -> new Vector3f(-vec.x, vec.y, -vec.z);
            case EAST -> new Vector3f(-vec.z, vec.y, vec.x);
            case WEST -> new Vector3f(vec.z, vec.y, -vec.x);
            default -> new Vector3f(vec);
        };
    }

    private VoxelShape getBox(Element element, Direction rotation) {
        Vector3f from = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f to = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        Vector3f[] corners = ModelUtils.getCorners(element);
        for (Vector3f corner : corners) {
            from.x = Math.min(from.x, corner.x);
            from.y = Math.min(from.y, corner.y);
            from.z = Math.min(from.z, corner.z);
            to.x = Math.max(to.x, corner.x);
            to.y = Math.max(to.y, corner.y);
            to.z = Math.max(to.z, corner.z);
        }

        // Adjust volume
        double volume = element.getVolume();
        double newVolume = (to.x - from.x) * (to.y - from.y) * (to.z - from.z);
        float fraction = (float) Math.sqrt(volume / newVolume);
        if (element.axis != Direction.Axis.X) {
            float mid = 0.5f * (from.x + to.x);
            from.x = from.x * fraction + mid * (1.0f - fraction);
            to.x = to.x * fraction + mid * (1.0f - fraction);
        }
        if (element.axis != Direction.Axis.Y) {
            float mid = 0.5f * (from.y + to.y);
            from.y = from.y * fraction + mid * (1.0f - fraction);
            to.y = to.y * fraction + mid * (1.0f - fraction);
        }
        if (element.axis != Direction.Axis.Z) {
            float mid = 0.5f * (from.z + to.z);
            from.z = from.z * fraction + mid * (1.0f - fraction);
            to.z = to.z * fraction + mid * (1.0f - fraction);
        }

        // Rotate the box
        Vector3f rotatedFrom = rotate(from, rotation);
        Vector3f rotatedTo = rotate(to, rotation);

        // Quantize the box
        float resolution = elements.size() > 96 ? 1.0f : elements.size() > 48 ? 2.0f : 4.0f;
        return Block.box(
                Math.round(Math.min(rotatedFrom.x, rotatedTo.x) * resolution) / resolution,
                Math.round(Math.min(rotatedFrom.y, rotatedTo.y) * resolution) / resolution,
                Math.round(Math.min(rotatedFrom.z, rotatedTo.z) * resolution) / resolution,
                Math.round(Math.max(rotatedFrom.x, rotatedTo.x) * resolution) / resolution,
                Math.round(Math.max(rotatedFrom.y, rotatedTo.y) * resolution) / resolution,
                Math.round(Math.max(rotatedFrom.z, rotatedTo.z) * resolution) / resolution
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
        public Direction.Axis axis = Direction.Axis.Y;
        public float rotation = 0.0f;
        public ElementType type = ElementType.ELEMENT;
        public int color = -1;
        public int emission = 0;
        public int mask = 3;
        public Material material;
        public ParticleEmitter particleEmitter;
        public SoundEmitter soundEmitter;
        public PlayerPose playerPose;
        public Sprite sprite;

        public ElementBakedTextures bakedTextures = new ElementBakedTextures();
        public ElementRotationAxes rotationAxes;

        public Element() {
            from = new Vector3f(2, 0, 2);
            to = new Vector3f(14, 12, 14);
            material = new Material();
            particleEmitter = new ParticleEmitter();
            soundEmitter = new SoundEmitter();
            playerPose = new PlayerPose();
            sprite = new Sprite();
        }

        public Element(CompoundTag tag) {
            this.from = NBTHelper.getVector3f(tag.getList("From", 5));
            this.to = NBTHelper.getVector3f(tag.getList("To", 5));
            this.axis = NBTHelper.getEnum(tag, Direction.Axis.class, "Axis", axis);
            this.rotation = NBTHelper.getFloat(tag, "Rotation", rotation);
            this.type = NBTHelper.getEnum(tag, ElementType.class, "Type", type);
            this.color = NBTHelper.getInt(tag, "Color", color);
            this.emission = NBTHelper.getInt(tag, "Emission", 0);
            this.material = new Material(tag.getCompound("Material"));
            this.particleEmitter = new ParticleEmitter(tag.getCompound("ParticleEmitter"));
            this.soundEmitter = new SoundEmitter(tag.getCompound("SoundEmitter"));
            this.playerPose = new PlayerPose(tag.getCompound("PlayerPose"));
            this.sprite = new Sprite(tag.getCompound("Sprite"));
            this.mask = NBTHelper.getInt(tag, "Mask", this.mask);
            this.bakedTextures = new ElementBakedTextures(
                    tag.getCompound("BakedTexture"),
                    tag.getCompound("BakedTextures")
            );
        }

        public Element(Element element) {
            this.from = new Vector3f(element.from);
            this.to = new Vector3f(element.to);
            this.axis = element.axis;
            this.rotation = element.rotation;
            this.type = element.type;
            this.color = element.color;
            this.emission = element.emission;
            this.mask = element.mask;
            this.material = new Material(element.material);
            this.particleEmitter = new ParticleEmitter(element.particleEmitter);
            this.soundEmitter = new SoundEmitter(element.soundEmitter);
            this.playerPose = new PlayerPose(element.playerPose);
            this.sprite = new Sprite(element.sprite);
            this.bakedTextures = new ElementBakedTextures();
            this.rotationAxes = null;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("From", NBTHelper.getFloatList(from));
            tag.put("To", NBTHelper.getFloatList(to));
            tag.putString("Axis", axis.getSerializedName());
            tag.putFloat("Rotation", rotation);
            tag.putString("Type", type.name().toLowerCase());
            tag.putInt("Color", color);
            tag.putInt("Emission", emission);
            tag.putInt("Mask", mask);

            if (type == ElementType.ELEMENT) {
                tag.put("Material", material.toTag());
                bakedTextures.save(tag);
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

        public float getVolume() {
            Vector3i size = getSize();
            return size.x * size.y * size.z;
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
            if (sprite.item) {
                sprite.tiled = false;

                // TODO: Remove this once most people ported to 0.1.0
                sprite.sprite = new ResourceLocation("minecraft:item/bread");
            }

            // Pose anchors are the shape of the players' butt
            if (type == ElementType.PLAYER_POSE) {
                Vector3f center = getCenter();
                from.x = center.x - 4.0f;
                from.y = center.y - 1.0f;
                from.z = center.z - (playerPose.pose == Pose.SLEEPING ? 14.0f : 4.0f);
                to.x = center.x + 4.0f;
                to.y = center.y + 1.0f;
                to.z = center.z + (playerPose.pose == Pose.SLEEPING ? 14.0f : 4.0f);
                axis = Direction.Axis.Y;
            } else if (type == ElementType.SPRITE) {
                // Sprites are forced to be 16x16x0
                Vector3f center = getCenter();
                if (!sprite.tiled) {
                    from.x = center.x - 8.0f * sprite.size;
                    from.y = center.y - 8.0f * sprite.size;
                    to.x = center.x + 8.0f * sprite.size;
                    to.y = center.y + 8.0f * sprite.size;
                }
                to.z = from.z;
            } else if (type == ElementType.ELEMENT) {
                // While elements could be tinted, there is no gui to do so
                color = -1;
            }

            // Reduce the risk of weird rounding errors
            from.x = Math.round(from.x * 64.0f) / 64.0f;
            from.y = Math.round(from.y * 64.0f) / 64.0f;
            from.z = Math.round(from.z * 64.0f) / 64.0f;
            to.x = Math.round(to.x - from.x) + from.x;
            to.y = Math.round(to.y - from.y) + from.y;
            to.z = Math.round(to.z - from.z) + from.z;
        }

        public boolean contains(Vector3f pos) {
            return contains(pos, 0.0001f);
        }

        public boolean contains(Vector3f pos, float margin) {
            return pos.x >= from.x - margin && pos.x <= to.x + margin &&
                   pos.y >= from.y - margin && pos.y <= to.y + margin &&
                   pos.z >= from.z - margin && pos.z <= to.z + margin;
        }

        public void move(float x, float y, float z) {
            from.add(x, y, z);
            to.add(x, y, z);
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

        public Vector3f sampleRandomPosition(RandomSource random, Direction direction) {
            ElementRotationAxes axes = getRotationAxes();
            float x = random.nextFloat() - 0.5f;
            float y = random.nextFloat() - 0.5f;
            float z = random.nextFloat() - 0.5f;
            Vector3f pos = new Vector3f(
                    axes.center.x + x * axes.right.x + y * axes.up.x + z * axes.forward.x,
                    axes.center.y + x * axes.right.y + y * axes.up.y + z * axes.forward.y,
                    axes.center.z + x * axes.right.z + y * axes.up.z + z * axes.forward.z
            );
            if (direction != null) {
                return rotate(pos, direction);
            }
            return pos;
        }

        public Vector3f getGlobalDirectionNormal(Direction direction) {
            return ModelUtils.rotate(direction.step(), axis, rotation);
        }

        public boolean isFlat() {
            return from.x == to.x || from.y == to.y || from.z == to.z;
        }

        public boolean isMasked(int state) {
            return (mask & (1 << state)) != 0;
        }
    }

    public static class ElementBakedTextures {
        public Map<Direction, Map<Integer, int[]>> textures = new ConcurrentHashMap<>();

        public ElementBakedTextures() {

        }

        public ElementBakedTextures(CompoundTag primary, CompoundTag secondary) {
            loadTextures(primary);
            loadTextures(secondary);
        }

        private void loadTextures(CompoundTag secondary) {
            for (String key : secondary.getAllKeys()) {
                String[] split = key.split(":");
                Direction direction = Direction.CODEC.byName(split[0]);
                int state = split.length == 1 ? 0 : Integer.parseInt(split[1]);
                put(direction, state, secondary.getIntArray(key));
            }
        }

        public void save(CompoundTag tag) {
            CompoundTag primary = new CompoundTag();
            CompoundTag secondary = new CompoundTag();
            for (Map.Entry<Direction, Map<Integer, int[]>> directionMapEntry : textures.entrySet()) {
                Direction direction = directionMapEntry.getKey();
                Map<Integer, int[]> stateMap = directionMapEntry.getValue();
                for (Map.Entry<Integer, int[]> stateEntry : stateMap.entrySet()) {
                    int state = stateEntry.getKey();
                    int[] texture = stateEntry.getValue();
                    if (state == 0) {
                        primary.putIntArray(direction.getSerializedName(), texture);
                    } else {
                        secondary.putIntArray(direction.getSerializedName() + ":" + state, texture);
                    }
                }
            }
            tag.put("BakedTexture", primary);
            tag.put("BakedTextures", secondary);
        }

        public void clear() {
            textures.clear();
        }

        public int[] get(Direction direction, int state) {
            Map<Integer, int[]> states = textures.get(direction);
            return states == null ? null : states.getOrDefault(state, states.get(0));
        }

        public void put(Direction direction, int state, int[] baked) {
            Map<Integer, int[]> states = textures.computeIfAbsent(direction, k -> new HashMap<>());
            if (state == 0 || !states.containsKey(0) || !Arrays.equals(states.get(0), baked)) {
                states.put(state, baked);
            }
        }
    }

    public enum WrapMode {
        EXPAND,
        REPEAT,
    }

    public enum MaterialAxis {
        X,
        Y,
        Z
    }

    public static class Material {
        public ResourceLocation source = new ResourceLocation("minecraft:oak_log");
        public int margin = 4;
        public WrapMode wrap = WrapMode.EXPAND;
        public MaterialAxis axis = MaterialAxis.X;
        public TransparencyType transparency = TransparencyType.SOLID;

        public LightMaterialEffect lightEffect = new LightMaterialEffect();

        public Material() {

        }

        public Material(CompoundTag tag) {
            source = NBTHelper.getResourceLocation(tag, "Source", source);
            margin = NBTHelper.getInt(tag, "Margin", margin);
            wrap = NBTHelper.getEnum(tag, WrapMode.class, "Wrap", wrap);
            axis = NBTHelper.getEnum(tag, MaterialAxis.class, "Axis", axis);
            transparency = NBTHelper.getEnum(tag, TransparencyType.class, "Transparency", transparency);
            lightEffect.load(tag.getCompound("LightEffect"));
        }

        public Material(Material material) {
            source = material.source;
            margin = material.margin;
            wrap = material.wrap;
            axis = material.axis;
            transparency = material.transparency;
            lightEffect = new LightMaterialEffect(material.lightEffect);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Source", source.toString());
            tag.putInt("Margin", margin);
            tag.putString("Wrap", wrap.name());
            tag.putString("Axis", axis.name());
            tag.putString("Transparency", transparency.name());
            tag.put("LightEffect", lightEffect.save());
            return tag;
        }
    }

    public static class ParticleEmitter {
        public ResourceLocation particle = new ResourceLocation("minecraft:smoke");
        public float velocityDirectional = 0.0f;
        public float velocityRandom = 0.1f;
        public float amount = 0.5f;
        public boolean onInteract = false;

        public ParticleEmitter() {

        }

        public ParticleEmitter(CompoundTag tag) {
            this.particle = NBTHelper.getResourceLocation(tag, "Particle", particle);
            this.velocityDirectional = NBTHelper.getFloat(tag, "VelocityDirectional", velocityDirectional);
            this.velocityRandom = NBTHelper.getFloat(tag, "VelocityRandom", velocityRandom);
            this.amount = NBTHelper.getFloat(tag, "Amount", amount);
            this.onInteract = NBTHelper.getBoolean(tag, "OnInteract", onInteract);
        }

        public ParticleEmitter(ParticleEmitter particleEmitter) {
            this.particle = particleEmitter.particle;
            this.velocityDirectional = particleEmitter.velocityDirectional;
            this.velocityRandom = particleEmitter.velocityRandom;
            this.amount = particleEmitter.amount;
            this.onInteract = particleEmitter.onInteract;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Particle", particle.toString());
            tag.putFloat("VelocityDirectional", velocityDirectional);
            tag.putFloat("VelocityRandom", velocityRandom);
            tag.putFloat("Amount", amount);
            tag.putBoolean("OnInteract", onInteract);
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
            this.sound = NBTHelper.getResourceLocation(tag, "Sound", sound);
            this.volume = NBTHelper.getFloat(tag, "Volume", volume);
            this.pitch = NBTHelper.getFloat(tag, "Pitch", pitch);
            this.frequency = NBTHelper.getFloat(tag, "Frequency", frequency);
            this.onInteract = NBTHelper.getBoolean(tag, "OnInteract", onInteract);
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
            this.pose = NBTHelper.getEnum(tag, Pose.class, "Pose", pose);
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
        public ResourceLocation sprite = new ResourceLocation("minecraft:block/soul_fire_1");
        public int rotation = 0;
        public float size = 1.0f;
        public boolean tiled = false;
        public boolean item = false;
        public boolean align = false;

        public Sprite() {
        }

        public Sprite(CompoundTag tag) {
            this.sprite = NBTHelper.getResourceLocation(tag, "Sprite", sprite);
            this.rotation = NBTHelper.getInt(tag, "Rotation", rotation);
            this.size = NBTHelper.getFloat(tag, "Size", size);
            this.tiled = NBTHelper.getBoolean(tag, "Tiled", tiled);
            this.item = NBTHelper.getBoolean(tag, "Item", item);
            this.align = NBTHelper.getBoolean(tag, "Align", align);
        }

        public Sprite(Sprite sprite) {
            this.sprite = sprite.sprite;
            this.rotation = sprite.rotation;
            this.size = sprite.size;
            this.tiled = sprite.tiled;
            this.item = sprite.item;
            this.align = sprite.align;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Sprite", sprite.toString());
            tag.putInt("Rotation", rotation);
            tag.putFloat("Size", size);
            tag.putBoolean("Tiled", tiled);
            tag.putBoolean("Item", item);
            tag.putBoolean("Align", align);
            return tag;
        }
    }

    public static class LightMaterialEffect {
        public float roundness = 0.0f;
        public float brightness = 0.0f;
        public float contrast = 0.0f;

        public float hue = 0.0f;
        public float saturation = 0.0f;
        public float value = 0.0f;

        public LightMaterialEffect() {

        }

        public LightMaterialEffect(LightMaterialEffect lightEffect) {
            this.roundness = lightEffect.roundness;
            this.brightness = lightEffect.brightness;
            this.contrast = lightEffect.contrast;

            this.hue = lightEffect.hue;
            this.saturation = lightEffect.saturation;
            this.value = lightEffect.value;
        }

        public void load(CompoundTag tag) {
            roundness = NBTHelper.getFloat(tag, "Roundness", roundness);
            brightness = NBTHelper.getFloat(tag, "Brightness", brightness);
            contrast = NBTHelper.getFloat(tag, "Contrast", contrast);

            hue = NBTHelper.getFloat(tag, "Hue", hue);
            saturation = NBTHelper.getFloat(tag, "Saturation", saturation);
            value = NBTHelper.getFloat(tag, "Value", value);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("Roundness", roundness);
            tag.putFloat("Brightness", brightness);
            tag.putFloat("Contrast", contrast);

            tag.putFloat("Hue", hue);
            tag.putFloat("Saturation", saturation);
            tag.putFloat("Value", value);
            return tag;
        }
    }
}
