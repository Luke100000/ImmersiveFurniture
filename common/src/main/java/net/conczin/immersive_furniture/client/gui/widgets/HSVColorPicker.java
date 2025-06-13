package net.conczin.immersive_furniture.client.gui.widgets;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.function.Consumer;

public class HSVColorPicker extends AbstractWidget {
    private final ColorSliderWidget hueSlider;
    private final ColorSliderWidget saturationSlider;
    private final ColorSliderWidget valueSlider;

    private int color;
    private float hue;
    private float saturation;
    private float value;

    private final Consumer<Integer> callback;

    public HSVColorPicker(int x, int y, int width, int height, int color, Consumer<Integer> callback) {
        super(x, y, width, height, Component.literal(""));

        // Convert RGB to HSV
        this.color = color;
        float[] hsv = Utils.rgbToHsv(color);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        this.callback = callback;

        // Calculate slider heights
        int sliderHeight = (height - 6) / 3;

        // Create the sliders
        hueSlider = new TexturedColorSliderWidget(
                x, y + 1,
                width, sliderHeight,
                hue / 360.0f,
                Common.locate("textures/gui/hue.png"),
                v -> {
                    hue = v.floatValue() * 360.0f;
                    update();
                }
        );

        saturationSlider = new ColorSliderWidget(
                x, y + 3 + sliderHeight,
                width, sliderHeight,
                saturation,
                v -> {
                    saturation = v.floatValue();
                    update();
                }
        );

        valueSlider = new ColorSliderWidget(
                x, y + 5 + 2 * sliderHeight,
                width, sliderHeight,
                value,
                v -> {
                    value = v.floatValue();
                    update();
                }
        );

        update();
    }

    private void update() {
        color = Utils.hsvToRgb(hue, saturation, value);
        saturationSlider.setColor(
                0xFF000000 + Utils.hsvToRgb(hue, 0.0f, 1.0f),
                0xFF000000 + Utils.hsvToRgb(hue, 1.0f, 1.0f)
        );
        valueSlider.setColor(
                0xFF000000,
                0xFF000000 + Utils.hsvToRgb(hue, 1.0f, 1.0f)
        );

        callback.accept(FastColor.ARGB32.color(
                255,
                FastColor.ABGR32.red(color),
                FastColor.ABGR32.green(color),
                FastColor.ABGR32.blue(color)
        ));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public List<AbstractWidget> getWidgets() {
        return List.of(this, hueSlider, saturationSlider, valueSlider);
    }
}
