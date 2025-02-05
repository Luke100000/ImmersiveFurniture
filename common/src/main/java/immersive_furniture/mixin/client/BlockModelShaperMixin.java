package immersive_furniture.mixin.client;

import immersive_furniture.block.FurnitureBlock;
import immersive_furniture.data.FurnitureBakedModel;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelShaper.class)
public class BlockModelShaperMixin {
    @Inject(method = "getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    private void immersiveFuture$getBlockModel(BlockState state, CallbackInfoReturnable<BakedModel> cir) {
        if (state.getBlock() instanceof FurnitureBlock) {
            cir.setReturnValue(FurnitureBakedModel.getModel(state));
        }
    }
}
