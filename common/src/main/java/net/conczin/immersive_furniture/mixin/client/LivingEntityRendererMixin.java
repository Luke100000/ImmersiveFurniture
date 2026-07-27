package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.immersive_furniture.InteractionManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getBedOrientation()Lnet/minecraft/core/Direction;")
    )
    private void immersiveFurniture$render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        InteractionManager.Interaction interaction = InteractionManager.INSTANCE.getInteraction(entity);
        if (entity.isSleeping() && interaction != null) {
            float yaw = interaction.offset().rotation();
            double stepX = Math.cos(yaw * Math.PI / 180F + Math.PI / 2F);
            double stepZ = Math.sin(yaw * Math.PI / 180F + Math.PI / 2F);
            float eyeHeight = entity.getEyeHeight(Pose.STANDING) - 0.1F;
            poseStack.translate((float) (stepX) * eyeHeight, 0.0F, (float) (stepZ) * eyeHeight);
        }
    }
}
