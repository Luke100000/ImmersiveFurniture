package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class FurnitureInteractMessage implements ImmersivePayload {
    BlockPos pos;

    public FurnitureInteractMessage(BlockPos pos) {
        this.pos = pos;
    }

    public FurnitureInteractMessage(FriendlyByteBuf b) {
        this.pos = b.readBlockPos();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeBlockPos(pos);
    }

    @Override
    public void handle(Player e) {
        BlockState blockState = e.level().getBlockState(pos);
        if (blockState.getBlock() instanceof BaseFurnitureBlock furnitureBlock) {
            furnitureBlock.onInteract(e.level(), blockState, pos, e);
        }
    }
}
