package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.Map;
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

    public void clearInteraction() {
        interactions.clear();
    }

    public Interaction getInteraction(Entity entity) {
        return interactions.get(entity.getUUID());
    }
}
