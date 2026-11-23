package net.conczin.immersive_furniture.network.c2s;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.item.MalletItem;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record MalletRequest(BlockPos pos, Direction face) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<MalletRequest> TYPE = new CustomPacketPayload.Type<>(Common.locate("mallet_request"));
    public static final StreamCodec<FriendlyByteBuf, MalletRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MalletRequest::pos,
            Direction.STREAM_CODEC, MalletRequest::face,
            MalletRequest::new
    );

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    @Override
    public void handle(Player e) {
        if (!(e instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().is(Items.MALLET) || !player.mayBuild()) return;
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;
        if (!player.level().mayInteract(player, pos) || !MalletItem.isFurniture(player.level().getBlockState(pos))) {
            return;
        }

        MalletItem.nudge(player.level(), pos, face.getOpposite());
    }

    @Override
    public Type<MalletRequest> type() {
        return TYPE;
    }
}
