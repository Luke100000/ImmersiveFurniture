package immersive_furniture.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import immersive_furniture.block.BaseFurnitureBlock;
import immersive_furniture.client.FurnitureRenderer;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.client.model.FurnitureModelBaker;
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
import org.spongepowered.asm.mixin.Unique;
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
        if (state.getBlock() instanceof BaseFurnitureBlock) {
            immersive_furniture$render(state, pos, level, poseStack, consumer, checkSides, random);
            ci.cancel();
        }
    }

    @Unique
    private void immersive_furniture$render(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random) {
        // Check if data is available already
        FurnitureRenderer.Status status = FurnitureRenderer.getLoadedStatus(pos);

        // Render it
        if (status.data() != null) {
            int yRot = (int) state.getValue(BaseFurnitureBlock.FACING).toYRot();
            BakedModel model = FurnitureModelBaker.getModel(status.data(), DynamicAtlas.BAKED, yRot, false);
            if (model != null) {
                modelRenderer.tesselateBlock(level, model, state, pos, poseStack, consumer, checkSides, random, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
            }
        } else if (!status.done()) {
            // Schedule a re-render
            FurnitureRenderer.delayRendering(pos);
        }
    }
}
