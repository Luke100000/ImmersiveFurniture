package net.conczin.immersive_furniture.forge;

import net.conczin.immersive_furniture.Client;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT)
public class ClientForgeBusEvents {
    public static boolean firstLoad = true;

    @SubscribeEvent
    public static void onClientStart(TickEvent.ClientTickEvent event) {
        //forge decided to be funny and won't trigger the client load event
        if (firstLoad) {
            Client.postLoad();
            firstLoad = false;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent event) {
        if (event.type == TickEvent.Type.CLIENT && event.phase == TickEvent.Phase.START) {
            Client.tick();
        }
    }

    private static boolean warned = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null && !warned) {
                warned = true;
                ModList modList = ModList.get();

                if (!modList.isLoaded("ferritecore")) {
                    Client.dependencyWarn(mc.player, "ferritecore");
                }

                if (!modList.isLoaded("packetfixer")) {
                    Client.dependencyWarn(mc.player, "packetfixer");
                }
            }
        }
    }
}
