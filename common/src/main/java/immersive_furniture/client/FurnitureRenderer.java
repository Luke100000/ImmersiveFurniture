package immersive_furniture.client;

import immersive_furniture.Common;
import immersive_furniture.block.FurnitureBlock;
import immersive_furniture.block.FurnitureBlockEntity;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.data.ClientFurnitureRegistry;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.data.FurnitureDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class FurnitureRenderer {
    private static boolean quickCheck = false;
    private static final Map<Long, Function<BlockPos, Status>> delayedRendering = new ConcurrentHashMap<>();

    public static void delayRendering(BlockGetter level, BlockPos pos) {
        delayedRendering.put(pos.asLong(), FurnitureRenderer::getLoadedStatus);
        quickCheck = true;
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
        if (client.level != null && (client.level.getDayTime() % 20 == 0 || quickCheck) && !delayedRendering.isEmpty()) {
            Common.logger.info("Re-checking {} delayed chunks", delayedRendering.size());
            quickCheck = false;

            Iterator<Map.Entry<Long, Function<BlockPos, Status>>> it = delayedRendering.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Function<BlockPos, Status>> entry = it.next();
                BlockPos pos = BlockPos.of(entry.getKey());
                Status status = entry.getValue().apply(pos);

                // Data available, re-render
                if (status.data() != null) {
                    Common.logger.info("Re-rendering chunk {}", entry.getKey());
                    client.levelRenderer.setSectionDirty(
                            SectionPos.blockToSectionCoord(pos.getX()),
                            SectionPos.blockToSectionCoord(pos.getY()),
                            SectionPos.blockToSectionCoord(pos.getZ())
                    );
                }

                if (status.done()) {
                    it.remove();
                }
            }
        }
    }

    public static void clear() {
        delayedRendering.clear();
    }

    public record Status(boolean done, FurnitureData data) {
    }

    /*
    Loads the furniture data from either the block entity or the registry via block state identifier.
    Returns whether rechecking later is possible or the loading failed (e.g., block changed or is invalid).
     */
    public static Status getLoadedStatus(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return new Status(true, null);
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FurnitureBlock)) {
            return new Status(true, null);
        }

        // If this is entity-based furniture, fetch the furniture from its data
        int identifier = state.getValue(FurnitureBlock.IDENTIFIER);
        if (identifier == 0) {
            if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity blockEntity) {
                FurnitureData data = blockEntity.getData();
                return new Status(data != null, data);
            } else {
                // Expected an entity
                return new Status(true, null);
            }
        } else {
            String hash = ClientFurnitureRegistry.resolve(identifier);
            if (hash == null) {
                // Hashes are synced very early on, them missing is considered a critical error
                return new Status(true, null);
            }
            FurnitureData data = FurnitureDataManager.getData(hash);
            return new Status(data != null, data);
        }
    }
}
