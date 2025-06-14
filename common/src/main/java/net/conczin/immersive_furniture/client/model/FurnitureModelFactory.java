package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.Utils;
import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public class FurnitureModelFactory {
    private final FurnitureData data;
    private final DynamicAtlas atlas;
    private final AmbientOcclusion ao;

    private FurnitureModelFactory(FurnitureData data, DynamicAtlas atlas) {
        // Populate AO lookup
        ao = new AmbientOcclusion();
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.ELEMENT) {
                ao.place(element);
            }
        }

        this.data = data;
        this.atlas = atlas;
    }

    private BlockElementFace getFace(FurnitureData.Element element, Direction direction) {
        // Cull fully invisible faces
        float[] fs = ClientModelUtils.getShapeData(element);
        Vector3f[] vertices = ClientModelUtils.getVertices(element, direction, fs, null);
        for (FurnitureData.Element otherElement : data.elements) {
            if (otherElement == element) continue;
            if (otherElement.type != FurnitureData.ElementType.ELEMENT) continue;
            if (theSame(element, otherElement) && otherElement.hashCode() < element.hashCode()) continue;
            if (fullyContained(otherElement, vertices)) return null;
        }

        // Allocate pixels
        Vector2i dimensions = ClientModelUtils.getFaceDimensions(element, direction);
        DynamicAtlas.Quad quad = atlas.allocate(dimensions.x, dimensions.y);

        if (quad.w() > 0 && quad.h() > 0) {
            // Render
            NativeImage pixels = atlas.getPixels();
            assert pixels != null;

            // Use baked texture if available
            boolean useBaked = true;
            int[] baked = element.bakedTexture.get(direction);
            if (baked == null || baked.length != dimensions.x * dimensions.y) {
                baked = new int[dimensions.x * dimensions.y];
                useBaked = false;
            }

            for (int x = 0; x < dimensions.x; x++) {
                for (int y = 0; y < dimensions.y; y++) {
                    int color;
                    if (!useBaked) {
                        color = MaterialSource.fromCube(element.material, direction, element.getCenter(), x, y, dimensions.x, dimensions.y);
                        int r = ((color >> 16) & 0xFF);
                        int g = ((color >> 8) & 0xFF);
                        int b = (color & 0xFF);
                        int a = ((color >> 24) & 0xFF);

                        ElementRotation rotation = element.getRotation();
                        Vector3f pos = new Vector3f(ClientModelUtils.to3D(element, direction, x, y));
                        ModelUtils.applyElementRotation(pos, rotation);

                        Vector3f normal = ModelUtils.getElementRotation(rotation).transform(direction.step());

                        FurnitureData.LightMaterialEffect lightEffect = element.material.lightEffect;

                        // Apply HSV
                        float[] hsv = Utils.rgbToHsv(r / 255.0f, g / 255.0f, b / 255.0f);
                        hsv[0] = (hsv[0] + element.material.lightEffect.hue * 1.8f) % 360.0f;
                        hsv[1] = Math.max(0.0f, Math.min(1.0f, hsv[1] + element.material.lightEffect.saturation * 0.01f));
                        hsv[2] = Math.max(0.0f, Math.min(1.0f, hsv[2] + element.material.lightEffect.value * 0.01f));
                        float[] rgb = Utils.hsvToRgbRaw(hsv[0], hsv[1], hsv[2]);
                        r = (int) (rgb[0] * 255);
                        g = (int) (rgb[1] * 255);
                        b = (int) (rgb[2] * 255);

                        // Smooth light
                        float light = 1.0f;
                        float roundness = lightEffect.roundness / 75.0f;
                        if (roundness != 0.0f) {
                            light = getLight(x, y, dimensions);
                            light = quantize(x, y, light);
                            light = light * roundness + (1.0f - roundness * 0.5f);
                        }

                        // Brightness
                        light += lightEffect.brightness / 100.0f;

                        // Ambient Occlusion
                        float ao = Math.min(1.0f, Math.max(0.0f, 1.0f - this.ao.sample(pos, normal) * 1.5f));
                        light *= ao;

                        // Contrast
                        float contrast = lightEffect.contrast / 100.0f;
                        r = (int) Math.max(0.0, Math.min(255.0, ((r - 128) * (1.0f + contrast) + 128) * light));
                        g = (int) Math.max(0.0, Math.min(255.0, ((g - 128) * (1.0f + contrast) + 128) * light));
                        b = (int) Math.max(0.0, Math.min(255.0, ((b - 128) * (1.0f + contrast) + 128) * light));

                        color = (a << 24) | (r << 16) | (g << 8) | b;
                        baked[x + y * dimensions.x] = color;
                    } else {
                        color = baked[x + y * dimensions.x];
                    }
                    pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
                }
            }

            atlas.setDirty();

            if (!useBaked) {
                element.bakedTexture.put(direction, baked);
            }
        }

        float uvScale = 16.0f / atlas.size;
        return new BlockElementFace(
                getCulledDirection(vertices),
                -1,
                "0",
                new BlockFaceUV(
                        new float[]{
                                quad.x() * uvScale,
                                quad.y() * uvScale,
                                (quad.x() + quad.w()) * uvScale,
                                (quad.y() + quad.h()) * uvScale
                        },
                        0
                )
        );
    }

    private Direction getCulledDirection(Vector3f[] vertices) {
        if (vertices[0].x() == 0.0f && vertices[1].x() == 0.0f &&
            vertices[2].x() == 0.0f && vertices[3].x() == 0.0f) {
            return Direction.WEST;
        } else if (vertices[0].x() == 1.0f && vertices[1].x() == 1.0f &&
                   vertices[2].x() == 1.0f && vertices[3].x() == 1.0f) {
            return Direction.EAST;
        } else if (vertices[0].y() == 0.0f && vertices[1].y() == 0.0f &&
                   vertices[2].y() == 0.0f && vertices[3].y() == 0.0f) {
            return Direction.DOWN;
        } else if (vertices[0].y() == 1.0f && vertices[1].y() == 1.0f &&
                   vertices[2].y() == 1.0f && vertices[3].y() == 1.0f) {
            return Direction.UP;
        } else if (vertices[0].z() == 0.0f && vertices[1].z() == 0.0f &&
                   vertices[2].z() == 0.0f && vertices[3].z() == 0.0f) {
            return Direction.NORTH;
        } else if (vertices[0].z() == 1.0f && vertices[1].z() == 1.0f &&
                   vertices[2].z() == 1.0f && vertices[3].z() == 1.0f) {
            return Direction.SOUTH;
        } else {
            return null;
        }
    }

    private static boolean theSame(FurnitureData.Element element, FurnitureData.Element otherElement) {
        return element.from.equals(otherElement.from) &&
               element.to.equals(otherElement.to) &&
               element.getRotation().equals(otherElement.getRotation());
    }

    private static boolean fullyContained(FurnitureData.Element otherElement, Vector3f[] vertices) {
        for (Vector3f vertex : vertices) {
            Vector3f localVertex = new Vector3f(vertex);
            ModelUtils.applyInverseElementRotation(localVertex, otherElement.getRotation());
            if (!otherElement.contains(localVertex.mul(16.0f))) {
                return false;
            }
        }
        return true;
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
                getFaces(element),
                ClientModelUtils.toBlockElementRotation(element.getRotation()),
                true
        );
    }

    private Map<Direction, BlockElementFace> getFaces(FurnitureData.Element element) {
        if (element.type == FurnitureData.ElementType.SPRITE) {
            return Map.of(
                    Direction.NORTH, getSpriteFace(element),
                    Direction.SOUTH, getSpriteFace(element)
            );
        } else {
            return EnumSet.allOf(Direction.class).stream()
                    .map(dir -> Optional.ofNullable(getFace(element, dir))
                            .map(face -> Map.entry(dir, face)))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private static BlockElementFace getSpriteFace(FurnitureData.Element element) {
        return new BlockElementFace(
                null,
                element.color,
                element.sprite.sprite.toString(),
                new BlockFaceUV(
                        new float[]{0, 0, 16, 16},
                        0
                )
        );
    }

    private BlockModel getModel() {
        Map<String, Either<Material, String>> textures = new HashMap<>();
        textures.put("0", Either.left(new Material(InventoryMenu.BLOCK_ATLAS, Common.locate("block/furniture"))));
        data.elements.stream().filter(e -> e.type == FurnitureData.ElementType.SPRITE)
                .map(e -> e.sprite.sprite).distinct().forEach(source ->
                        textures.put(source.toString(), Either.left(new Material(InventoryMenu.BLOCK_ATLAS, source))));

        return new BlockModel(
                null,
                data.elements.stream().filter(e -> e.type == FurnitureData.ElementType.ELEMENT || e.type == FurnitureData.ElementType.SPRITE).map(this::getElement).toList(),
                textures,
                false,
                BlockModel.GuiLight.SIDE,
                getTransforms(),
                List.of()
        );
    }

    private ItemTransforms getTransforms() {
        float scale = (float) (1.0 / (Math.max(16.0, data.getSize()) / 16.0));
        return new ItemTransforms(
                new ItemTransform(new Vector3f(75, 225, 0), new Vector3f(0, 2.5f / 16.0f, 0), new Vector3f(0.375f * scale)),
                new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16.0f, 0), new Vector3f(0.375f * scale)),
                new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(), new Vector3f(0.4f * scale)),
                new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(), new Vector3f(0.4f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f)),
                new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(0.625f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(0, 3.0f / 16.0f, 0), new Vector3f(0.25f * scale)),
                new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(0.5f * scale))
        );
    }

    public static BlockModel getModel(FurnitureData data, DynamicAtlas atlas) {
        data.transparency = computeTransparency(data);
        return new FurnitureModelFactory(data, atlas).getModel();
    }

    private static TransparencyType computeTransparency(FurnitureData data) {
        TransparencyType transparencyType = TransparencyType.SOLID;
        for (FurnitureData.Element element : data.elements) {
            TransparencyType elementTransparencyType = null;
            if (element.type == FurnitureData.ElementType.SPRITE) {
                TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
                TextureAtlasSprite sprite = atlas.getSprite(element.sprite.sprite);
                elementTransparencyType = TransparencyManager.INSTANCE.getTransparencyType(sprite.contents());
            } else if (element.type == FurnitureData.ElementType.ELEMENT) {
                BlockState state = BuiltInRegistries.BLOCK.get(element.material.source).defaultBlockState();
                RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
                elementTransparencyType = fromRenderType(renderType);
            }
            if (elementTransparencyType != null && elementTransparencyType.isHigherPriorityThan(transparencyType)) {
                transparencyType = elementTransparencyType;
            }
        }
        return transparencyType;
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
}
