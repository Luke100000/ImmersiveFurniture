package net.conczin.immersive_furniture.mixin;

import net.conczin.immersive_furniture.InteractionManager;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract Optional<BlockPos> getSleepingPos();

    @Shadow
    public abstract boolean isSleeping();

    @Unique
    private @NotNull boolean immersiveFurniture$IsFurnitureBed() {
        return this.getSleepingPos().map(blockPos -> this.level().getBlockState(blockPos).getBlock() instanceof BaseFurnitureBlock).orElse(false);
    }

    @Inject(method = "checkBedExists()Z", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$checkBedExists(CallbackInfoReturnable<Boolean> cir) {
        if (immersiveFurniture$IsFurnitureBed()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setPosToBed(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$setPosToBed(BlockPos pos, CallbackInfo ci) {
        if (immersiveFurniture$IsFurnitureBed()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void immersiveFurniture$tick(CallbackInfo ci) {
        if (isSleeping()) {
            InteractionManager.Interaction interaction = InteractionManager.INSTANCE.getInteraction((LivingEntity) (Object) this);
            if (interaction != null && immersiveFurniture$IsFurnitureBed()) {
                float rotation = interaction.offset().rotation();
                setYRot(-rotation - 90f);
                setYBodyRot(-rotation - 90f);
                setYHeadRot(-rotation - 90f);

                setPos(
                        interaction.pos().getX() + interaction.offset().offset().x(),
                        interaction.pos().getY() + interaction.offset().offset().y(),
                        interaction.pos().getZ() + interaction.offset().offset().z()
                );
            }
        }
    }
}
