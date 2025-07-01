package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.BoundedIntSliderButton;
import net.conczin.immersive_furniture.client.gui.widgets.HSVColorPicker;
import net.conczin.immersive_furniture.data.FurnitureData;

public class EffectsComponent extends ScreenComponent {
    public EffectsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        FurnitureData.Element firstElement = screen.getFirstElement().orElse(null);
        if (firstElement == null) return;

        int y = topPos + 6;
        y = initLightGUI(screen, leftPos, y, width);

        if (firstElement.type == FurnitureData.ElementType.SPRITE) {
            HSVColorPicker colorSelector = new HSVColorPicker(leftPos + 6, y, width - 12, 60, firstElement.color,
                    c -> screen.selectedElements.forEach(e -> e.color = c));
            colorSelector.getWidgets().forEach(screen::addRenderableWidget);
            y += 66;
        }

        // Emission
        BoundedIntSliderButton emissionSlider = new BoundedIntSliderButton(leftPos + 6, y, width - 12, 20,
                "gui.immersive_furniture.emission",
                firstElement.emission, 0, 15);
        emissionSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.emission = i));
        screen.addRenderableWidget(emissionSlider);
    }

    public int initLightGUI(ArtisansWorkstationEditorScreen screen, int x, int y, int width) {
        FurnitureData.Element firstElement = screen.getFirstElement().orElse(null);
        if (firstElement == null) return y;
        if (firstElement.type != FurnitureData.ElementType.ELEMENT) return y;

        // Roundness
        BoundedIntSliderButton lightLevelSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20,
                "gui.immersive_furniture.roundness",
                (int) firstElement.material.lightEffect.roundness, -100, 100);
        lightLevelSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.material.lightEffect.roundness = i));
        screen.addRenderableWidget(lightLevelSlider);
        y += 22;

        // Brightness
        BoundedIntSliderButton brightnessSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20,
                "gui.immersive_furniture.brightness",
                (int) firstElement.material.lightEffect.brightness, -100, 100);
        brightnessSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.material.lightEffect.brightness = i));
        screen.addRenderableWidget(brightnessSlider);
        y += 22;

        // Contrast
        BoundedIntSliderButton contrastSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20,
                "gui.immersive_furniture.contrast",
                (int) firstElement.material.lightEffect.contrast, -100, 100);
        contrastSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.material.lightEffect.contrast = i));
        screen.addRenderableWidget(contrastSlider);
        y += 22;

        // Hue
        BoundedIntSliderButton hueSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20,
                "gui.immersive_furniture.hue",
                (int) firstElement.material.lightEffect.hue, -100, 100);
        hueSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.material.lightEffect.hue = i));
        screen.addRenderableWidget(hueSlider);
        y += 22;

        // Saturation
        BoundedIntSliderButton saturationSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20,
                "gui.immersive_furniture.saturation",
                (int) firstElement.material.lightEffect.saturation, -100, 100);
        saturationSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.material.lightEffect.saturation = i));
        screen.addRenderableWidget(saturationSlider);
        y += 22;

        // Value
        BoundedIntSliderButton valueSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20,
                "gui.immersive_furniture.value",
                (int) firstElement.material.lightEffect.value, -100, 100);
        valueSlider.setCallback(i -> screen.selectedElements.forEach(e -> e.material.lightEffect.value = i));
        screen.addRenderableWidget(valueSlider);
        y += 22;

        return y;
    }
}
