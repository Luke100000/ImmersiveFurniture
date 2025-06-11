package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.MaterialRegistry;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record MaterialSource(
        ResourceLocation location,
        Material down,
        Material up,
        Material north,
        Material south,
        Material west,
        Material east
) {
    public static final ResourceLocation MISSING = new ResourceLocation("minecraft:missingno");
    public static final MaterialSource DEFAULT = new MaterialSource(
            new ResourceLocation("minecraft:oak_log"),
            new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/oak_log_top")),
            new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/oak_log_top")),
            new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/oak_log")),
            new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/oak_log")),
            new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/oak_log")),
            new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/oak_log"))
    );

    public NativeImage get(Direction direction) {
        return getImage(getMaterial(direction).sprite());
    }

    public Material getMaterial(Direction direction) {
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

    private static Material getMaterial(ResourceLocation location) {
        Material material = new Material(InventoryMenu.BLOCK_ATLAS, location);
        if (!material.sprite().contents().name().equals(MISSING)) {
            return material;
        }
        return null;
    }

    private static Material findMaterial(ResourceLocation location, Material fallback, String... suffixes) {
        for (String suffix : suffixes) {
            Material material = getMaterial(location.withSuffix(suffix));
            if (material != null) return material;
        }
        return fallback;
    }

    // TODO: This is for custom materials, added via resource packs or config
    public static MaterialSource create(ResourceLocation location) {
        Material material = getMaterial(location);
        if (material == null) return null;
        return new MaterialSource(
                location,
                findMaterial(location, material, "_down", "_bottom", "_top"),
                findMaterial(location, material, "_up", "_top"),
                findMaterial(location, material, "_front", "_north", "_side"),
                findMaterial(location, material, "_back", "_south", "_side"),
                findMaterial(location, material, "_west", "_side"),
                findMaterial(location, material, "_east", "_side")
        );
    }

    private static final RandomSource random = RandomSource.create();

    public static MaterialSource create(BlockState state) {
        BakedModel model;
        try {
            model = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(state);
        } catch (Exception e) {
            return null;
        }

        Map<Direction, Material> materials = new HashMap<>();
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, random);
            if (quads.size() != 1) return null;

            TextureAtlasSprite sprite = quads.get(0).getSprite();
            int width = sprite.contents().width();
            int height = sprite.contents().height();
            if (width != height || Math.pow((int) Math.sqrt(width), 2) != width) return null;

            ResourceLocation name = sprite.contents().name();
            materials.put(direction, new Material(sprite.atlasLocation(), name));
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
        NativeImage texture = MaterialRegistry.INSTANCE.materials.getOrDefault(
                material.source,
                MaterialSource.DEFAULT
        ).get(direction);

        if (material.wrap == FurnitureData.WrapMode.REPEAT) {
            //noinspection SuspiciousNameCombination
            x += (int) (center.x += center.y);
            y += (int) (center.z += center.y);
        }

        if (material.flip) {
            x = w - x - 1;
        }

        if (material.rotate) {
            int temp = x;
            //noinspection SuspiciousNameCombination
            x = y;
            y = temp;

            temp = w;
            w = h;
            h = temp;
        }

        if (material.wrap == FurnitureData.WrapMode.EXPAND) {
            x = smartWrap(x, w, texture.getWidth(), material.margin);
            y = smartWrap(y, h, texture.getHeight(), material.margin);
        }

        x = x % texture.getWidth();
        y = y % texture.getHeight();

        return texture.getPixelRGBA(x, y);
    }
}
