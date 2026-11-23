package net.conczin.immersive_furniture.network.c2s;

import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.item.MalletItem;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record MalletRequest(BlockPos pos, Direction face) implements ImmersivePayload {
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    public MalletRequest(FriendlyByteBuf b) {
        this(b.readBlockPos(), b.readEnum(Direction.class));
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeBlockPos(pos);
        b.writeEnum(face);
    }

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
}
