package immersive_furniture;

import immersive_furniture.client.ClientHandlerImpl;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.network.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

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

    private static final Map<BlockPos, Supplier<Boolean>> delayedRendering = new ConcurrentHashMap<>();

    public static void delayRendering(Supplier<Boolean> isReady, BlockPos pos) {
        delayedRendering.put(pos, isReady);
    }

    public static void onLevelLoad() {
        DynamicAtlas.BAKED.clear();
        DynamicAtlas.SCRATCH.clear();
        DynamicAtlas.ENTITY.clear();

        delayedRendering.clear();
    }

    public static void tick() {
        if (DynamicAtlas.SCRATCH.isFull() || DynamicAtlas.SCRATCH.getUsage() > 0.9f) {
            DynamicAtlas.SCRATCH.clear();
        }
        if (DynamicAtlas.ENTITY.isFull() || DynamicAtlas.ENTITY.getUsage() > 0.9f) {
            DynamicAtlas.ENTITY.clear();
        }

        // Re-render chunks where furniture failed to render
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.getDayTime() % 20 == 0 && !delayedRendering.isEmpty()) {
            Common.logger.info("Re-rendering {} delayed chunks", delayedRendering.size());
            Iterator<Map.Entry<BlockPos, Supplier<Boolean>>> it = delayedRendering.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Supplier<Boolean>> entry = it.next();
                if (entry.getValue().get()) {
                    BlockPos pos = entry.getKey();
                    client.levelRenderer.setSectionDirty(
                            SectionPos.blockToSectionCoord(pos.getX()),
                            SectionPos.blockToSectionCoord(pos.getY()),
                            SectionPos.blockToSectionCoord(pos.getZ())
                    );
                    it.remove();
                }
            }
        }
    }
}
