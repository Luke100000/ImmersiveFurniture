package immersive_furniture.client.gui.components;

import immersive_furniture.client.FurnitureDataManager;
import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.widgets.BoundedIntSliderButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

    public final static List<String> INVENTORIES = List.of(
            "none",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six"
    );

    private BoundedIntSliderButton lightLevelSlider;

    private boolean localFileExists = FurnitureDataManager.localFileExists(screen.data);

    public SettingsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        // Name box
        EditBox nameBox = new EditBox(minecraft.font, leftPos + 6, topPos + 6, width - 12, minecraft.font.lineHeight + 3, SEARCH_TITLE);
        nameBox.setMaxLength(50);
        nameBox.setVisible(true);
        nameBox.setValue("");
        nameBox.setHint(SEARCH_HINT);
        nameBox.setResponder(s -> {
            screen.data.name = s;
            this.localFileExists = FurnitureDataManager.localFileExists(screen.data);
        });
        screen.addRenderableWidget(nameBox);

        int x = leftPos + 6;
        int y = topPos + 64;
        for (String tag : TAGS) {
            MutableComponent text = Component.translatable("gui.immersive_furniture.tag." + tag.toLowerCase(Locale.ROOT));
            addButton(x, topPos + 1, 16, 48, 96, text, () -> screen.data.tag = tag).setEnabled(!tag.equals("light"));

            screen.addRenderableWidget(
                    new PlainTextButton(x, y, 48, 24,
                            Component.translatable("gui.immersive_furniture.tag." + tag),
                            b -> {
                            }, Minecraft.getInstance().font)
            );
            x += 50;
        }

        // Light level
        this.lightLevelSlider = new BoundedIntSliderButton(leftPos + 6, topPos + 94, width - 12, 20, "gui.immersive_furniture.light_level", 0);
        screen.addRenderableWidget(lightLevelSlider);

        // Inventory
        int x2 = leftPos + 6;
        int y2 = topPos + 124;
        for (String inventory : INVENTORIES) {
            addButton("gui.immersive_furniture.inventory." + inventory, b -> {
            }, x2, y2, 48);
            x2 += 50;
        }

        // Save
        addButton("gui.immersive_furniture.save", b -> {
            screen.data.lightLevel = lightLevelSlider.getIntegerValue();
            FurnitureDataManager.safeLocalFile(screen.data);
        }, leftPos + 6, topPos + 154, width - 12);
    }

    public void render(GuiGraphics context) {
        if (localFileExists) {
            context.drawCenteredString(
                    minecraft.font,
                    Component.translatable("gui.immersive_furniture.overwrite_file"),
                    leftPos + width / 2, topPos + height - 16, 0xFFFFFF
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
