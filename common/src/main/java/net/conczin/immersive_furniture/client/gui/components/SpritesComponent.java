package net.conczin.immersive_furniture.client.gui.components;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.SpriteButton;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.conczin.immersive_furniture.mixin.client.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.*;

public class SpritesComponent extends ListComponent {
    private final List<ResourceLocation> allSprites;
    private final List<ResourceLocation> filteredSprites = new LinkedList<>();

    private final Map<ResourceLocation, Boolean> transparencyCache = new HashMap<>();

    final List<SpriteButton> spriteButtons = new ArrayList<>();

    StateImageButton vanillaOnlyButton;
    StateImageButton transparencyFilterButton;
    StateImageButton animationFilterButton;
    StateImageButton itemFilterButton;

    private boolean vanillaOnly = true;
    private boolean transparencyFilter = false;
    private boolean animationFilter = false;
    private boolean itemFilter = false;

    public SpritesComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);

        List<SpriteContents> sprites = ((TextureAtlasAccessor) Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)).getSprites();
        allSprites = sprites.stream()
                .filter(SpritesComponent::isSquare)
                .filter(s -> ((SpriteContentsAccessor) s).immersiveFurniture$getFrameCount() > 1 || isTransparent(s))
                .map(SpriteContents::name)
                .toList();
    }

    private static boolean isSquare(SpriteContents spriteContents) {
        int width = spriteContents.width();
        int height = spriteContents.height();
        return width == height && Math.pow((int) Math.sqrt(width), 2) == width;
    }

    private boolean isTransparent(SpriteContents s) {
        return transparencyCache.computeIfAbsent(s.name(), location -> computeIsTransparent(s));
    }

    private boolean computeIsTransparent(SpriteContents s) {
        NativeImage image = ((SpriteContentsAccessor) s).getMipLevelData()[0];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (FastColor.ABGR32.alpha(image.getPixelRGBA(x, y)) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        // Toggle vanilla only
        vanillaOnlyButton = addToggleButton(leftPos + 6, topPos + 22, 16, 96, 96, "gui.immersive_furniture.vanilla_only", () -> {
            vanillaOnly = !vanillaOnly;
            vanillaOnlyButton.setEnabled(vanillaOnly);
            updateSearch(searchBox.getValue());
        });
        vanillaOnlyButton.setEnabled(vanillaOnly);

        // Toggle transparency filter
        transparencyFilterButton = addToggleButton(leftPos + 6 + 18, topPos + 22, 16, 112, 96, "gui.immersive_furniture.transparency_filter", () -> {
            transparencyFilter = !transparencyFilter;
            transparencyFilterButton.setEnabled(transparencyFilter);
            updateSearch(searchBox.getValue());
        });
        transparencyFilterButton.setEnabled(transparencyFilter);

        // Toggle animation filter
        animationFilterButton = addToggleButton(leftPos + 6 + 36, topPos + 22, 16, 144, 96, "gui.immersive_furniture.animation_filter", () -> {
            animationFilter = !animationFilter;
            animationFilterButton.setEnabled(animationFilter);
            updateSearch(searchBox.getValue());
        });
        animationFilterButton.setEnabled(animationFilter);

        // Set to water color
        StateImageButton waterButton = addToggleButton(leftPos + 6 + 54, topPos + 22, 16, 144, 96, "gui.immersive_furniture.set_water_color", () -> {
            if (screen.selectedElement == null) return;
            screen.selectedElement.color = screen.selectedElement.color == 0x3F76E4 ? -1 : 0x3F76E4;
        });

        // Set to foliage color
        StateImageButton foliageButton = addToggleButton(leftPos + 6 + 72, topPos + 22, 16, 144, 96, "gui.immersive_furniture.set_foliage_color", () -> {
            if (screen.selectedElement == null) return;
            screen.selectedElement.color = screen.selectedElement.color == 0x3F76E4 ? -1 : 0x3F76E4;
        });

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
                button.setEnabled(screen.selectedElement != null && button.getSpriteLocation() != null &&
                                  button.getSpriteLocation().equals(screen.selectedElement.sprite.sprite));
                spriteButtons.add(button);
                screen.addRenderableWidget(button);
            }
        }

        super.init(leftPos, topPos, width, height);
    }

    @Override
    int getPages() {
        return (filteredSprites.size() - 1) / 20 + 1;
    }

    @Override
    void updateSearch(String search) {
        // Filter sprites
        filteredSprites.clear();

        // Filter sprites based on search term and filters
        for (ResourceLocation location : allSprites) {
            // Filter by search term
            if (search.isEmpty() || location.toString().contains(search)) {
                // Apply vanilla-only filter if enabled
                if (!vanillaOnly || location.getNamespace().equals("minecraft")) {
                    filteredSprites.add(location);
                }
            }
        }

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
