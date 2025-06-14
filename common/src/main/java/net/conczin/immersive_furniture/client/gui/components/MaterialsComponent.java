package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.MaterialButton;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.client.model.MaterialRegistry;
import net.conczin.immersive_furniture.client.model.MaterialSource;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MaterialsComponent extends ListComponent {
    private final List<Map.Entry<ResourceLocation, MaterialSource>> filteredMaterials = new LinkedList<>();

    final List<MaterialButton> materialButtons = new ArrayList<>();

    StateImageButton repeatButton;
    StateImageButton favoriteButton;

    public MaterialsComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        // Material settings
        if (screen.selectedElement != null) {
            // Texture axis
            int i = 0;
            for (FurnitureData.MaterialAxis axis : FurnitureData.MaterialAxis.values()) {
                addToggleButton(leftPos + 6 + i * 18, topPos + 22, 16, 16 + i * 16, 96, "", () -> {
                    screen.selectedElement.material.axis = axis;
                    screen.init();
                }).setEnabled(screen.selectedElement.material.axis != axis);
                i++;
            }

            // Toggle repeat
            repeatButton = addToggleButton(leftPos + 6 + 54, topPos + 22, 16, 144, 96, "gui.immersive_furniture.repeat", () -> {
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
                            MaterialSource material = ((MaterialButton) b).getMaterial();
                            if (screen.selectedElement != null && material != null) {
                                screen.selectedElement.material.source = material.location();
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
    void updateSearch() {
        // Filter materials
        filteredMaterials.clear();
        MaterialRegistry.INSTANCE.materials.entrySet().stream()
                .filter(entry -> Utils.search(searchBox.getValue(), entry.getKey().toString()))
                .sorted(Comparator.comparingInt(a -> (Config.getInstance().favorites.contains(a.getKey().toString()) ? 0 : 1)))
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