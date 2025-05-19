package immersive_furniture.client;

import immersive_furniture.ClientHandler;
import immersive_furniture.client.gui.ArtisansWorkstationLibraryScreen;
import net.minecraft.client.Minecraft;

public class ClientHandlerImpl implements ClientHandler {
    @Override
    public void openScreen() {
        Minecraft.getInstance().setScreen(new ArtisansWorkstationLibraryScreen());
    }
}
