package net.conczin.immersive_furniture.network.c2s;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.s2c.FurnitureDataResponse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record FurnitureDataRequest(String hash) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<FurnitureDataRequest> TYPE = new CustomPacketPayload.Type<>(Common.locate("furniture_data_request"));
    public static final StreamCodec<FriendlyByteBuf, FurnitureDataRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FurnitureDataRequest::hash,
            FurnitureDataRequest::new
    );

    @Override
    public void handle(Player e) {
        if (e instanceof ServerPlayer sp) {
            FurnitureData data = FurnitureDataManager.getData(hash);

            // Cache damaged, it's better to return fallback data
            if (data == null) data = FurnitureData.EMPTY;

            Network.sendToPlayer(new FurnitureDataResponse(hash, data), sp);
        }
    }

    @Override
    public Type<FurnitureDataRequest> type() {
        return TYPE;
    }
}
