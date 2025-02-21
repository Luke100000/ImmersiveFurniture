package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.minecraft.client.Minecraft;

public class ScreenComponent {
    final ArtisansWorkstationEditorScreen screen;
    final Minecraft minecraft;

    int leftPos = 0;
    int topPos = 0;
    int width = 0;
    int height = 0;

    public ScreenComponent(ArtisansWorkstationEditorScreen screen) {
        this.screen = screen;
        this.minecraft = Minecraft.getInstance();
    }

    public void init(int leftPos, int topPos, int width, int height) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        this.height = height;
    }
}
