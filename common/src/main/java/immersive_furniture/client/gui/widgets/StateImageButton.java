package immersive_furniture.client.gui.widgets;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StateImageButton extends ImageButton {
    private boolean enabled = true;

    public StateImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, ResourceLocation resourceLocation, int textureWidth, int textureHeight, OnPress onPress, Component message) {
        super(x, y, width, height, xTexStart, yTexStart, height, resourceLocation, textureWidth, textureHeight, onPress, message);
    }


    @Override
    public boolean isHoveredOrFocused() {
        return super.isHoveredOrFocused() ^ isEnabled();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
