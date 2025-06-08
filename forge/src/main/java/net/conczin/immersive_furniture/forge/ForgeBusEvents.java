package net.conczin.immersive_furniture.forge;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.CommonClient;
import net.conczin.immersive_furniture.Items;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Common.MOD_ID)
public class ForgeBusEvents {
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
    public static void tick(TickEvent event) {
        if (event.type == TickEvent.Type.CLIENT && event.phase == TickEvent.Phase.START) {
            CommonClient.tick();
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerFurnitureRegistry.syncWithPlayer(player);
        }
    }
}
