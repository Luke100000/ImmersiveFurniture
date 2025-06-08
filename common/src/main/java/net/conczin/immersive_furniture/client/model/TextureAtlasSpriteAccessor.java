package net.conczin.immersive_furniture.client.model;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class TextureAtlasSpriteAccessor extends TextureAtlasSprite {
    public TextureAtlasSpriteAccessor(ResourceLocation atlasLocation, SpriteContents contents, int originX, int originY, int x, int y) {
        super(atlasLocation, contents, originX, originY, x, y);
    }
}
