package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.widgets.StateImageButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

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

    protected ImageButton addButton(int x, int y, int size, int u, int v, String tooltip, Runnable clicked) {
        ImageButton button = screen.addRenderableWidget(
                new ImageButton(x, y, size, size, u, v, size, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                        b -> clicked.run(),
                        tooltip == null ? Component.literal("") : Component.translatable(tooltip))
        );

        if (tooltip != null) {
            button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }

        return button;
    }

    protected StateImageButton addToggleButton(int x, int y, int size, int u, int v, String tooltip, Runnable clicked) {
        StateImageButton button = screen.addRenderableWidget(
                new StateImageButton(x, y, size, size, u, v, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                        b -> clicked.run(),
                        tooltip == null ? Component.literal("") : Component.translatable(tooltip))
        );

        if (tooltip != null) {
            button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }

        return button;
    }
}
