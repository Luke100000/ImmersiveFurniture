package immersive_furniture.client.model.effects;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.widgets.BoundedIntSliderButton;
import net.minecraft.nbt.CompoundTag;

public class LightMaterialEffect extends MaterialEffect {
    private float roundness = 0.0f;
    private float brightness = 0.0f;
    private float contrast = 0.0f;

    public LightMaterialEffect() {

    }

    public LightMaterialEffect(LightMaterialEffect lightEffect) {
        this.roundness = lightEffect.roundness;
        this.brightness = lightEffect.brightness;
        this.contrast = lightEffect.contrast;
    }

    @Override
    public void load(CompoundTag tag) {
        roundness = tag.getFloat("Roundness");
        brightness = tag.getFloat("Brightness");
        contrast = tag.getFloat("Contrast");
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Roundness", roundness);
        tag.putFloat("Brightness", brightness);
        tag.putFloat("Contrast", contrast);
        return tag;
    }

    @Override
    public int initGUI(ArtisansWorkstationEditorScreen screen, int x, int y, int width) {
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

    public float getRoundness() {
        return roundness;
    }

    public float getBrightness() {
        return brightness;
    }

    public float getContrast() {
        return contrast;
    }
}
