package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.FurnitureRegistry;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class FurnitureRegistryMessage implements ImmersivePayload {
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
    public void handle(Player e) {
        FurnitureRegistry.INSTANCE.identifierToHash.putAll(registry);
        for (Map.Entry<Integer, String> entry : registry.entrySet()) {
            FurnitureRegistry.INSTANCE.hashToIdentifier.put(entry.getValue(), entry.getKey());

            // Download all data now, since it's harder to differentiate between server and client later on
            FurnitureDataManager.getData(new ResourceLocation("hash", entry.getValue()), true);
        }
    }
}
