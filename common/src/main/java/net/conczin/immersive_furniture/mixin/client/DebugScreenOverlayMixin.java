package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Inject(at = @At("RETURN"), method = "getGameInformation")
    protected void immersiveFurniture$getLeftText(CallbackInfoReturnable<List<String>> info) {
        info.getReturnValue().add("[IF] Atlas utilization: B: %s%% (%s), E: %s%% (%s), S: %s%% (%s)".formatted(
                (int) (DynamicAtlas.BAKED.getUsage() * 100),
                DynamicAtlas.BAKED.knownFurniture.size(),
                (int) (DynamicAtlas.ENTITY.getUsage() * 100),
                DynamicAtlas.ENTITY.knownFurniture.size(),
                (int) (DynamicAtlas.SCRATCH.getUsage() * 100),
                DynamicAtlas.SCRATCH.knownFurniture.size()
        ));
        info.getReturnValue().add("[IF] Delayed renders: %s (%s checks)".formatted(
                Common.delayedRenders,
                Common.delayedRendersChecks
        ));
        info.getReturnValue().add("[IF] Entity renders: %s (%s total)".formatted(
                Common.entityRendersLast,
                Common.entityRendersTotalLast
        ));
    }
}
