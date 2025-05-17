package immersive_furniture.client.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class BoundedIntSliderButton extends AbstractSliderButton {
    int integerValue;
    String template;

    public BoundedIntSliderButton(int x, int y, int width, int height, String template, int value) {
        super(x, y, width, height, Component.literal(""), value);

        this.integerValue = value;
        this.template = template;

        updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.translatable(this.template, this.integerValue));
    }

    @Override
    protected void applyValue() {
        integerValue = (int) this.value;
    }

    public int getIntegerValue() {
        return integerValue;
    }
}
