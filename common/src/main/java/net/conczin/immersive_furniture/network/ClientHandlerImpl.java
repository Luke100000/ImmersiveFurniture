package net.conczin.immersive_furniture.network;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationLibraryScreen;
import net.conczin.immersive_furniture.network.s2c.FurnitureInteractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void openScreen() {
        Minecraft.getInstance().setScreen(new ArtisansWorkstationLibraryScreen());
    }

    @Override
    public void handleFurnitureInteract(FurnitureInteractMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        BlockState blockState = level.getBlockState(message.pos());
        if (blockState.getBlock() instanceof BaseFurnitureBlock furnitureBlock) {
            furnitureBlock.onInteract(level, blockState, message.pos(), minecraft.player);
        }
    }
}
