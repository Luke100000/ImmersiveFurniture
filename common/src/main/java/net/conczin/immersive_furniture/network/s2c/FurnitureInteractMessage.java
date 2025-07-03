package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public record FurnitureInteractMessage(BlockPos pos) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<FurnitureInteractMessage> TYPE = new CustomPacketPayload.Type<>(Common.locate("furniture_interact_message"));
    public static final StreamCodec<FriendlyByteBuf, FurnitureInteractMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FurnitureInteractMessage::pos,
            FurnitureInteractMessage::new
    );

    @Override
    public void handle(Player e) {
        BlockState blockState = e.level().getBlockState(pos);
        if (blockState.getBlock() instanceof BaseFurnitureBlock furnitureBlock) {
            furnitureBlock.onInteract(e.level(), blockState, pos, e);
        }
    }

    @Override
    public Type<FurnitureInteractMessage> type() {
        return TYPE;
    }
}
