package immersive_furniture;

import immersive_furniture.client.ClientHandlerImpl;
import immersive_furniture.client.DelayedFurnitureRenderer;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.network.ClientNetworkManager;

public class CommonClient {
    public static void postLoad() {
        Common.networkManager = new ClientNetworkManager();
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
