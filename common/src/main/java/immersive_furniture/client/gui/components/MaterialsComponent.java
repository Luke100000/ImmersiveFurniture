package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.widgets.MaterialButton;
import immersive_furniture.client.gui.widgets.StateImageButton;
import immersive_furniture.client.model.MaterialRegistry;
import immersive_furniture.client.model.MaterialSource;
import immersive_furniture.config.Config;
import immersive_furniture.data.FurnitureData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class MaterialsComponent extends ScreenComponent {
    static final Component SEARCH_TITLE = Component.translatable("itemGroup.search");
    static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    private final List<Map.Entry<ResourceLocation, MaterialSource>> filteredMaterials = new LinkedList<>();

    EditBox searchBox;
    final List<MaterialButton> materialButtons = new ArrayList<>();

    int page = 0;

    StateImageButton rotateButton;
    StateImageButton flipButton;
    StateImageButton repeatButton;
    StateImageButton favoriteButton;

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

        // Material settings
        if (screen.selectedElement != null) {
            // Toggle 90° rotation
            rotateButton = addToggleButton(leftPos + 6, topPos + 20, 16, 96, 96, "gui.immersive_furniture.rotate", () -> {
                screen.selectedElement.material.rotate = !screen.selectedElement.material.rotate;
                rotateButton.setEnabled(screen.selectedElement.material.rotate);
            });
            rotateButton.setEnabled(screen.selectedElement.material.rotate);

            // Toggle flip
            flipButton = addToggleButton(leftPos + 6 + 18, topPos + 20, 16, 112, 96, "gui.immersive_furniture.flip", () -> {
                screen.selectedElement.material.flip = !screen.selectedElement.material.flip;
                flipButton.setEnabled(screen.selectedElement.material.flip);
            });
            flipButton.setEnabled(screen.selectedElement.material.flip);

            // Toggle repeat
            repeatButton = addToggleButton(leftPos + 6 + 36, topPos + 20, 16, 144, 96, "gui.immersive_furniture.repeat", () -> {
                if (screen.selectedElement.material.wrap == FurnitureData.WrapMode.EXPAND) {
                    screen.selectedElement.material.wrap = FurnitureData.WrapMode.REPEAT;
                    repeatButton.setEnabled(false);
                } else {
                    screen.selectedElement.material.wrap = FurnitureData.WrapMode.EXPAND;
                    repeatButton.setEnabled(true);
                }
            });
            repeatButton.setEnabled(screen.selectedElement.material.wrap == FurnitureData.WrapMode.EXPAND);

            // Mark as favorite
            favoriteButton = addToggleButton(leftPos + 100 - 6 - 16, topPos + 20, 16, 128, 96, "gui.immersive_furniture.favorite", () -> {
                String location = screen.selectedElement.material.source.location().toString();
                if (Config.getInstance().favorites.contains(location)) {
                    Config.getInstance().favorites.remove(location);
                    favoriteButton.setEnabled(true);
                } else {
                    Config.getInstance().favorites.add(location);
                    favoriteButton.setEnabled(false);
                }
                Config.getInstance().save();
            });
            favoriteButton.setEnabled(!Config.getInstance().favorites.contains(screen.selectedElement.material.source.location().toString()));
        }

        // Material buttons
        materialButtons.clear();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 4; x++) {
                MaterialButton button = new MaterialButton(
                        leftPos + 6 + x * 22, topPos + 50 + y * 22,
                        22, 22, 234, 130,
                        b -> {
                            if (screen.selectedElement != null) {
                                screen.selectedElement.material.source = ((MaterialButton) b).getMaterial();
                                screen.init();
                            }
                        }
                );
                button.setEnabled(screen.selectedElement != null && button.getMaterial() == screen.selectedElement.material.source);
                materialButtons.add(button);
                screen.addRenderableWidget(button);
            }
        }

        // Page buttons
        screen.addRenderableWidget(
                new ImageButton(leftPos + 6, topPos + height - 19, 12, 15, 13, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                    page = Math.max(0, page - 1);
                    updateSearch(searchBox.getValue());
                })
        );
        screen.addRenderableWidget(
                new ImageButton(leftPos + width - 18, topPos + height - 19, 12, 15, 0, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                    page += 1;
                    updateSearch(searchBox.getValue());
                })
        );

        updateSearch("");
    }

    private int getPages() {
        return (filteredMaterials.size() - 1) / 16 + 1;
    }

    private void updateSearch(String search) {
        // Filter materials
        filteredMaterials.clear();
        MaterialRegistry.INSTANCE.materials.entrySet().stream()
                .filter(entry -> search.isEmpty() || entry.getKey().toString().contains(search))
                .sorted(Comparator.comparingInt(a -> (Config.getInstance().favorites.contains(a.getKey().toString()) ? 1 : 0)))
                .forEach(filteredMaterials::add);

        page = Math.max(0, Math.min(page, (filteredMaterials.size() - 1) / 16));

        for (int i = 0; i < materialButtons.size(); i++) {
            int li = i + page * materialButtons.size();
            if (li < filteredMaterials.size()) {
                materialButtons.get(i).setMaterial(filteredMaterials.get(li).getValue());
                materialButtons.get(i).setEnabled(true);
            } else {
                materialButtons.get(i).setMaterial(null);
                materialButtons.get(i).setEnabled(false);
            }
        }
    }

    public void render(GuiGraphics context) {
        context.drawCenteredString(minecraft.font, String.format("%s / %S", page + 1, getPages() + 1), leftPos + width / 2, topPos + height - 16, 0xFFFFFF);
    }
}
