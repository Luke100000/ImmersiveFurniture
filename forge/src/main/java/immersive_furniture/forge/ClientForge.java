package immersive_furniture.forge;

import immersive_furniture.BlockEntityTypes;
import immersive_furniture.Common;
import immersive_furniture.CommonClient;
import immersive_furniture.Entities;
import immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
import immersive_furniture.client.renderer.SittingEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public final class ClientForge {
    @SubscribeEvent
    public static void data(FMLConstructModEvent event) {
        ReloadableResourceManager resourceManager = (ReloadableResourceManager) Minecraft.getInstance().getResourceManager();
        ForgeBusEvents.RESOURCE_REGISTRY.getLoaders().forEach(resourceManager::registerReloadListener);
    }

    @SubscribeEvent
    public static void tick(TickEvent event) {
        if (event.type == TickEvent.Type.CLIENT && event.phase == TickEvent.Phase.START) {
            CommonClient.tick();
        }
    }

    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityTypes.FURNITURE.get(), FurnitureBlockEntityRenderer::new);
        event.registerEntityRenderer(Entities.SITTING.get(), SittingEntityRenderer::new);
    }
}
