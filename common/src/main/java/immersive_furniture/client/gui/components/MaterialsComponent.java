package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.widgets.MaterialButton;
import immersive_furniture.client.model.MaterialRegistry;
import immersive_furniture.client.model.MaterialSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class MaterialsComponent extends ScreenComponent {
    static final Component SEARCH_TITLE = Component.translatable("itemGroup.search");
    static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    EditBox searchBox;

    int page = 0;

    List<MaterialButton> materialButtons = new ArrayList<>();

    public MaterialsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        // Search box
        this.searchBox = new EditBox(minecraft.font, leftPos + 6, topPos + 6, width - 12, minecraft.font.lineHeight + 3, SEARCH_TITLE);
        this.searchBox.setMaxLength(50);
        this.searchBox.setVisible(true);
        this.searchBox.setValue("");
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setResponder(this::updateSearch);
        screen.addRenderableWidget(searchBox);

        // Material buttons
        materialButtons.clear();
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 4; x++) {
                MaterialButton button = new MaterialButton(
                        leftPos + 6 + x * 22, topPos + 24 + y * 22,
                        22, 22, 24, 48,
                        b -> {
                            // TODO: Handle button press
                        }
                );
                materialButtons.add(button);
                screen.addRenderableWidget(button);
            }
        }

        // Page buttons
        screen.addRenderableWidget(
                new ImageButton(leftPos + 6, topPos + height - 20, 12, 15, 13, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                    page = Math.max(0, page - 1);
                    updateSearch(searchBox.getValue());
                })
        );
        screen.addRenderableWidget(
                new ImageButton(leftPos + width - 18, topPos + height - 20, 12, 15, 0, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                    page += 1;
                    updateSearch(searchBox.getValue());
                })
        );

        updateSearch("");
    }

    private void updateSearch(String search) {
        for (MaterialButton button : materialButtons) {
            button.setMaterial(null);
        }
        int i = -page * materialButtons.size();
        for (Map.Entry<ResourceLocation, MaterialSource> entry : MaterialRegistry.INSTANCE.materials.entrySet()) {
            if (search.isEmpty() || entry.getKey().toString().contains(search)) {
                if (i >= 0) {
                    materialButtons.get(i).setMaterial(entry.getValue());
                }
                i++;
                if (i >= materialButtons.size()) {
                    break;
                }
            }
        }
    }

    public void render(GuiGraphics context) {
        context.drawCenteredString(minecraft.font, String.valueOf(page + 1), leftPos + width / 2, topPos + height - 16, 0xFFFFFF);
    }
}
