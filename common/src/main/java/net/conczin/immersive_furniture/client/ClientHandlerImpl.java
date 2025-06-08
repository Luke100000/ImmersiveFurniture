package net.conczin.immersive_furniture.client;

import net.conczin.immersive_furniture.ClientHandler;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationLibraryScreen;
import net.minecraft.client.Minecraft;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void openScreen() {
        Minecraft.getInstance().setScreen(new ArtisansWorkstationLibraryScreen());
    }
}
