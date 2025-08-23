package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/gui/screens/ReceivingLevelScreen$Reason;)V", at = @At("HEAD"))
    private void immersiveFurniture$onSetLevel(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        Client.onLevelLoad();
    }
}
