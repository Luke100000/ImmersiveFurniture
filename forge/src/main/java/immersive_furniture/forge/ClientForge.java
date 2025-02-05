package immersive_furniture.forge;

import immersive_furniture.Common;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public final class ClientForge {
    @SubscribeEvent
    public static void data(FMLConstructModEvent event) {
        ReloadableResourceManager resourceManager = (ReloadableResourceManager) Minecraft.getInstance().getResourceManager();
        ForgeBusEvents.RESOURCE_REGISTRY.getLoaders().forEach(resourceManager::registerReloadListener);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {

    }
}
