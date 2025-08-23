package net.conczin.immersive_furniture.fabric;

import net.conczin.immersive_furniture.Client;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.entity.BlockEntityTypes;
import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
import net.conczin.immersive_furniture.client.renderer.SittingEntityRenderer;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.entity.Entities;
import net.conczin.immersive_furniture.fabric.client.FabricFurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.network.Network;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ClientFabric implements ClientModInitializer {
    private static boolean warned = false;

    @Override
    public void onInitializeClient() {
        Network.registerClientSender(ClientPlayNetworking::send);

        ClientLifecycleEvents.CLIENT_STARTED.register(event -> Client.postLoad());
        ClientTickEvents.START_CLIENT_TICK.register(event -> Client.tick());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && client.player != null && !warned) {
                warned = true;

                // Immersive Furniture uses the FabricBakedModel and thus required Indium to be installed when Sodium is present.
                if (FabricLoader.getInstance().isModLoaded("sodium") && !FabricLoader.getInstance().isModLoaded("indium")) {
                    Client.dependencyWarn(client.player, "indium");
                }

                if (!FabricLoader.getInstance().isModLoaded("ferritecore")) {
                    Client.dependencyWarn(client.player, "ferritecore");
                }

                if (!FabricLoader.getInstance().isModLoaded("packetfixer")) {
                    Client.dependencyWarn(client.player, "packetfixer");
                }
            }
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return CompletableFuture.supplyAsync(() -> null, backgroundExecutor)
                        .thenCompose(preparationBarrier::wait)
                        .thenRunAsync(Client::onLevelLoad, gameExecutor);
            }

            @Override
            public ResourceLocation getFabricId() {
                return Common.locate("on_level_load");
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!client.isLocalServer()) {
                FurnitureDataManager.setWorldRoot();
            }
        });

        BlockEntityRenderers.register(BlockEntityTypes.FURNITURE, FurnitureBlockEntityRenderer::new);
        EntityRendererRegistry.register(Entities.SITTING, SittingEntityRenderer::new);
    }

    static {
        FurnitureBakedModelWrapper.model = new FabricFurnitureBakedModelWrapper();
    }
}
