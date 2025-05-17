package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;

public class EffectsComponent extends ScreenComponent {
    public EffectsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        // Basically hardcoded tick buttons and options, with each effect having its own class instance
        // overall seed
        // worn - -100% to 100% to darken/lighting center and corner
        // moss - top, down
        // rust - top, down
        // dirt - top, down
        // burned
    }
}
