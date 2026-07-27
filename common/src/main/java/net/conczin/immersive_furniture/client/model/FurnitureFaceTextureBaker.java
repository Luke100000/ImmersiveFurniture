package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.conczin.immersive_furniture.client.Utils;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FurnitureFaceTextureBaker {
    private final DynamicAtlas atlas;
    private final boolean inEditor;
    private final List<FurnitureData.Element> elements;
    private final Map<Integer, AmbientOcclusion> ambientOcclusion = new HashMap<>();

    FurnitureFaceTextureBaker(DynamicAtlas atlas, boolean inEditor, List<FurnitureData.Element> elements) {
        this.atlas = atlas;
        this.inEditor = inEditor;
        this.elements = elements;
    }

    BlockElementFace bake(
            FurnitureData.Element element,
            Direction direction,
            int state,
            Direction cullDirection,
            int tintIndex
    ) {
        Vector2i dimensions = ClientModelUtils.getFaceDimensions(element, direction);
        int padding = getPadding();
        DynamicAtlas.Quad quad = atlas.allocate(
                (int) (Math.ceil(dimensions.x / (double) padding) * padding),
                (int) (Math.ceil(dimensions.y / (double) padding) * padding)
        );

        if (quad.w() > 0 && quad.h() > 0) {
            writeTexture(element, direction, state, dimensions, quad);
        }

        float uvScale = 16.0f / atlas.size;
        return new BlockElementFace(
                cullDirection,
                tintIndex,
                "0",
                new BlockFaceUV(new float[]{
                        quad.x() * uvScale,
                        quad.y() * uvScale,
                        (quad.x() + dimensions.x()) * uvScale,
                        (quad.y() + dimensions.y()) * uvScale
                }, 0)
        );
    }

    private int getPadding() {
        if (atlas != DynamicAtlas.BAKED) return 1;

        int mipLevel = Minecraft.getInstance().options.mipmapLevels().get();
        int pressureReduction = atlas.getUsage() > 0.75 ? 2 : atlas.getUsage() > 0.5 ? 1 : 0;
        return 1 << Math.max(0, Math.min(mipLevel - pressureReduction, Config.getInstance().maxMipLevel));
    }

    private void writeTexture(
            FurnitureData.Element element,
            Direction direction,
            int state,
            Vector2i dimensions,
            DynamicAtlas.Quad quad
    ) {
        NativeImage pixels = atlas.getPixels();
        assert pixels != null;

        int[] baked = inEditor ? null : element.bakedTextures.get(direction, state);
        boolean useBaked = baked != null && baked.length == dimensions.x * dimensions.y;
        if (!useBaked) baked = new int[dimensions.x * dimensions.y];

        AmbientOcclusion ao = useBaked ? null : getAmbientOcclusion(state);
        ElementRotation rotation = element.getRotation();
        Vector3f normal = useBaked ? null : ModelUtils.getElementRotation(rotation).transform(direction.step());
        Vector3f center = useBaked ? null : element.getCenter();
        FurnitureData.LightMaterialEffect lightEffect = element.material.lightEffect;

        for (int x = 0; x < dimensions.x; x++) {
            for (int y = 0; y < dimensions.y; y++) {
                int index = x + y * dimensions.x;
                int color = useBaked
                        ? baked[index]
                        : shadePixel(element, direction, dimensions, x, y, rotation, normal, center, lightEffect, ao);
                baked[index] = color;
                pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
            }
        }

        padTexture(pixels, dimensions, quad);
        atlas.setDirty();
        if (inEditor) element.bakedTextures.put(direction, state, baked);
    }

    private int shadePixel(
            FurnitureData.Element element,
            Direction direction,
            Vector2i dimensions,
            int x,
            int y,
            ElementRotation rotation,
            Vector3f normal,
            Vector3f center,
            FurnitureData.LightMaterialEffect effect,
            AmbientOcclusion ao
    ) {
        int sourceColor = MaterialSource.fromCube(element.material, direction, center, x, y, dimensions.x, dimensions.y);
        int r = sourceColor >> 16 & 0xFF;
        int g = sourceColor >> 8 & 0xFF;
        int b = sourceColor & 0xFF;
        int a = sourceColor >>> 24;

        float[] hsv = Utils.rgbToHsv(r / 255.0f, g / 255.0f, b / 255.0f);
        if (Math.abs(effect.hue) >= 1.0f) hsv[0] = positiveModulo(effect.hue * 1.8f, 360.0f);
        hsv[1] = Math.max(0.0f, Math.min(1.0f, hsv[1] + effect.saturation * 0.01f));
        hsv[2] = Math.max(0.0f, Math.min(1.0f, hsv[2] + effect.value * 0.01f));
        float[] rgb = Utils.hsvToRgbRaw(hsv[0], hsv[1], hsv[2]);
        r = (int) (rgb[0] * 255);
        g = (int) (rgb[1] * 255);
        b = (int) (rgb[2] * 255);

        float light = 1.0f;
        float roundness = effect.roundness / 75.0f;
        if (roundness != 0.0f) {
            light = quantize(x, y, getLight(x, y, dimensions));
            light = light * roundness + (1.0f - roundness * 0.5f);
        }
        light += effect.brightness / 100.0f;

        Vector3f position = new Vector3f(ClientModelUtils.to3D(element, direction, x, y));
        ModelUtils.applyElementRotation(position, rotation);
        float ambientLight = Math.min(1.0f, Math.max(0.0f, 1.0f - ao.sample(position, normal) * 1.5f));
        float emission = element.emission / 15.0f;
        light *= ambientLight * (1.0f - emission) + emission;

        float contrast = effect.contrast / 100.0f;
        r = applyContrast(r, contrast, light);
        g = applyContrast(g, contrast, light);
        b = applyContrast(b, contrast, light);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private AmbientOcclusion getAmbientOcclusion(int state) {
        return ambientOcclusion.computeIfAbsent(state, ignored -> {
            AmbientOcclusion ao = new AmbientOcclusion();
            for (FurnitureData.Element element : elements) {
                if (element.type == FurnitureData.ElementType.ELEMENT && element.isMasked(state)) {
                    ao.place(element, element.material.transparency == TransparencyType.SOLID ? 1.0f : 0.25f);
                }
            }
            return ao;
        });
    }

    private static void padTexture(NativeImage pixels, Vector2i dimensions, DynamicAtlas.Quad quad) {
        for (int x = dimensions.x(); x < quad.w(); x++) {
            for (int y = 0; y < dimensions.y(); y++) {
                pixels.setPixelRGBA(quad.x() + x, quad.y() + y,
                        pixels.getPixelRGBA(quad.x() + x - 1, quad.y() + y));
            }
        }
        for (int y = dimensions.y(); y < quad.h(); y++) {
            for (int x = 0; x < quad.w(); x++) {
                pixels.setPixelRGBA(quad.x() + x, quad.y() + y,
                        pixels.getPixelRGBA(quad.x() + x, quad.y() + y - 1));
            }
        }
    }

    private static int applyContrast(int color, float contrast, float light) {
        return (int) Math.max(0.0, Math.min(255.0, ((color - 128) * (1.0f + contrast) + 128) * light));
    }

    private static float positiveModulo(float value, float modulus) {
        return (value % modulus + modulus) % modulus;
    }

    private static float getLight(int x, int y, Vector2i dimensions) {
        float rx = x / (dimensions.x - 1.0f) * 2.0f - 1.0f;
        float ry = y / (dimensions.y - 1.0f) * 2.0f - 1.0f;
        float r = 1.0f - Math.max(Math.abs(rx), Math.abs(ry)) * 0.95f;
        float dist = Math.max(0.0f, 1.0f - (float) Math.sqrt(rx * rx + ry * ry) / 1.42f);
        return (float) Math.sqrt(r * (1.0f - r) + dist * r);
    }

    private static float quantize(int x, int y, float light) {
        int levels = 8;
        int hash = x * 0x27d4eb2d ^ y * 0x85ebca6b;
        hash ^= hash >>> 16;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        hash *= 0xc2b2ae35;
        hash ^= hash >>> 16;
        float n = (hash & 0xFFFFFFFFL) / (float) (1L << 32);
        return (float) Math.round(light * levels + n) / levels;
    }
}
