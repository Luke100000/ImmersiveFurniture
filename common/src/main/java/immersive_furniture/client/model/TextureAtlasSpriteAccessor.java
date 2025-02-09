package immersive_furniture.client.model;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class TextureAtlasSpriteAccessor extends TextureAtlasSprite {
    private final int originX;
    private final int originY;

    public TextureAtlasSpriteAccessor(ResourceLocation atlasLocation, SpriteContents contents, int originX, int originY, int x, int y) {
        super(atlasLocation, contents, originX, originY, x, y);

        this.originX = originX;
        this.originY = originY;
    }

    @Override
    public float uvShrinkRatio() {
        return 0.0f;
    }

    @Override
    public float getU(double u) {
        return (float) (u / this.originX);
    }

    @Override
    public float getUOffset(float offset) {
        return offset / this.originX;
    }

    @Override
    public float getV(double v) {
        return (float) (v / this.originY);
    }

    @Override
    public float getVOffset(float offset) {
        return offset / this.originY;
    }
}
