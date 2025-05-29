package immersive_furniture.forge;

import immersive_furniture.Common;
import immersive_furniture.CommonClient;
import immersive_furniture.data.ServerFurnitureRegistry;
import immersive_furniture.forge.cobalt.registration.RegistrationImpl.DataLoaderRegister;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Common.MOD_ID)
public class ForgeBusEvents {
    // Require access to the DataLoaderRegister here as forge uses events, could put this in RegistrationImpl, but it would just be messy
    public static DataLoaderRegister DATA_REGISTRY;
    public static DataLoaderRegister RESOURCE_REGISTRY;

    public static boolean firstLoad = true;

    @SubscribeEvent
    public static void onClientStart(TickEvent.ClientTickEvent event) {
        //forge decided to be funny and won't trigger the client load event
        if (firstLoad) {
            CommonClient.postLoad();
            firstLoad = false;
        }
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(AddReloadListenerEvent event) {
        if (DATA_REGISTRY != null) {
            for (PreparableReloadListener loader : DATA_REGISTRY.getLoaders()) {
                event.addListener(loader);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerFurnitureRegistry.syncWithPlayer(player);
        }
    }
}
