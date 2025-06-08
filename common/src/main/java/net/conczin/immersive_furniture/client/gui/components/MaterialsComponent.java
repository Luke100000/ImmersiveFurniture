package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.MaterialButton;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.client.model.MaterialSource;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.MaterialRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MaterialsComponent extends ListComponent {
    private final List<Map.Entry<ResourceLocation, MaterialSource>> filteredMaterials = new LinkedList<>();

    final List<MaterialButton> materialButtons = new ArrayList<>();

    StateImageButton rotateButton;
    StateImageButton flipButton;
    StateImageButton repeatButton;
    StateImageButton favoriteButton;

    public MaterialsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        // Material settings
        if (screen.selectedElement != null) {
            // Toggle 90° rotation
            rotateButton = addToggleButton(leftPos + 6, topPos + 22, 16, 96, 96, "gui.immersive_furniture.rotate", () -> {
                screen.selectedElement.material.rotate = !screen.selectedElement.material.rotate;
                rotateButton.setEnabled(screen.selectedElement.material.rotate);
            });
            rotateButton.setEnabled(screen.selectedElement.material.rotate);

            // Toggle flip
            flipButton = addToggleButton(leftPos + 6 + 18, topPos + 22, 16, 112, 96, "gui.immersive_furniture.flip", () -> {
                screen.selectedElement.material.flip = !screen.selectedElement.material.flip;
                flipButton.setEnabled(screen.selectedElement.material.flip);
            });
            flipButton.setEnabled(screen.selectedElement.material.flip);

            // Toggle repeat
            repeatButton = addToggleButton(leftPos + 6 + 36, topPos + 22, 16, 144, 96, "gui.immersive_furniture.repeat", () -> {
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
            favoriteButton = addToggleButton(leftPos + 100 - 6 - 16, topPos + 22, 16, 128, 96, "gui.immersive_furniture.favorite", () -> {
                String location = screen.selectedElement.material.source.toString();
                if (Config.getInstance().favorites.contains(location)) {
                    Config.getInstance().favorites.remove(location);
                    favoriteButton.setEnabled(true);
                } else {
                    Config.getInstance().favorites.add(location);
                    favoriteButton.setEnabled(false);
                }
                Config.getInstance().save();
            });
            favoriteButton.setEnabled(!Config.getInstance().favorites.contains(screen.selectedElement.material.source.toString()));
        }

        // Material buttons
        materialButtons.clear();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 4; x++) {
                MaterialButton button = new MaterialButton(
                        leftPos + 6 + x * 22, topPos + 44 + y * 22,
                        22, 22, 234, 162,
                        b -> {
                            if (screen.selectedElement != null) {
                                screen.selectedElement.material.source = ((MaterialButton) b).getMaterial().location();
                                screen.init();
                            }
                        }
                );
                button.setEnabled(screen.selectedElement != null && button.getMaterial() != null && button.getMaterial().location().equals(screen.selectedElement.material.source));
                materialButtons.add(button);
                screen.addRenderableWidget(button);
            }
        }

        super.init(leftPos, topPos, width, height);
    }

    @Override
    int getPages() {
        return (filteredMaterials.size() - 1) / 16 + 1;
    }

    @Override
    void updateSearch(String search) {
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
}
