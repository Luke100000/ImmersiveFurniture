package immersive_furniture.mixin;

import immersive_furniture.InteractionManager;
import immersive_furniture.block.BaseFurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Inject(method = "checkBedExists()Z", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$checkBedExists(CallbackInfoReturnable<Boolean> cir) {
        if (this.getSleepingPos().map(blockPos -> this.level().getBlockState(blockPos).getBlock() instanceof BaseFurnitureBlock).orElse(false)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setPosToBed(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$setPosToBed(BlockPos pos, CallbackInfo ci) {
        InteractionManager.Interaction interaction = InteractionManager.INSTANCE.getInteraction(this);
        if (interaction != null) {
            Vector3f offset = interaction.offset().offset();
            this.setPos(pos.getX() + offset.x(), pos.getY() + offset.y(), pos.getZ() + offset.z());
            this.setYRot(interaction.offset().rotation());
            ci.cancel();
        }
    }
}
