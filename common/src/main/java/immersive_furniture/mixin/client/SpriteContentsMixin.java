package immersive_furniture.mixin.client;

import immersive_furniture.Common;
import immersive_furniture.client.AtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.renderer.texture.SpriteContents.class)
public abstract class SpriteContentsMixin {
    @Shadow
    public abstract ResourceLocation name();

    @Inject(method = "createTicker()Lnet/minecraft/client/renderer/texture/SpriteTicker;", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$createTicker(CallbackInfoReturnable<SpriteTicker> cir) {
        // We use a custom ticker which keeps the block atlas in sync with the dynamic furniture atlas.
        if (this.name().equals(Common.locate("block/furniture"))) {
            cir.setReturnValue(new AtlasSprite.Ticker((SpriteContents) (Object) this));
        }
    }
}
