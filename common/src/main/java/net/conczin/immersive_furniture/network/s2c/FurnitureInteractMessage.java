package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record FurnitureInteractMessage(BlockPos pos) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<FurnitureInteractMessage> TYPE = new CustomPacketPayload.Type<>(Common.locate("furniture_interact_message"));
    public static final StreamCodec<FriendlyByteBuf, FurnitureInteractMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FurnitureInteractMessage::pos,
            FurnitureInteractMessage::new
    );

    @Override
    public void handle(Player e) {
        Common.clientHandler.handleFurnitureInteract(this);
    }

    @Override
    public Type<FurnitureInteractMessage> type() {
        return TYPE;
    }
}
