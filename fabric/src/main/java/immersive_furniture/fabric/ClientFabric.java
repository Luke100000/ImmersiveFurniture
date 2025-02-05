package immersive_furniture.fabric;

import immersive_furniture.BlockEntityTypes;
import immersive_furniture.CommonClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import immersive_furniture.block.FurnitureBlockEntityRenderer;

public final class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> CommonClient.postLoad());

        BlockEntityRenderers.register(BlockEntityTypes.FURNITURE.get(), FurnitureBlockEntityRenderer::new);
    }
}
