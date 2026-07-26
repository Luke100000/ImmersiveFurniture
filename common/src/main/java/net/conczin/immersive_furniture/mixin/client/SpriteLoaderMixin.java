package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.config.Config;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {
    private static final ResourceLocation FURNITURE_TEXTURE = Common.locate("block/furniture");

    @Inject(method = "loadSprite", at = @At("HEAD"), cancellable = true)
    private static void immersiveFurniture$loadSprite(ResourceLocation location, Resource resource, CallbackInfoReturnable<SpriteContents> cir) {
        if (location.equals(FURNITURE_TEXTURE)) {
            int size = Config.getInstance().getBakedAtlasSize();
            cir.setReturnValue(new SpriteContents(
                    location,
                    new FrameSize(size, size),
                    new NativeImage(size, size, true),
                    AnimationMetadataSection.EMPTY
            ));
        }
    }
}
