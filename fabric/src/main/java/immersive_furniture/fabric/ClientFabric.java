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
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> CommonClient.postLoad());
        ClientTickEvents.START_CLIENT_TICK.register(event -> CommonClient.tick());

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return CompletableFuture.supplyAsync(() -> null, backgroundExecutor)
                        .thenCompose(preparationBarrier::wait)
                        .thenRunAsync(CommonClient::onLevelLoad, gameExecutor);
            }

            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation("immersive_furniture", "on_level_load");
            }
        });

        BlockEntityRenderers.register(BlockEntityTypes.FURNITURE.get(), FurnitureBlockEntityRenderer::new);
        EntityRendererRegistry.register(Entities.SITTING.get(), SittingEntityRenderer::new);
    }
}
