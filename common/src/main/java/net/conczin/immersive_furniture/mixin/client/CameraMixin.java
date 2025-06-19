package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.InteractionManager;
import net.minecraft.client.Camera;
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

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract Vec3 getPosition();

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void immersiveFurniture$setup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.isSleeping()) {
            InteractionManager.Interaction interaction = InteractionManager.INSTANCE.getInteraction(livingEntity);
            if (interaction != null) {
                setRotation(interaction.offset().rotation(), 0f);
                Vector3f offset = interaction.offset().offset();
                Vec3 position = getPosition();
                setPosition(
                        position.x + offset.x - 0.5,
                        position.y + offset.y - 0.5,
                        position.z + offset.z - 0.5
                );
            }
        }
    }
}
