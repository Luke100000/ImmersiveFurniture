package immersive_furniture.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import immersive_furniture.CommonClient;
import immersive_furniture.block.FurnitureBlock;
import immersive_furniture.block.FurnitureBlockEntity;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.client.model.FurnitureModelBaker;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
    @Shadow
    @Final
    private ModelBlockRenderer modelRenderer;

    @Inject(method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderBatched(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, CallbackInfo ci) {
        if (state.getBlock() instanceof FurnitureBlock) {
            int value = state.getValue(FurnitureBlock.IDENTIFIER);
            if (value == 0 && level.getBlockEntity(pos) instanceof FurnitureBlockEntity blockEntity) {
                FurnitureData data = blockEntity.getData();

                if (data != null) {
                    BakedModel model = FurnitureModelBaker.getModel(data, DynamicAtlas.BAKED);
                    modelRenderer.tesselateBlock(level, model, state, pos, poseStack, consumer, checkSides, random, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
                } else {
                    // The model is not yet available (block entity not loaded, or lite model not downloaded yet).
                    CommonClient.delayRendering(() -> blockEntity.getData() != null, pos);
                }
            } else if (value > 0) {
                // TODO
            }
            ci.cancel();
        }
    }
}
