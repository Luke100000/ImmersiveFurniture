package immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Optional;

public class TextureSource {
    public static final ResourceLocation MISSING = new ResourceLocation("minecraft:missingno");

    public record TextureData(NativeImage down, NativeImage up, NativeImage north, NativeImage south, NativeImage west,
                              NativeImage east) {
        public NativeImage get(Direction direction) {
            return switch (direction) {
                case DOWN -> down;
                case UP -> up;
                case NORTH -> north;
                case SOUTH -> south;
                case WEST -> west;
                case EAST -> east;
            };
        }
    }

    public static SpriteContents getSpriteContents(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location).contents();
    }

    public static Optional<NativeImage> getTexture(ResourceLocation location, String suffix) {
        return getTexture(location.withSuffix(suffix));
    }

    public static Optional<NativeImage> getTexture(ResourceLocation location) {
        SpriteContents spriteContents = getSpriteContents(location);
        if (spriteContents.name().equals(MISSING)) {
            return Optional.empty();
        }
        return Optional.of(((SpriteContentsAccessor) spriteContents).getMipLevelData()[0]);
    }

    public static ResourceLocation withPrefix(ResourceLocation location, String prefix) {
        return new ResourceLocation(location.getNamespace(), prefix + location.getPath());
    }

    public static TextureData getTextureData(ResourceLocation location) {
        Optional<NativeImage> texture = getTexture(withPrefix(location, ""));
        return texture.map(nativeImage -> new TextureData(
                getTexture(location, "_down").orElse(getTexture(location, "_bottom").orElse(nativeImage)),
                getTexture(location, "_up").orElse(getTexture(location, "_top").orElse(nativeImage)),
                getTexture(location, "_north").orElse(getTexture(location, "_side").orElse(nativeImage)),
                getTexture(location, "_south").orElse(getTexture(location, "_side").orElse(nativeImage)),
                getTexture(location, "_west").orElse(getTexture(location, "_side").orElse(nativeImage)),
                getTexture(location, "_east").orElse(getTexture(location, "_side").orElse(nativeImage))
        )).orElse(null);
    }

    public static int wrap(int x, int w, int n) {
        if (w >= n) return x + (w - n) / 2;

        int cycleLength = 2 * w;
        int fullCycles = n / cycleLength;
        int remainder = n % cycleLength;

        int pos = x % (fullCycles * cycleLength + remainder);
        int block = pos / w;
        int offset = pos % w;

        return (block % 2 == 0) ? offset : (w - 1 - offset);
    }

    public static int smartWrap(int coord, int size, int textureSize, int margin) {
        if (coord < margin || coord >= size - margin) {
            // Outer part, mirror the last few pixels
            return (coord > size / 2) ? textureSize - (size - coord) : coord;
        } else {
            // Inner part, mirror repeat
            int w = Math.max(1, textureSize - margin * 2);
            int n = Math.max(1, size - margin * 2);
            return wrap(coord - margin, w, n) + margin;
        }
    }

    public static int fromCube(FurnitureData.Element element, Direction direction, int x, int y, int w, int h) {
        FurnitureData.Material material = element.material;
        TextureData data = getTextureData(material.texture);
        if (data == null) return 0;

        NativeImage texture = data.get(direction);

        if (material.wrap == FurnitureData.WrapMode.EXPAND) {
            x = smartWrap(x, w, texture.getWidth(), material.margin);
            y = smartWrap(y, h, texture.getHeight(), material.margin);
        }

        x = x % texture.getWidth();
        y = y % texture.getHeight();

        return texture.getPixelRGBA(x, y);
    }
}
