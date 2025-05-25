package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;

public class EffectsComponent extends ScreenComponent {
    public EffectsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        if (screen.selectedElement == null) return;

        int y = topPos + 6;
        y = screen.selectedElement.material.lightEffect.initGUI(screen, leftPos, y, width);
    }
}
