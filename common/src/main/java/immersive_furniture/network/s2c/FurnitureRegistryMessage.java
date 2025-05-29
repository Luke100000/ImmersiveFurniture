package immersive_furniture.network.s2c;

import immersive_furniture.cobalt.network.Message;
import immersive_furniture.data.ClientFurnitureRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class FurnitureRegistryMessage extends Message {
    Map<Integer, String> registry;

    public FurnitureRegistryMessage(Map<Integer, String> registry) {
        this.registry = registry;
    }

    public FurnitureRegistryMessage(FriendlyByteBuf b) {
        this.registry = b.readMap(FriendlyByteBuf::readVarInt, FriendlyByteBuf::readUtf);
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeMap(registry, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::writeUtf);
    }

    @Override
    public void receive(Player e) {
        ClientFurnitureRegistry.idToHash.putAll(registry);
        for (Map.Entry<Integer, String> entry : registry.entrySet()) {
            ClientFurnitureRegistry.hashToId.put(entry.getValue(), entry.getKey());
        }
    }
}
