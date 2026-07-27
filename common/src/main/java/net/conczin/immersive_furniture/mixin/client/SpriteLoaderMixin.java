package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.config.Config;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteSource.Output.class)
public interface SpriteLoaderMixin {
    @Inject(method = "add(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/server/packs/resources/Resource;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$addFurnitureSprite(ResourceLocation location, Resource resource, CallbackInfo ci) {
        if (location.equals(Common.locate("block/furniture"))) {
            int size = Config.getInstance().getBakedAtlasSize();
            ((SpriteSource.Output) this).add(location, loader -> new SpriteContents(
                    location,
                    new FrameSize(size, size),
                    new NativeImage(size, size, true),
                    ResourceMetadata.EMPTY
            ));
            ci.cancel();
        }
    }
}
