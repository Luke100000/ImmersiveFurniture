package immersive_furniture.fabric;

import immersive_furniture.BlockEntityTypes;
import immersive_furniture.CommonClient;
import immersive_furniture.Entities;
import immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
import immersive_furniture.client.renderer.SittingEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> CommonClient.postLoad());
        ClientTickEvents.START_CLIENT_TICK.register(event -> CommonClient.tick());

        BlockEntityRenderers.register(BlockEntityTypes.FURNITURE.get(), FurnitureBlockEntityRenderer::new);
        EntityRendererRegistry.register(Entities.SITTING.get(), SittingEntityRenderer::new);
    }
}
