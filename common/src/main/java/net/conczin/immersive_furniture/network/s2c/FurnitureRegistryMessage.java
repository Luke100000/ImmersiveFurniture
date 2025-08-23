package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.FurnitureRegistry;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public record FurnitureRegistryMessage(Map<Integer, String> registry) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<FurnitureRegistryMessage> TYPE = new CustomPacketPayload.Type<>(Common.locate("furniture_registry_message"));
    public static final StreamCodec<FriendlyByteBuf, FurnitureRegistryMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, ByteBufCodecs.STRING_UTF8), FurnitureRegistryMessage::registry,
            FurnitureRegistryMessage::new
    );

    @Override
    public void handle(Player e) {
        FurnitureRegistry.INSTANCE.identifierToHash.putAll(registry);
        for (Map.Entry<Integer, String> entry : registry.entrySet()) {
            FurnitureRegistry.INSTANCE.hashToIdentifier.put(entry.getValue(), entry.getKey());

            // Download all data now, since it's harder to differentiate between server and client later on
            FurnitureDataManager.getCachedData(entry.getValue());
        }
    }

    @Override
    public Type<FurnitureRegistryMessage> type() {
        return TYPE;
    }
}
