package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.InteractionManager;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 700)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void move(float zoom, float dy, float dx);

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void immersiveFurniture$setup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (!detached && entity instanceof LivingEntity livingEntity && livingEntity.isSleeping()) {
            InteractionManager.Interaction interaction = InteractionManager.INSTANCE.getInteraction(livingEntity);
            if (interaction != null) {
                move(0.0f, -0.3f, 0.0f);
                setRotation(interaction.offset().rotation(), 0f);
                Vector3f offset = interaction.offset().offset();
                BlockPos pos = interaction.pos();
                setPosition(
                        pos.getX() + (double) offset.x,
                        pos.getY() + (double) offset.y + 0.4,
                        pos.getZ() + (double) offset.z
                );
            }
        }
    }
}