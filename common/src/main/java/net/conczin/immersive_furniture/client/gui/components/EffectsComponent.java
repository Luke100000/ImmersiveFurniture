package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.BoundedIntSliderButton;

public class EffectsComponent extends ScreenComponent {
    public EffectsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        if (screen.selectedElement == null) return;

        int y = topPos + 6;
        y = initLightGUI(screen, leftPos, y, width);
    }

    public int initLightGUI(ArtisansWorkstationEditorScreen screen, int x, int y, int width) {
        // Roundness
        BoundedIntSliderButton lightLevelSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20, "gui.immersive_furniture.roundness", 0, -100, 100);
        lightLevelSlider.setCallback(i -> {
            if (screen.selectedElement == null) return;
            screen.selectedElement.material.lightEffect.roundness = i;
        });
        screen.addRenderableWidget(lightLevelSlider);
        y += 22;

        // Brightness
        BoundedIntSliderButton brightnessSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20, "gui.immersive_furniture.brightness", 0, -100, 100);
        brightnessSlider.setCallback(i -> {
            if (screen.selectedElement == null) return;
            screen.selectedElement.material.lightEffect.brightness = i;
        });
        screen.addRenderableWidget(brightnessSlider);
        y += 22;

        // Contrast
        BoundedIntSliderButton contrastSlider = new BoundedIntSliderButton(x + 6, y, width - 12, 20, "gui.immersive_furniture.contrast", 0, -100, 100);
        contrastSlider.setCallback(i -> {
            if (screen.selectedElement == null) return;
            screen.selectedElement.material.lightEffect.contrast = i;
        });
        screen.addRenderableWidget(contrastSlider);
        y += 22;

        return y;
    }
}
