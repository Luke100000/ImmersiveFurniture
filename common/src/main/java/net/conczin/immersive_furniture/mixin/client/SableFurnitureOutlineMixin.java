package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.block.FurnitureProxyBlock;
import net.conczin.immersive_furniture.client.renderer.CachedShapeRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders furniture outlines in Sable sublevels when Sable is installed.
 */
@Mixin(value = LevelRenderer.class, priority = 50)
public abstract class SableFurnitureOutlineMixin {
    @Inject(method = "renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$renderSableFurnitureOutline(PoseStack poseStack, VertexConsumer consumer, Entity entity,
                                                                double camX, double camY, double camZ, BlockPos pos,
                                                                BlockState state, CallbackInfo ci) {
        if (!(state.getBlock() instanceof BaseFurnitureBlock) && !(state.getBlock() instanceof FurnitureProxyBlock))
            return;

        ClientSubLevelAccess subLevel = SableCompanion.INSTANCE.getContainingClient(pos);
        if (subLevel == null) return;

        Pose3dc renderPose = subLevel.renderPose();
        poseStack.pushPose();
        poseStack.translate(
                renderPose.position().x() - camX,
                renderPose.position().y() - camY,
                renderPose.position().z() - camZ
        );
        poseStack.mulPose(new Quaternionf(renderPose.orientation()));
        poseStack.translate(
                pos.getX() - renderPose.rotationPoint().x(),
                pos.getY() - renderPose.rotationPoint().y(),
                pos.getZ() - renderPose.rotationPoint().z()
        );
        CachedShapeRenderer.renderShape(poseStack, consumer, state.getShape(entity.level(), pos, CollisionContext.of(entity)), 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F, 0.4F);
        poseStack.popPose();
        ci.cancel();
    }
}
