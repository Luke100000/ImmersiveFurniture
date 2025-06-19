package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.client.DelayedFurnitureRenderer;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.network.ClientHandlerImpl;

public class CommonClient {
    public static void postLoad() {
        Common.clientHandler = new ClientHandlerImpl();

        // Load on the right thread
        DynamicAtlas.boostrap();
    }

    public static void onLevelLoad() {
        DynamicAtlas.BAKED.clear();
        DynamicAtlas.SCRATCH.clear();
        DynamicAtlas.ENTITY.clear();

        DelayedFurnitureRenderer.INSTANCE.clear();
        InteractionManager.INSTANCE.clearInteraction();
    }

    public static void tick() {
        DelayedFurnitureRenderer.INSTANCE.tick();
    }
}