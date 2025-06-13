package net.conczin.immersive_furniture.client.gui.widgets;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class ColorSliderWidget extends AbstractWidget {
    private final Consumer<Double> consumer;

    double value;

    int color0;
    int color1;

    public ColorSliderWidget(int x, int y, int width, int height, double value, Consumer<Double> consumer) {
        super(x, y, width, height, Component.literal(""));

        this.consumer = consumer;
        this.value = value;
    }

    protected void drawInnerWidget(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY() + getHeight(), 0);
        graphics.pose().rotateAround(Axis.ZP.rotationDegrees(-90.0f), 0, 0, 0.0f);
        graphics.fillGradient(
                0, 0,
                getHeight(), getWidth(),
                color0, color1
        );
        graphics.pose().popPose();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawInnerWidget(graphics);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF000000);

        // Selector
        int x = (int) (getX() + value * getWidth());
        graphics.fill(x - 1, getY(), x + 1, getY() + getHeight(), 0xFF000000);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        update(mouseX);
        super.onDrag(mouseX, mouseY, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInArea(mouseX, mouseY)) {
            update(mouseX);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInArea(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height;
    }

    void update(double mouseX) {
        value = Mth.clamp((mouseX - getX()) / width, 0.0, 1.0);
        consumer.accept(value);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {

    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setColor(int color0, int color1) {
        this.color0 = color0;
        this.color1 = color1;
    }
}
