package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.Client;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$handleComponentClicked(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null) return;

        ClickEvent click = style.getClickEvent();
        if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND && Client.ATLAS_REFRESH_COMMAND.equals(click.getValue())) {
            Client.refreshAtlas();
            cir.setReturnValue(true);
        } else if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND && Client.ATLAS_INCREASE_COMMAND.equals(click.getValue())) {
            Client.increaseAtlasSize();
            cir.setReturnValue(true);
        }
    }
}
