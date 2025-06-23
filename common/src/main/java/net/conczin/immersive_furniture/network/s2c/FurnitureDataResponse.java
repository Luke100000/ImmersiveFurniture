package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record FurnitureDataResponse(String hash, FurnitureData data) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<FurnitureDataResponse> TYPE = new CustomPacketPayload.Type<>(Common.locate("furniture_data_response"));
    public static final StreamCodec<FriendlyByteBuf, FurnitureDataResponse> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FurnitureDataResponse::hash,
            FurnitureData.STREAM_CODEC, FurnitureDataResponse::data,
            FurnitureDataResponse::new
    );

    @Override
    public void handle(Player e) {
        FurnitureDataManager.save(data, ResourceLocation.fromNamespaceAndPath("hash", hash));
    }

    @Override
    public Type<FurnitureDataResponse> type() {
        return TYPE;
    }
}
