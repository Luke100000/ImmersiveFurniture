package immersive_furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionManager {
    public static final InteractionManager INSTANCE = new InteractionManager();

    public enum InteractionPose {
        SITTING,
        LYING
    }

    public record Interaction(BlockPos pos, Vector3f offset, InteractionPose pose) {

    }

    private final Map<UUID, Interaction> interactions = new ConcurrentHashMap<>();
    private final Map<Long, Interaction> blockInteractions = new ConcurrentHashMap<>();

    public void addInteraction(Entity entity, BlockPos pos, Vector3f offset, InteractionPose pose) {
        if (interactions.containsKey(entity.getUUID())) {
            clearInteraction(entity);
        }
        interactions.put(entity.getUUID(), new Interaction(pos, offset, pose));
        blockInteractions.put(pos.asLong(), new Interaction(pos, offset, pose));
    }

    public void clearInteraction(Entity entity) {
        Interaction interaction = getInteraction(entity);
        if (interaction != null) {
            blockInteractions.remove(interaction.pos().asLong());
            interactions.remove(entity.getUUID());
        }
    }

    public Interaction getInteraction(Entity entity) {
        return interactions.get(entity.getUUID());
    }

    public Interaction getInteraction(BlockPos pos) {
        return blockInteractions.get(pos.asLong());
    }
}
