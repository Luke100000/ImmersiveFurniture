package net.conczin.immersive_furniture.neoforge;

import net.conczin.immersive_furniture.Client;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.entity.BlockEntityTypes;
import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
import net.conczin.immersive_furniture.client.renderer.SittingEntityRenderer;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.entity.Entities;
import net.conczin.immersive_furniture.neoforge.client.NeoForgeFurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.network.Network;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.concurrent.CompletableFuture;

@Mod(value = Common.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT)
public final class ClientNeoForge {
    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityTypes.FURNITURE, FurnitureBlockEntityRenderer::new);
        event.registerEntityRenderer(Entities.SITTING, SittingEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerNetwork(final RegisterPayloadHandlersEvent event) {
        Network.registerClientSender(PacketDistributor::sendToServer);
    }

    @SubscribeEvent
    public static void onClientConnected(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!Minecraft.getInstance().isLocalServer()) {
            FurnitureDataManager.setWorldRoot();
        }
    }

    @SubscribeEvent
    public static void onReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller1, backgroundExecutor, gameExecutor)
                -> CompletableFuture.supplyAsync(() -> null, backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenRunAsync(Client::onLevelLoad, gameExecutor));
    }

    static {
        FurnitureBakedModelWrapper.model = new NeoForgeFurnitureBakedModelWrapper();
    }
}
