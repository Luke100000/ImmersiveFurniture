package net.conczin.immersive_furniture.client.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class TexturedColorSliderWidget extends ColorSliderWidget {
    private final ResourceLocation texture;

    public TexturedColorSliderWidget(int x, int y, int width, int height, double value, ResourceLocation texture, Consumer<Double> consumer) {
        super(x, y, width, height, value, consumer);

        this.texture = texture;
    }

    @Override
    protected void drawInnerWidget(GuiGraphics graphics) {
        graphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);
    }
}
