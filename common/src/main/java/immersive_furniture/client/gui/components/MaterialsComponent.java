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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class MaterialsComponent extends ScreenComponent {
    static final Component SEARCH_TITLE = Component.translatable("itemGroup.search");
    static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    record Group(String name, ResourceLocation preview) {
    }

    static final List<Group> GROUPS = List.of(
            new Group("favorites", new ResourceLocation("light_blue_glazed_terracotta")),
            new Group("wood", new ResourceLocation("oak_planks")),
            new Group("stone", new ResourceLocation("stone")),
            new Group("fabric", new ResourceLocation("white_wool")),
            new Group("mechanical", new ResourceLocation("piston_inventory")),
            new Group("all", new ResourceLocation("bricks"))
    );

    EditBox searchBox;
    final List<MaterialButton> groupButtons = new ArrayList<>();
    final List<MaterialButton> materialButtons = new ArrayList<>();

    int page = 0;
    String currentGroup = "favorites";

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
                    favoriteButton.setEnabled(false);
                } else {
                    Config.getInstance().favorites.add(location);
                    favoriteButton.setEnabled(true);
                }
            });
            favoriteButton.setEnabled(Config.getInstance().favorites.contains(screen.selectedElement.material.source.location().toString()));
        }

        // Material groups
        for (int i = 0; i < GROUPS.size(); i++) {
            final int index = i;
            int x = i % 5;
            int y = i / 5;
            MaterialButton button = new MaterialButton(
                    leftPos + 6 + x * 18, topPos + 37 + y * 17,
                    16, 16, 0, 96,
                    b -> {
                        currentGroup = GROUPS.get(index).name;
                        updateSearch(searchBox.getValue());
                        setGroupButtons();
                    }
            );
            setGroupButtons();
            button.setMaterial(MaterialRegistry.INSTANCE.materials.get(GROUPS.get(i).preview()));
            groupButtons.add(button);
            screen.addRenderableWidget(button);
        }

        // Material buttons
        materialButtons.clear();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                MaterialButton button = new MaterialButton(
                        leftPos + 6 + x * 22, topPos + 72 + y * 22,
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

    private void setGroupButtons() {
        for (MaterialButton button : groupButtons) {
            button.setEnabled(button.getMaterial() != null && button.getMaterial().location().toString().equals(currentGroup));
        }
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
