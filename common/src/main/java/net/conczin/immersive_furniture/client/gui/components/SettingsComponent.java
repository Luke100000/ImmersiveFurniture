package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationLibraryScreen;
import net.conczin.immersive_furniture.client.gui.widgets.BoundedIntSliderButton;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.client.model.*;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingsComponent extends ScreenComponent {
    static final Component SEARCH_TITLE = Component.translatable("gui.immersive_furniture.name");
    static final Component SEARCH_HINT = Component.translatable("gui.immersive_furniture.name.hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);
    static final Component TAGS_TITLE = Component.translatable("gui.immersive_furniture.tags");
    static final Component TAGS_HINT = Component.translatable("gui.immersive_furniture.tags.hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    public final static List<String> TAGS = List.of(
            "living_room",
            "bed_room",
            "kitchen",
            "bath_room",
            "office",
            "storage",
            "outdoor",
            "communal",
            "workshop",
            "miscellaneous"
    );

    private boolean localFileExists;
    private Button saveButton;

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
            updateSaveButton();
        });
        screen.addRenderableWidget(nameBox);

        int i = 0;
        int x = leftPos + 6;
        int y = topPos + 22;
        for (String tag : TAGS) {
            addToggleButton(x, y, 16, i * 16, 224,
                    "gui.immersive_furniture.tag." + tag.toLowerCase(Locale.ROOT),
                    () -> {
                        screen.data.tag = tag;
                        screen.init();
                    }).setEnabled(tag.equals(screen.data.tag));
            x += 18;
            i++;
            if (x > leftPos + width - 6) {
                x = leftPos + 6;
                y += 18;
            }
        }

        // Free tags
        EditBox tagsBox = new EditBox(minecraft.font, leftPos + 6, topPos + 60, width - 12, minecraft.font.lineHeight + 3, TAGS_TITLE);
        tagsBox.setMaxLength(200);
        tagsBox.setValue(String.join(", ", screen.data.tags));
        tagsBox.setHint(TAGS_HINT);
        tagsBox.setTooltip(Tooltip.create(Component.translatable("gui.immersive_furniture.tags.tooltip")));
        tagsBox.setResponder(s -> {
            screen.data.tags = parseTags(s);
            updateSaveButton();
        });
        screen.addRenderableWidget(tagsBox);

        // Light level
        BoundedIntSliderButton lightLevelSlider = new BoundedIntSliderButton(leftPos + 6, topPos + 76, width - 12, 20, "gui.immersive_furniture.light_level", screen.data.lightLevel, 0, 15);
        lightLevelSlider.setCallback(c -> screen.data.lightLevel = c);
        screen.addRenderableWidget(lightLevelSlider);

        // Inventory space
        BoundedIntSliderButton inventorySlider = new BoundedIntSliderButton(leftPos + 6, topPos + 98, width - 12, 20, "gui.immersive_furniture.inventory", screen.data.inventorySize, 0, 6);
        inventorySlider.setCallback(c -> screen.data.inventorySize = c);
        screen.addRenderableWidget(inventorySlider);

        // Allow toggle with right click
        int togglesY = topPos + 122;
        StateImageButton rightClickToggle = addToggleButton(leftPos + 6, togglesY, 16, 192, 160, "gui.immersive_furniture.toggle_with_right_click", () -> {
            screen.data.toggleWithRightClick = !screen.data.toggleWithRightClick;
            screen.init();
        });
        rightClickToggle.setEnabled(!screen.data.toggleWithRightClick);

        // Toggle light
        StateImageButton lightToggle = addToggleButton(leftPos + 24, togglesY, 16, 208, 160, "gui.immersive_furniture.toggle_light", () -> {
            screen.data.toggleLight = !screen.data.toggleLight;
            screen.init();
        });
        lightToggle.setEnabled(!screen.data.toggleLight);

        // Save
        saveButton = addButton("gui.immersive_furniture.save", b -> {
            // Finish and bake the model and save
            finish(screen.data);
            FurnitureDataManager.saveLocalFile(screen.data);

            // Delete the autosave file if it exists
            FurnitureDataManager.deleteLocalFile(ResourceLocation.fromNamespaceAndPath("local", "autosave"));

            // Switch to the library screen
            ArtisansWorkstationLibraryScreen libraryScreen = new ArtisansWorkstationLibraryScreen();
            libraryScreen.setSelected(FurnitureDataManager.getSafeLocalLocation(screen.data));
            libraryScreen.setTab(ArtisansWorkstationLibraryScreen.Tab.LOCAL);
            Minecraft.getInstance().setScreen(libraryScreen);
        }, leftPos + 6, topPos + height - 26, width - 12);
        updateSaveButton();
    }

    private static Set<String> parseTags(String text) {
        Set<String> tags = new LinkedHashSet<>();
        for (String tag : text.split(",")) {
            tag = tag.trim().toLowerCase(Locale.ROOT);
            if (!tag.isEmpty()) tags.add(tag);
        }
        return tags;
    }

    private String getMissingRequirement() {
        if (screen.data.name.isBlank()) return "gui.immersive_furniture.name_required";
        if (!screen.data.hasCategory() && screen.data.tags.isEmpty()) return "gui.immersive_furniture.tag_required";
        return null;
    }

    private void updateSaveButton() {
        saveButton.active = getMissingRequirement() == null;
    }

    public void render(GuiGraphics graphics) {
        String missingRequirement = getMissingRequirement();
        if (missingRequirement != null) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable(missingRequirement),
                    screen.getRightPaneCenterX(), topPos + height - 16, 0xFF5555
            );
        } else if (localFileExists) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("gui.immersive_furniture.overwrite_file"),
                    screen.getRightPaneCenterX(), topPos + height - 16, 0xFFFFFF
            );
        }
    }

    private Button addButton(String message, Button.OnPress onPress, int x, int y, int w) {
        return screen.addRenderableWidget(
                Button.builder(Component.translatable(message), onPress)
                        .bounds(x, y, w, 20)
                        .build()
        );
    }

    public void finish(FurnitureData data) {
        // Set author
        data.author = Minecraft.getInstance().getUser().getName();

        // Fix obvious usage errors
        if (data.hasDisplayItems()) {
            data.inventorySize = Math.max(1, data.inventorySize);
        }

        // Find and log sources of textures
        data.sources.clear();
        for (FurnitureData.Element element : data.elements) {
            if (element.type != FurnitureData.ElementType.ELEMENT) continue;
            MaterialSource source = MaterialRegistry.INSTANCE.materials.getOrDefault(element.material.source, MaterialSource.DEFAULT);

            for (Direction value : Direction.values()) {
                ResourceLocation location = source.getMaterial(value).sprite().contents().name();
                data.sources.add(location.getNamespace());

                // Also try to detect resource packs
                ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "textures/" + location.getPath() + ".png");
                Minecraft.getInstance().getResourceManager().getResource(textureLocation)
                        .ifPresent(resource -> data.sources.add(Utils.beatifyPackID(resource.sourcePackId())));
            }
        }

        data.sources.remove("vanilla");
        data.sources.remove("minecraft");
        data.sources.remove("mod_resources");
        data.sources.remove("mod resources");

        // Find and log dependencies
        data.dependencies.clear();
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.PARTICLE_EMITTER) {
                data.dependencies.add(element.particleEmitter.particle.getNamespace());
            } else if (element.type == FurnitureData.ElementType.SOUND_EMITTER) {
                data.dependencies.add(element.soundEmitter.sound.getNamespace());
            } else if (element.type == FurnitureData.ElementType.SPRITE) {
                data.dependencies.add(element.sprite.sprite.getNamespace());
            }
        }
        data.dependencies.remove("minecraft");
        data.dependencies.remove("mod resources");

        // Bake after all metadata changes so the preview cache uses the final saved hash.
        data.invalidateHash();
        TransparencyManager.prepare(data);
        try (var ignored = DynamicAtlas.SCRATCH.bakeLock.acquire()) {
            DynamicAtlas.SCRATCH.clear();
            FurnitureModelFactory.getModel(data, DynamicAtlas.SCRATCH);
        }
    }
}
