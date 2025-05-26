package immersive_furniture.client.gui.components;

import immersive_furniture.client.FurnitureDataManager;
import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.ArtisansWorkstationLibraryScreen;
import immersive_furniture.client.gui.widgets.BoundedIntSliderButton;
import immersive_furniture.client.model.FurnitureModelBaker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public class SettingsComponent extends ScreenComponent {
    static final Component SEARCH_TITLE = Component.translatable("gui.immersive_furniture.name");
    static final Component SEARCH_HINT = Component.translatable("gui.immersive_furniture.name.hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    public final static List<String> TAGS = List.of(
            "table",
            "chair",
            "storage",
            "light",
            "decorative",
            "functional"
    );

    private BoundedIntSliderButton lightLevelSlider;

    private boolean localFileExists;

    public SettingsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        this.localFileExists = FurnitureDataManager.localFileExists(screen.data);

        // Name box
        EditBox nameBox = new EditBox(minecraft.font, leftPos + 6, topPos + 6, width - 12, minecraft.font.lineHeight + 3, SEARCH_TITLE);
        nameBox.setMaxLength(50);
        nameBox.setVisible(true);
        nameBox.setValue(screen.data.name);
        nameBox.setHint(SEARCH_HINT);
        nameBox.setResponder(s -> {
            screen.data.name = s;
            this.localFileExists = FurnitureDataManager.localFileExists(screen.data);
        });
        screen.addRenderableWidget(nameBox);

        int x = leftPos + 6;
        int y = topPos + 22;
        for (String tag : TAGS) {
            addToggleButton(x, y, 16, 48, 96,
                    "gui.immersive_furniture.tag." + tag.toLowerCase(Locale.ROOT),
                    () -> screen.data.tag = tag).setEnabled(!tag.equals("light"));
            x += 18;
            if (x > leftPos + width - 6) {
                x = leftPos + 6;
                y += 18;
            }
        }

        // Light level
        this.lightLevelSlider = new BoundedIntSliderButton(leftPos + 6, topPos + 60, width - 12, 20, "gui.immersive_furniture.light_level", 0, 0, 15);
        screen.addRenderableWidget(lightLevelSlider);

        // Inventory space
        this.lightLevelSlider = new BoundedIntSliderButton(leftPos + 6, topPos + 82, width - 12, 20, "gui.immersive_furniture.inventory", 0, 0, 6);
        screen.addRenderableWidget(lightLevelSlider);

        // Save
        addButton("gui.immersive_furniture.save", b -> {
            screen.data.lightLevel = lightLevelSlider.getIntegerValue();
            screen.data.inventorySize = lightLevelSlider.getIntegerValue();

            // Bake the model and save the face textures
            FurnitureModelBaker.bakeTexture(screen.data);

            FurnitureDataManager.saveLocalFile(screen.data);

            // Switch to the library screen
            ArtisansWorkstationLibraryScreen libraryScreen = new ArtisansWorkstationLibraryScreen();
            libraryScreen.setSelected(FurnitureDataManager.getSafeLocalLocation(screen.data));
            libraryScreen.setTab(ArtisansWorkstationLibraryScreen.Tab.LOCAL);
            Minecraft.getInstance().setScreen(libraryScreen);
        }, leftPos + 6, topPos + 154, width - 12);
    }

    public void render(GuiGraphics context) {
        if (localFileExists) {
            context.drawCenteredString(
                    minecraft.font,
                    Component.translatable("gui.immersive_furniture.overwrite_file"),
                    leftPos + width + (280 - width) / 2, topPos + height - 16, 0xFFFFFF
            );
        }
    }

    private void addButton(String message, Button.OnPress onPress, int x, int y, int w) {
        screen.addRenderableWidget(
                Button.builder(Component.translatable(message), onPress)
                        .bounds(x, y, w, 20)
                        .build()
        );
    }
}
