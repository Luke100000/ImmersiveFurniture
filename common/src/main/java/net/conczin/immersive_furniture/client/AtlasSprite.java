package net.conczin.immersive_furniture.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;

public class AtlasSprite {
    public static class Ticker implements SpriteTicker {
        private final SpriteContents spriteContents;
        private final SpriteContentsAccessor spriteContentsAccessor;
        private float lastUtilization = 0.0f;

        public Ticker(SpriteContents spriteContents) {
            this.spriteContents = spriteContents;
            this.spriteContentsAccessor = (SpriteContentsAccessor) spriteContents;
        }

        void upload(int x, int y, NativeImage[] atlasData) {
            for (int i = 0; i < atlasData.length; ++i) {
                atlasData[i].upload(i, x >> i, y >> i, 0, 0, spriteContents.width() >> i, spriteContents.height() >> i, atlasData.length > 1, false);
            }
        }

        @Override
        public void tickAndUpload(int x, int y) {
            float usage = DynamicAtlas.BAKED.getUsage();
            if (lastUtilization == usage || usage == 0) return;
            lastUtilization = usage;

            NativeImage source = DynamicAtlas.BAKED.getPixels();
            assert source != null;
            NativeImage[] content = spriteContentsAccessor.getMipLevelData();

            // Copy the main image
            copyRect(source, content[0], x, y, spriteContents.width(), spriteContents.height());

            // Create mipmaps
            for (int i = 1; i < content.length; ++i) {
                mipTheMap(content[i - 1], content[i], x >> i, y >> i, spriteContents.width() >> i, spriteContents.height() >> i);
            }

            upload(x, y, content);
        }

        public void copyRect(NativeImage source, NativeImage destination, int xTo, int yTo, int width, int height) {
            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    int m = source.getPixelRGBA(j, i);
                    destination.setPixelRGBA(xTo + j, yTo + i, m);
                }
            }
        }

        static int blend(int... colors) {
            int r = 0, g = 0, b = 0, a = 0;
            for (int c : colors) {
                r += (c >> 24) & 0xFF;
                g += (c >> 16) & 0xFF;
                b += (c >> 8) & 0xFF;
                a = Math.max(a, c & 0xFF);
            }
            r /= colors.length;
            g /= colors.length;
            b /= colors.length;
            return (r << 24) | (g << 16) | (b << 8) | a;
        }

        public void mipTheMap(NativeImage source, NativeImage destination, int xTo, int yTo, int width, int height) {
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

        @Override
        public void close() {
            // Nop
        }
    }
}