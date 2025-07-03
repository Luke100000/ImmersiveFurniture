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
        FurnitureData.Element element = screen.getFirstElement().orElse(null);
        if (element != null) {
            // Texture axis
            int i = 0;
            for (FurnitureData.MaterialAxis axis : FurnitureData.MaterialAxis.values()) {
                addToggleButton(leftPos + 6 + i * 18, topPos + 22, 16, 16 + i * 16, 192, "", () -> {
                    screen.selectedElements.forEach(e -> e.material.axis = axis);
                    screen.init();
                }).setEnabled(element.material.axis != axis);
                i++;
            }

            // Toggle repeat
            repeatButton = addToggleButton(leftPos + 6 + 54, topPos + 22, 16, 144, 192, "gui.immersive_furniture.repeat", () -> {
                if (element.material.wrap == FurnitureData.WrapMode.EXPAND) {
                    screen.selectedElements.forEach(e -> e.material.wrap = FurnitureData.WrapMode.REPEAT);
                    repeatButton.setEnabled(false);
                } else {
                    screen.selectedElements.forEach(e -> e.material.wrap = FurnitureData.WrapMode.EXPAND);
                    repeatButton.setEnabled(true);
                }
            });
            repeatButton.setEnabled(element.material.wrap == FurnitureData.WrapMode.EXPAND);

            // Mark as favorite
            favoriteButton = addToggleButton(leftPos + 100 - 6 - 16, topPos + 22, 16, 128, 192, "gui.immersive_furniture.favorite", () -> {
                String location = element.material.source.toString();
                if (Config.getInstance().favorites.contains(location)) {
                    Config.getInstance().favorites.remove(location);
                    favoriteButton.setEnabled(true);
                } else {
                    Config.getInstance().favorites.add(location);
                    favoriteButton.setEnabled(false);
                }
                Config.getInstance().save();
                screen.init();
            });
            favoriteButton.setEnabled(!Config.getInstance().favorites.contains(element.material.source.toString()));
        }

        // Material buttons
        materialButtons.clear();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 4; x++) {
                MaterialButton button = new MaterialButton(
                        leftPos + 6 + x * 22, topPos + 44 + y * 22,
                        22, 22, 146, 0,
                        b -> {
                            MaterialSource material = ((MaterialButton) b).getMaterial();
                            if (material != null) {
                                screen.selectedElements.forEach(e -> e.material.source = material.location());
                                screen.init();
                            }
                        }
                );
                button.setEnabled(element != null && button.getMaterial() != null && button.getMaterial().location().equals(element.material.source));
                materialButtons.add(button);
                screen.addRenderableWidget(button);
            }
        }

        super.init(leftPos, topPos, width, height);
    }

    @Override
    int getPages() {
        return Math.max(0, (filteredMaterials.size() - 1) / 20 + 1);
    }

    @Override
    void updateSearch() {
        // Filter materials
        filteredMaterials.clear();
        MaterialRegistry.INSTANCE.materials.entrySet().stream()
                .filter(entry -> Utils.search(searchBox.getValue(), entry.getKey().toString()))
                .sorted(Comparator.comparingInt(a -> (Config.getInstance().favorites.contains(a.getKey().toString()) ? 0 : 1)))
                .forEach(filteredMaterials::add);

        page = Math.min(page, getPages() - 1);

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