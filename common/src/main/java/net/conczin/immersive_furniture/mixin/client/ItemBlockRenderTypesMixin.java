package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.client.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to handle render types for furniture blocks based on their transparency property.
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Inject(method = "getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"),
            cancellable = true)
    private static void immersiveFurniture$onGetChunkRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        if (state.getBlock() instanceof BaseFurnitureBlock) {
            TransparencyType transparencyType = state.getValue(BaseFurnitureBlock.TRANSPARENCY);
            cir.setReturnValue(FurnitureBakedModelWrapper.getRenderType(transparencyType));
        }
    }
}