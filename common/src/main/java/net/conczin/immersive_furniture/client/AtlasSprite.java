package net.conczin.immersive_furniture.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class AtlasSprite {
    private static int lastBakedAtlasId = -1;
    private static SpriteContents lastBakedSpriteContents;

    public static void syncBakedAtlas() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(AtlasSprite::syncBakedAtlas);
            return;
        }
        if (DynamicAtlas.BAKED.getUsage() == 0) {
            return;
        }

        // Fetch sprite
        ResourceLocation location = Common.locate("block/furniture");
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(location);
        if (!sprite.contents().name().equals(location)) {
            return;
        }

        // Check if sync is required
        int atlasId = DynamicAtlas.BAKED.getLastStateId();
        if (atlasId == lastBakedAtlasId && sprite.contents() == lastBakedSpriteContents) {
            return;
        }

        NativeImage source = DynamicAtlas.BAKED.getPixels();
        if (source == null) return;

        SpriteContents contents = sprite.contents();
        NativeImage[] content = ((SpriteContentsAccessor) contents).getMipLevelData();

        // Copy the main image
        copyRect(source, content[0], 0, 0, contents.width(), contents.height());

        // Create mipmaps
        for (int i = 1; i < content.length; ++i) {
            mipTheMap(content[i - 1], content[i], 0, 0, contents.width() >> i, contents.height() >> i);
        }

        // Upload
        Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).bind();
        upload(sprite.getX(), sprite.getY(), contents.width(), contents.height(), content);

        lastBakedAtlasId = atlasId;
        lastBakedSpriteContents = sprite.contents();
    }

    private static void upload(int x, int y, int width, int height, NativeImage[] atlasData) {
        for (int i = 0; i < atlasData.length; ++i) {
            atlasData[i].upload(i, x >> i, y >> i, 0, 0, width >> i, height >> i, atlasData.length > 1, false);
        }
    }

    private static void copyRect(NativeImage source, NativeImage destination, int xTo, int yTo, int width, int height) {
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int m = source.getPixelRGBA(j, i);
                destination.setPixelRGBA(xTo + j, yTo + i, m);
            }
        }
    }

    private static int blend(int... colors) {
        int r = 0, g = 0, b = 0, a = 0;
        for (int c : colors) {
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
            a = Math.max(a, (c >> 24) & 0xFF);
        }
        r /= colors.length;
        g /= colors.length;
        b /= colors.length;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void mipTheMap(NativeImage source, NativeImage destination, int xTo, int yTo, int width, int height) {
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int m = blend(
                        source.getPixelRGBA(j * 2, i * 2),
                        source.getPixelRGBA(j * 2 + 1, i * 2),
                        source.getPixelRGBA(j * 2, i * 2 + 1),
                        source.getPixelRGBA(j * 2 + 1, i * 2 + 1)
                );
                destination.setPixelRGBA(xTo + j, yTo + i, m);
            }
        }
    }
}