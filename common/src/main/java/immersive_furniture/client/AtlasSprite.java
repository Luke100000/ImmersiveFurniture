package immersive_furniture.client;

import com.mojang.blaze3d.platform.NativeImage;
import immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;

import java.util.Random;

public class AtlasSprite {
    public static class Ticker implements SpriteTicker {
        private final SpriteContents spriteContents;
        private final SpriteContentsAccessor spriteContentsAccessor;

        private final Random random = new Random();

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
            spriteContentsAccessor.getMipLevelData()[0].fillRect(x, y, spriteContents.width(), spriteContents.height(), random.nextInt());
            // TODO: MipmapGenerator.generateMipmaps(spriteContentsAccessor.getMipLevelData());
            upload(x, y, spriteContentsAccessor.getMipLevelData());
        }

        @Override
        public void close() {
            // Nop
        }
    }
}