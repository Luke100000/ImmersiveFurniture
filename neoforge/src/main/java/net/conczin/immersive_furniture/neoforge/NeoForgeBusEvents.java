package net.conczin.immersive_furniture.neoforge;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.CommonClient;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;


@EventBusSubscriber(modid = Common.MOD_ID)
public class NeoForgeBusEvents {
    public static boolean firstLoad = true;

    @SubscribeEvent
    public static void onClientStart(ClientTickEvent.Post event) {
        if (firstLoad) {
            CommonClient.postLoad();
            firstLoad = false;
        }
    }

    private static boolean warned = false;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        CommonClient.tick();


        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && !warned) {
            warned = true;
            ModList modList = ModList.get();
            if (!modList.isLoaded("ferritecore")) {
                mc.player.sendSystemMessage(Component.translatable("immersive_furniture.ferritecore_missing"));
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
