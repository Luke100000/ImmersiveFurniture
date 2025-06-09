package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.InteractionManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void move(double distanceOffset, double verticalOffset, double horizontalOffset);

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void immersiveFurniture$setup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.isSleeping()) {
            InteractionManager.Interaction interaction = InteractionManager.INSTANCE.getInteraction(livingEntity);
            if (interaction != null) {
                setRotation(interaction.offset().rotation(), 0f);
                Vector3f offset = interaction.offset().offset();
                move(offset.x - 0.5, offset.y - 0.5 + 0.3, offset.z - 0.5);
            }
        }
    }
}
