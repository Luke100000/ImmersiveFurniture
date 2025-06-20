package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
    @Accessor("byMipLevel")
    NativeImage[] getMipLevelData();

    @Invoker("getFrameCount")
    int immersiveFurniture$getFrameCount();
}
