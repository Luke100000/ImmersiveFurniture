package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.block.FurnitureProxyBlock;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionManager {
    public static final InteractionManager INSTANCE = new InteractionManager();

    public record Interaction(BlockPos pos, FurnitureData.PoseOffset offset) {

    }

    private final Map<UUID, Interaction> interactions = new ConcurrentHashMap<>();

    public void addInteraction(Entity entity, BlockPos pos, FurnitureData.PoseOffset offset) {
        interactions.put(entity.getUUID(), new Interaction(pos, offset));
    }

    public Interaction getInteraction(LivingEntity entity) {
        Interaction interaction = interactions.get(entity.getUUID());
        if (interaction != null) {
            // If the position has changed, clear the interaction
            Optional<BlockPos> sleepingPos = entity.getSleepingPos();
            if (sleepingPos.isPresent() && !sleepingPos.get().equals(interaction.pos)) {
                clearInteraction(entity);
                return null;
            }

            Level level = entity.level();
            if (isFurnitureBed(level, interaction.pos())) {
                return interaction;
            } else {
                clearInteraction(entity);
            }
        }
        return null;
    }

    public static boolean isFurnitureBed(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block instanceof BaseFurnitureBlock || block instanceof FurnitureProxyBlock;
    }

    public void clearInteraction(Entity entity) {
        interactions.remove(entity.getUUID());
    }

    public void clearInteraction() {
        interactions.clear();
    }
}
