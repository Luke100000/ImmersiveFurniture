package net.conczin.immersive_furniture.forge;

import net.conczin.immersive_furniture.Client;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.entity.BlockEntityTypes;
import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
import net.conczin.immersive_furniture.client.renderer.SittingEntityRenderer;
import net.conczin.immersive_furniture.entity.Entities;
import net.conczin.immersive_furniture.forge.client.ForgeFurnitureBakedModelWrapper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public final class ClientForge {
    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityTypes.FURNITURE, FurnitureBlockEntityRenderer::new);
        event.registerEntityRenderer(Entities.SITTING, SittingEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller1, backgroundExecutor, gameExecutor)
                -> CompletableFuture.supplyAsync(() -> null, backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenRunAsync(Client::onLevelLoad, gameExecutor));
    }

    static {
        FurnitureBakedModelWrapper.model = new ForgeFurnitureBakedModelWrapper();
    }
}
