package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SuspiciousNameCombination")
public record MaterialSource(
        ResourceLocation location,
        RotatedMaterial down,
        RotatedMaterial up,
        RotatedMaterial north,
        RotatedMaterial south,
        RotatedMaterial west,
        RotatedMaterial east
) {
    public static final MaterialSource DEFAULT = new MaterialSource(
            new ResourceLocation("minecraft:oak_log"),
            new RotatedMaterial("minecraft:block/oak_log_top"),
            new RotatedMaterial("minecraft:block/oak_log_top"),
            new RotatedMaterial("minecraft:block/oak_log"),
            new RotatedMaterial("minecraft:block/oak_log"),
            new RotatedMaterial("minecraft:block/oak_log"),
            new RotatedMaterial("minecraft:block/oak_log")
    );

    public NativeImage get(Direction direction) {
        return getImage(getMaterial(direction).sprite());
    }

    public RotatedMaterial getMaterial(Direction direction) {
        return switch (direction) {
            case DOWN -> down;
            case UP -> up;
            case NORTH -> north;
            case SOUTH -> south;
            case WEST -> west;
            case EAST -> east;
        };
    }

    public static NativeImage getImage(TextureAtlasSprite sprite) {
        return ((SpriteContentsAccessor) sprite.contents()).getMipLevelData()[0];
    }

    public Component name() {
        String key = "block." + location.getNamespace() + "." + location.getPath();
        return Component.translatableWithFallback(key, Utils.capitalize(location));
    }

    private static final RandomSource random = RandomSource.create();

    public static MaterialSource create(BlockState state) {
        BakedModel model = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(state);

        Map<Direction, RotatedMaterial> materials = new HashMap<>();
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, random);
            if (quads.size() != 1) return null;

            TextureAtlasSprite sprite = quads.get(0).getSprite();
            int width = sprite.contents().width();
            int height = sprite.contents().height();
            if (width != height || Math.pow((int) Math.sqrt(width), 2) != width) return null;

            int rotation = getRotation(quads);

            ResourceLocation name = sprite.contents().name();
            materials.put(direction, new RotatedMaterial(sprite.atlasLocation(), name, rotation));
        }

        ResourceLocation name = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return new MaterialSource(
                name,
                materials.get(Direction.DOWN),
                materials.get(Direction.UP),
                materials.get(Direction.NORTH),
                materials.get(Direction.SOUTH),
                materials.get(Direction.WEST),
                materials.get(Direction.EAST)
        );
    }

    private static int getRotation(List<BakedQuad> quads) {
        int[] v = quads.get(0).getVertices();
        float u0 = Float.intBitsToFloat(v[4]);
        float v0 = Float.intBitsToFloat(v[5]);
        float u1 = Float.intBitsToFloat(v[16 + 4]);
        float v1 = Float.intBitsToFloat(v[16 + 5]);

        int rotation = 0;
        if (u0 > u1) {
            if (v0 > v1) {
                rotation += 2;
            } else {
                rotation += 3;
            }
        } else if (v0 > v1) {
            rotation += 1;
        }
        return rotation;
    }

    public static int wrap(int x, int w, int n) {
        if (w >= n) return x + (w - n) / 2;

        int cycleLength = 2 * w;
        int fullCycles = n / cycleLength;
        int remainder = n % cycleLength;

        int pos = x % (fullCycles * cycleLength + remainder);
        int block = pos / w;
        int offset = pos % w;

        return block % 2 == 0 ? offset : w - 1 - offset;
    }

    public static int smartWrap(int coord, int size, int textureSize, int margin) {
        if (coord < margin || coord >= size - margin) {
            // Outer part, mirror the last few pixels
            return coord >= size / 2 ? textureSize - (size - coord) : coord;
        } else {
            // Inner part, mirror repeat
            int w = Math.max(1, textureSize - margin * 2);
            int n = Math.max(1, size - margin * 2);
            return wrap(coord - margin, w, n) + margin;
        }
    }

    public static int fromCube(FurnitureData.Material material, Direction direction, Vector3f center, int x, int y, int w, int h) {
        // Rotate the direction based on the axis
        Direction rotatedDirection = direction;
        int axisOrdinal = material.axis.ordinal();
        if (axisOrdinal > 0) {
            int shift = 2 * axisOrdinal;
            int directionOrdinal = (direction.ordinal() + shift) % 6;
            rotatedDirection = Direction.values()[directionOrdinal];
        }

        // Fetch texture data
        MaterialSource source = MaterialRegistry.INSTANCE.materials.getOrDefault(
                material.source,
                MaterialSource.DEFAULT
        );
        NativeImage texture = source.get(rotatedDirection);

        if (direction == Direction.EAST || direction == Direction.NORTH) x = w - x - 1;
        if (direction == Direction.DOWN) y = h - y - 1;

        if (material.wrap == FurnitureData.WrapMode.REPEAT) {
            x += (int) (center.x += center.y);
            y += (int) (center.z += center.y);
        }

        int tw = texture.getWidth();
        int th = texture.getHeight();
        if (material.wrap == FurnitureData.WrapMode.EXPAND) {
            x = smartWrap(x, w, tw, material.margin);
            y = smartWrap(y, h, th, material.margin);
        }

        x = x % tw;
        y = y % th;

        if (material.axis != FurnitureData.MaterialAxis.X && direction == Direction.NORTH) {
            x = tw - x - 1;
        }
        if (material.axis != FurnitureData.MaterialAxis.Y && direction == Direction.DOWN) {
            y = th - y - 1;
        }
        if (material.axis == FurnitureData.MaterialAxis.X && direction == Direction.EAST) {
            x = tw - x - 1;
        }

        // Rotate uv as well
        int rotation = source.getMaterial(rotatedDirection).getRotation();
        if (material.axis == FurnitureData.MaterialAxis.Y) {
            switch (direction) {
                case DOWN, UP, SOUTH -> rotation += 1;
                case NORTH -> rotation += 3;
                case EAST -> rotation += 2;
            }
        } else if (material.axis == FurnitureData.MaterialAxis.Z) {
            switch (direction) {
                case UP -> rotation += 2;
                case NORTH, SOUTH -> rotation += 3;
                case WEST, EAST -> rotation += 1;
            }
        }

        // Rotate UV
        rotation = rotation % 4;
        if (rotation == 1) {
            int temp = x;
            x = y;
            y = tw - temp - 1;
        } else if (rotation == 2) {
            x = tw - x - 1;
            y = th - y - 1;
        } else if (rotation == 3) {
            int temp = x;
            x = th - y - 1;
            y = temp;
        }

        return texture.getPixelRGBA(x, y);
    }
}
