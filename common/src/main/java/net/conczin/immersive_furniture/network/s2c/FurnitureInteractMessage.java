package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.network.ClientHandler;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public record FurnitureInteractMessage(BlockPos pos, boolean active) implements ImmersivePayload {
    public FurnitureInteractMessage(FriendlyByteBuf b) {
        this(b.readBlockPos(), b.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeBlockPos(pos);
        b.writeBoolean(active);
    }

    @Override
    public void handle(Player e) {
        Common.clientHandler.handleFurnitureInteract(this);
    }
}
