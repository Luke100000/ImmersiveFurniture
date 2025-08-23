package net.conczin.immersive_furniture.forge;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Common.MOD_ID, value = Dist.DEDICATED_SERVER)
public class CommonForgeBusEvents {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerFurnitureRegistry.syncWithPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        FurnitureDataManager.setWorldRoot(event.getServer().getWorldPath(LevelResource.ROOT));
    }
}
