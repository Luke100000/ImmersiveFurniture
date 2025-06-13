package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.conczin.immersive_furniture.mixin.client.SpriteContentsAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

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
}
