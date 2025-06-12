package net.conczin.immersive_furniture.mixin.client;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {
    @Accessor
    List<SpriteContents> getSprites();
}
