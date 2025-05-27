package immersive_furniture.client;

import com.mojang.blaze3d.platform.NativeImage;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;

public class AtlasSprite {
    public static class Ticker implements SpriteTicker {
        private final SpriteContents spriteContents;
        private final SpriteContentsAccessor spriteContentsAccessor;

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
            NativeImage[] content = spriteContentsAccessor.getMipLevelData();

            // TODO: Only copy when there is something to copy
            // TODO: Resize when spriteContents width/height mismatches

            NativeImage source = DynamicAtlas.BAKED.getPixels();
            assert source != null;

            content[0].copyFrom(source);
            content[0].copyRect(source, 0, 0, x, y, spriteContents.width(), spriteContents.height(), false, false);

            // Remember about mipmaps!
            upload(x, y, content);
        }

        @Override
        public void close() {
            // Nop
        }
    }
}