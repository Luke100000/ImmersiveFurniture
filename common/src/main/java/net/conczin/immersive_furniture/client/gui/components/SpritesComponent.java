package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.SpriteButton;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.client.model.TransparencyManager;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.conczin.immersive_furniture.mixin.client.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class SpritesComponent extends ListComponent {
    private final List<SpriteContents> allSprites;
    private List<ResourceLocation> filteredSprites = new LinkedList<>();

    final List<SpriteButton> spriteButtons = new ArrayList<>();

    private boolean vanillaOnly = true;

    public enum FilterType {
        ANIMATIONS,
        SPRITES,
        ITEMS,
        ALL
    }

    public FilterType filterType = FilterType.ANIMATIONS;

    public SpritesComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);

        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        List<SpriteContents> sprites = ((TextureAtlasAccessor) atlas).getSprites();
        allSprites = sprites.stream()
                .filter(SpritesComponent::sanityFilter)
                .filter(SpritesComponent::isSquare)
                .toList();
    }

    private static boolean sanityFilter(SpriteContents s) {
        if (s.name().getPath().endsWith("_side")) return false;
        if (s.name().getPath().endsWith("_bottom")) return false;
        return !s.name().getPath().endsWith("_top");
    }

    private static boolean isSquare(SpriteContents spriteContents) {
        int width = spriteContents.width();
        int height = spriteContents.height();
        return width == height && Math.pow((int) Math.sqrt(width), 2) == width;
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        int tx = 7;
        int u = 64;
        for (FilterType value : FilterType.values()) {
            StateImageButton button = addToggleButton(leftPos + tx, topPos + 22, 16, u, 224,
                    "gui.immersive_furniture.sprite_filter." + value.name().toLowerCase(Locale.ROOT), () -> {
                        filterType = value;
                        screen.init();
                    });
            button.setEnabled(filterType == value);
            tx += 18;
            u += 16;
        }

        // Vanilla only toggle
        addToggleButton(leftPos + 80, topPos + 22, 16, 144, 224,
                "gui.immersive_furniture.vanilla", () -> {
                    vanillaOnly = !vanillaOnly;
                    screen.init();
                }).setEnabled(vanillaOnly);

        // Sprite buttons
        spriteButtons.clear();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 4; x++) {
                SpriteButton button = new SpriteButton(
                        leftPos + 6 + x * 22, topPos + 44 + y * 22,
                        22, 22, 234, 162,
                        b -> {
                            ResourceLocation spriteLocation = ((SpriteButton) b).getSpriteLocation();
                            if (screen.selectedElement != null && spriteLocation != null) {
                                screen.selectedElement.sprite.sprite = spriteLocation;
                                screen.init();
                            }
                        }
                );
                button.setEnabled(
                        screen.selectedElement != null && button.getSpriteLocation() != null &&
                        button.getSpriteLocation().equals(screen.selectedElement.sprite.sprite)
                );
                spriteButtons.add(button);
                screen.addRenderableWidget(button);
            }
        }

        super.init(leftPos, topPos, width, height);
    }

    @Override
    int getPages() {
        return Math.max(0, (filteredSprites.size() - 1) / 20 + 1);
    }

    private boolean filter(SpriteContents s) {
        return switch (filterType) {
            case ANIMATIONS -> ((SpriteContentsAccessor) s).immersiveFurniture$getFrameCount() > 1;
            case ITEMS -> s.name().toString().contains("item/");
            case SPRITES -> TransparencyManager.INSTANCE.getTransparencyType(s) != TransparencyType.SOLID
                            && TransparencyManager.INSTANCE.isCornerTransparent(s)
                            && !s.name().toString().contains("item/");
            case ALL -> true;
        };
    }

    @Override
    void updateSearch(String search) {
        // Filter sprites
        filteredSprites = allSprites.stream()
                .filter(s -> !vanillaOnly || s.name().getNamespace().equals("minecraft"))
                .filter(s -> s.name().toString().contains(search))
                .filter(this::filter)
                .map(SpriteContents::name).toList();

        page = Math.max(0, Math.min(page, getPages() - 1));

        // Get the texture atlas
        TextureAtlas blockAtlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);

        for (int i = 0; i < spriteButtons.size(); i++) {
            int li = i + page * spriteButtons.size();
            if (li < filteredSprites.size()) {
                ResourceLocation location = filteredSprites.get(li);
                TextureAtlasSprite sprite = blockAtlas.getSprite(location);

                spriteButtons.get(i).setSpriteLocation(location);
                spriteButtons.get(i).setSprite(sprite);
                spriteButtons.get(i).setEnabled(screen.selectedElement != null);
            } else {
                spriteButtons.get(i).setSpriteLocation(null);
                spriteButtons.get(i).setSprite(null);
                spriteButtons.get(i).setEnabled(false);
            }
        }
    }
}
