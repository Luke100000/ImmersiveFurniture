package immersive_furniture.client.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class BoundedDoubleSlider extends AbstractSliderButton {
    double minValue;
    double maxValue;

    String template;
    Consumer<Double> callback;

    public BoundedDoubleSlider(int x, int y, int width, int height, String template, double value, double minValue, double maxValue) {
        super(x, y, width, height, Component.literal(""), (value - minValue) / (maxValue - minValue));

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.template = template;

        updateMessage();
    }

    @Override
    protected void updateMessage() {
        String val = String.format("%.2f", getValue());
        this.setMessage(Component.translatable(this.template + ".short", val));
        this.setTooltip(Tooltip.create(Component.translatable(this.template, val)));
    }

    @Override
    protected void applyValue() {
        if (callback != null) {
            callback.accept(getValue());
        }
    }

    public double getValue() {
        return (this.value * (maxValue - minValue)) + minValue;
    }

    public void setCallback(Consumer<Double> callback) {
        this.callback = callback;
    }
}