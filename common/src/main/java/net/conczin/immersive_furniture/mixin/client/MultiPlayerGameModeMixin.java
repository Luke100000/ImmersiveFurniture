package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.item.MalletItem;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.c2s.MalletRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$onStartDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (immersiveFurniture$isMalletTarget(pos)) {
            Network.sendToServer(new MalletRequest(pos, face));
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$onContinueDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (immersiveFurniture$isMalletTarget(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean immersiveFurniture$isMalletTarget(BlockPos pos) {
        return minecraft.player != null && minecraft.level != null
               && minecraft.player.getMainHandItem().is(Items.MALLET)
               && MalletItem.isFurniture(minecraft.level.getBlockState(pos));
    }
}
