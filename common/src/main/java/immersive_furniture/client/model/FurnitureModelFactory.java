package immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import immersive_furniture.Common;
import immersive_furniture.client.model.effects.LightMaterialEffect;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class FurnitureModelFactory {
    private static final Map<@NotNull String, @NotNull Either<Material, String>> TEXTURE_MAP = Map.of(
            "0", Either.left(new Material(InventoryMenu.BLOCK_ATLAS, Common.locate("block/furniture")))
    );

    private final FurnitureData data;
    private final DynamicAtlas atlas;

    private FurnitureModelFactory(FurnitureData data, DynamicAtlas atlas) {
        this.data = data;
        this.atlas = atlas;
    }

    private BlockElementFace getFace(FurnitureData.Element element, Direction direction) {
        // Allocate pixels
        Vector2i dimensions = ModelUtils.getFaceDimensions(element, direction);
        DynamicAtlas.Quad quad = atlas.allocate(dimensions.x, dimensions.y);

        if (quad.w() > 0 && quad.h() > 0) {
            // Render
            NativeImage pixels = atlas.getPixels();
            assert pixels != null;

            // Use baked texture if available
            int[] baked = element.bakedTexture.get(direction);
            if (baked != null && baked.length != dimensions.x * dimensions.y) baked = null;

            for (int x = 0; x < dimensions.x; x++) {
                for (int y = 0; y < dimensions.y; y++) {
                    int color;
                    if (baked == null) {
                        color = MaterialSource.fromCube(element.material, direction, x, y, dimensions.x, dimensions.y);
                        int r = ((color >> 16) & 0xFF);
                        int g = ((color >> 8) & 0xFF);
                        int b = (color & 0xFF);
                        int a = ((color >> 24) & 0xFF);

                        BlockElementRotation rotation = element.getRotation();
                        Vector3f pos = new Vector3f(ModelUtils.to3D(element, direction, x, y));
                        ModelUtils.applyElementRotation(pos.mul(1.0f / 16.0f), rotation);
                        pos.mul(16.0f);

                        Vector3f normal = ModelUtils.getElementRotation(rotation).transform(direction.step());

                        LightMaterialEffect lightEffect = element.material.lightEffect;

                        // Smooth light
                        float light = 1.0f;
                        float roundness = lightEffect.getRoundness() / 75.0f;
                        if (roundness != 0.0f) {
                            light = getLight(x, y, dimensions);
                            light = quantize(x, y, light);
                            light = light * roundness + (1.0f - roundness * 0.5f);
                        }

                        // Brightness
                        light += lightEffect.getBrightness() / 100.0f;

                        // Ambient Occlusion
                        float ao = Math.min(1.0f, Math.max(0.0f, 1.0f - AmbientOcclusion.INSTANCE.sample(pos, normal) * 1.5f));
                        light *= ao;

                        // Contrast
                        float contrast = lightEffect.getContrast() / 100.0f;
                        r = (int) Math.max(0.0, Math.min(255.0, ((r - 128) * (1.0f + contrast) + 128) * light));
                        g = (int) Math.max(0.0, Math.min(255.0, ((g - 128) * (1.0f + contrast) + 128) * light));
                        b = (int) Math.max(0.0, Math.min(255.0, ((b - 128) * (1.0f + contrast) + 128) * light));

                        color = (a << 24) | (r << 16) | (g << 8) | b;
                    } else {
                        color = baked[x + y * dimensions.x];
                    }
                    pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
                }
            }

            atlas.upload();
        }

        return new BlockElementFace(
                null, // TODO
                -1,
                "#0",
                new BlockFaceUV(
                        new float[]{
                                quad.x(),
                                quad.y(),
                                (quad.x() + quad.w()),
                                (quad.y() + quad.h())
                        },
                        0
                )
        );
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
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        float n = (hash & 0xFFFFFFFFL) / (float) (1L << 32);
        light = (float) Math.round(light * levels + n) / levels;
        return light;
    }

    private BlockElement getElement(FurnitureData.Element element) {
        return new BlockElement(
                element.from,
                element.to,
                Map.of(
                        Direction.UP, getFace(element, Direction.UP),
                        Direction.DOWN, getFace(element, Direction.DOWN),
                        Direction.NORTH, getFace(element, Direction.NORTH),
                        Direction.SOUTH, getFace(element, Direction.SOUTH),
                        Direction.WEST, getFace(element, Direction.WEST),
                        Direction.EAST, getFace(element, Direction.EAST)
                ),
                element.getRotation(),
                true
        );
    }

    private BlockModel getModel() {
        return new BlockModel(
                null,
                data.elements.stream().map(this::getElement).toList(),
                TEXTURE_MAP,
                true,
                BlockModel.GuiLight.SIDE,
                getTransforms(),
                List.of()
        );
    }

    private ItemTransforms getTransforms() {
        float scale = (float) (1.0 / (Math.max(8.0, data.getSize()) / 16.0));
        return new ItemTransforms(
                new ItemTransform(new Vector3f(75, 225, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f * scale)),
                new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f * scale)),
                new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(), new Vector3f(0.4f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(0, 45, 0), new Vector3f(0.4f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f)),
                new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(0.625f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(0, 3, 0), new Vector3f(0.25f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(0.5f * scale))
        );
    }

    public static BlockModel getModel(FurnitureData data, DynamicAtlas atlas) {
        // Populate AO lookup
        AmbientOcclusion.INSTANCE.clear();
        for (FurnitureData.Element element : data.elements) {
            AmbientOcclusion.INSTANCE.place(element);
        }

        atlas.clear(); // TODO
        return new FurnitureModelFactory(data, atlas).getModel();
    }
}
