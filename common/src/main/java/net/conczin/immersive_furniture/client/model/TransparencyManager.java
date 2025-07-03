package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransparencyManager {
    public static final TransparencyManager INSTANCE = new TransparencyManager();

    private final Map<ResourceLocation, TransparencyType> transparencyCache = new ConcurrentHashMap<>();

    public TransparencyType getTransparencyType(SpriteContents s) {
        return transparencyCache.computeIfAbsent(s.name(), location -> compute(s));
    }

    public boolean isCornerTransparent(SpriteContents s) {
        NativeImage image = ((SpriteContentsAccessor) s).getMipLevelData()[0];
        return (FastColor.ABGR32.alpha(image.getPixelRGBA(0, 0)) < 128)
               && (FastColor.ABGR32.alpha(image.getPixelRGBA(image.getWidth() - 1, 0)) < 128)
               && (FastColor.ABGR32.alpha(image.getPixelRGBA(image.getWidth() - 1, image.getHeight() - 1)) < 128)
               && (FastColor.ABGR32.alpha(image.getPixelRGBA(0, image.getHeight() - 1)) < 128);
    }

    private TransparencyType compute(SpriteContents s) {
        NativeImage image = ((SpriteContentsAccessor) s).getMipLevelData()[0];
        boolean hasTransparency = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = FastColor.ABGR32.alpha(image.getPixelRGBA(x, y));
                if (alpha == 0) {
                    hasTransparency = true;
                } else if (alpha < 255) {
                    return TransparencyType.TRANSLUCENT;
                }
            }
        }
        return hasTransparency ? TransparencyType.CUTOUT_MIPPED : TransparencyType.SOLID;
    }

    public static void prepare(FurnitureData data) {
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.ELEMENT) {
                BlockState state = BuiltInRegistries.BLOCK.get(element.material.source).defaultBlockState();
                RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
                element.material.transparency = fromRenderType(renderType);
            }
        }
    }

    public static TransparencyType fromRenderType(RenderType renderType) {
        if (renderType == RenderType.translucent()) {
            return TransparencyType.TRANSLUCENT;
        } else if (renderType == RenderType.cutoutMipped()) {
            return TransparencyType.CUTOUT_MIPPED;
        } else if (renderType == RenderType.cutout()) {
            return TransparencyType.CUTOUT;
        } else {
            return TransparencyType.SOLID;
        }
    }

    public static TransparencyType fromSprite(FurnitureData.Element element) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.getSprite(element.sprite.sprite);
        return TransparencyManager.INSTANCE.getTransparencyType(sprite.contents());
    }

    public static void heySodiumImInUse(FurnitureData data) {
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.SPRITE) {
                TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
                atlas.getSprite(element.sprite.sprite);
            }
        }
    }
}
