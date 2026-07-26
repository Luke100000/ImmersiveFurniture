package net.conczin.immersive_furniture.client;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class DelayedFurnitureRenderer {
    public static final DelayedFurnitureRenderer INSTANCE = new DelayedFurnitureRenderer();

    private static final int MAX_ENTITY_ATLAS_SIZE = 8192;
    private static final long ENTITY_ATLAS_RESET_COOLDOWN = TimeUnit.SECONDS.toNanos(10);

    private boolean quickCheck = false;
    private long lastEntityAtlasReset = System.nanoTime() - ENTITY_ATLAS_RESET_COOLDOWN;

    private final Map<Long, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<Long, Function<BlockPos, Status>> delayedRendering = new ConcurrentHashMap<>();

    public void delayRendering(BlockPos pos) {
        delayedRendering.put(pos.asLong(), this::getLoadedStatus);
        quickCheck = true;
    }

    public void tick() {
        // Clear the dynamic atlases if it is full or nearly full
        if (DynamicAtlas.SCRATCH.isFull() || DynamicAtlas.SCRATCH.getUsage() > 0.9f) {
            DynamicAtlas.SCRATCH.clear();
        }

        resetEntityAtlasIfNeeded();

        // Re-render chunks where furniture failed to render
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && (client.level.getDayTime() % 20 == 0 || quickCheck) && !delayedRendering.isEmpty()) {
            quickCheck = false;

            Iterator<Map.Entry<Long, Function<BlockPos, Status>>> it = delayedRendering.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Function<BlockPos, Status>> entry = it.next();
                BlockPos pos = BlockPos.of(entry.getKey());
                Status status = entry.getValue().apply(pos);

                int a = attempts.getOrDefault(entry.getKey(), 0);
                Common.delayedRendersChecks++;

                // Data available, re-render
                if (status.data() != null) {
                    Common.delayedRenders++;
                    client.levelRenderer.setSectionDirty(
                            SectionPos.blockToSectionCoord(pos.getX()),
                            SectionPos.blockToSectionCoord(pos.getY()),
                            SectionPos.blockToSectionCoord(pos.getZ())
                    );
                }

                if (status.done() || a >= 10) {
                    it.remove();
                } else {
                    attempts.put(entry.getKey(), a + 1);
                }
            }
        }
    }

    public void clear() {
        attempts.clear();
        delayedRendering.clear();
        lastEntityAtlasReset = System.nanoTime() - ENTITY_ATLAS_RESET_COOLDOWN;
    }

    private void resetEntityAtlasIfNeeded() {
        DynamicAtlas atlas = DynamicAtlas.ENTITY;
        if (!atlas.isFull() && atlas.getUsage() <= 0.9f) return;

        long now = System.nanoTime();
        if (atlas.isFull() && now - lastEntityAtlasReset < ENTITY_ATLAS_RESET_COOLDOWN) {
            if (atlas.getSize() < MAX_ENTITY_ATLAS_SIZE) {
                int size = Math.min(atlas.getSize() * 2, MAX_ENTITY_ATLAS_SIZE);
                DynamicAtlas.resizeEntity(size);
                lastEntityAtlasReset = now;
                Common.logger.info("Resized entity atlas to {}x{}", size, size);
            }
            return;
        }

        atlas.clear();
        lastEntityAtlasReset = now;
    }

    public record Status(boolean done, FurnitureData data) {
    }

    /*
    Loads the furniture data from either the block entity or the registry via block state identifier.
    Returns whether rechecking later is possible or the loading failed (e.g., block changed or is invalid).
     */
    public Status getLoadedStatus(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return new Status(true, null);
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BaseFurnitureBlock furnitureBlock) {
            FurnitureData data = furnitureBlock.getData(state, level, pos);
            return new Status(data != null, data);
        } else {
            return new Status(true, null);
        }
    }
}
