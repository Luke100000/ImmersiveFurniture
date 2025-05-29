package immersive_furniture;

import immersive_furniture.block.FurnitureBlock;
import immersive_furniture.client.ClientHandlerImpl;
import immersive_furniture.client.FurnitureRenderer;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.network.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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

        FurnitureRenderer.clear();
    }

    public static void tick() {
        FurnitureRenderer.tick();
    }
}
