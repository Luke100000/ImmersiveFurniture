package immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

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
        String fallback = StringUtils.capitalize(location.getPath().replace("/", " ").replace("_", " "));
        return Component.translatableWithFallback(key, fallback);
    }

    private static @Nullable Material getMaterial(ResourceLocation location) {
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

    // TODO: This is for custom materials
    public static MaterialSource create(ResourceLocation location) {
        Material material = getMaterial(location);
        if (material == null) return null;
        return new MaterialSource(
                location,
                findMaterial(location, material, "_down", "_bottom", "_top"),
                findMaterial(location, material, "_up", "_top"),
                findMaterial(location, material, "_north", "_side"),
                findMaterial(location, material, "_south", "_side"),
                findMaterial(location, material, "_west", "_side"),
                findMaterial(location, material, "_east", "_side")
        );
    }

    public static MaterialSource create(BlockModel model) {
        if (model.getElements().size() != 1) return null;

        BlockElement element = model.getElements().get(0);
        if (element.from.x() != 0 || element.from.y() != 0 || element.from.z() != 0) return null;
        if (element.to.x() != 16 || element.to.y() != 16 || element.to.z() != 16) return null;

        Material down = model.getMaterial(element.faces.get(Direction.DOWN).texture);
        Material up = model.getMaterial(element.faces.get(Direction.UP).texture);
        Material north = model.getMaterial(element.faces.get(Direction.SOUTH).texture);
        Material south = model.getMaterial(element.faces.get(Direction.NORTH).texture);
        Material west = model.getMaterial(element.faces.get(Direction.WEST).texture);
        Material east = model.getMaterial(element.faces.get(Direction.EAST).texture);

        // Not all models are fully textured
        if (down.texture().equals(MISSING) || up.texture().equals(MISSING) || north.texture().equals(MISSING) || south.texture().equals(MISSING) || west.texture().equals(MISSING) || east.texture().equals(MISSING)) {
            return null;
        }

        return new MaterialSource(
                new ResourceLocation(model.name.replace(":block/", ":")),
                down,
                up,
                north,
                south,
                west,
                east
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
            return coord > size / 2 ? textureSize - (size - coord) : coord;
        } else {
            // Inner part, mirror repeat
            int w = Math.max(1, textureSize - margin * 2);
            int n = Math.max(1, size - margin * 2);
            return wrap(coord - margin, w, n) + margin;
        }
    }

    public static int fromCube(FurnitureData.Material material, Direction direction, int x, int y, int w, int h) {
        NativeImage texture = material.source.get(direction);

        if (material.wrap == FurnitureData.WrapMode.EXPAND) {
            x = smartWrap(x, w, texture.getWidth(), material.margin);
            y = smartWrap(y, h, texture.getHeight(), material.margin);
        }

        x = x % texture.getWidth();
        y = y % texture.getHeight();

        return texture.getPixelRGBA(x, y);
    }
}
