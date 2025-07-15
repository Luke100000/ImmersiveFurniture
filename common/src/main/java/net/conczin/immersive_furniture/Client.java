package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.client.DelayedFurnitureRenderer;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.network.ClientHandlerImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class Client {
    public static void postLoad() {
        Common.clientHandler = new ClientHandlerImpl();

        // Load on the right thread
        DynamicAtlas.boostrap();
    }

    public static void onLevelLoad() {
        DynamicAtlas.BAKED.clear();
        DynamicAtlas.SCRATCH.clear();
        DynamicAtlas.ENTITY.clear();

        FurnitureDataManager.REQUESTED_DATA.clear();
        FurnitureDataManager.DATA.clear();
        DelayedFurnitureRenderer.INSTANCE.clear();
        InteractionManager.INSTANCE.clearInteraction();
    }

    public static void tick() {
        DelayedFurnitureRenderer.INSTANCE.tick();
    }

    public static void dependencyWarn(Player player, String key) {
        player.sendSystemMessage(Component.translatable("immersive_furniture." + key + "_missing"));
    }
}