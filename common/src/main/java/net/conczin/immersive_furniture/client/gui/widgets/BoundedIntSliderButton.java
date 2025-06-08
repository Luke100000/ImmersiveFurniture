package net.conczin.immersive_furniture.client.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class BoundedIntSliderButton extends AbstractSliderButton {
    int integerValue;
    int minValue;
    int maxValue;

    String template;
    Consumer<Integer> callback;

    public BoundedIntSliderButton(int x, int y, int width, int height, String template, int value, int minValue, int maxValue) {
        super(x, y, width, height, Component.literal(""), (value - minValue) / (float) (maxValue - minValue));

        this.integerValue = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.template = template;

        updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.translatable(this.template, this.integerValue));
    }

    @Override
    protected void applyValue() {
        integerValue = Math.toIntExact(Math.round(this.value * (maxValue - minValue)) + minValue);
        if (callback != null) {
            callback.accept(integerValue);
        }
    }

    public int getIntegerValue() {
        return integerValue;
    }

    public void setCallback(Consumer<Integer> callback) {
        this.callback = callback;
    }
}
