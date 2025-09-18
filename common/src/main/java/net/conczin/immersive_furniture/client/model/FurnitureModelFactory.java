package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.Utils;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Quaternionf;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.stream.Collectors;

public class FurnitureModelFactory {
    private final boolean inEditor;

    private final FurnitureData data;
    private final DynamicAtlas atlas;
    private final Map<Integer, AmbientOcclusion> aos;
    private int zFightingCounter = 0;

    private final Map<Integer, Map<FurnitureData.Element, Map<Direction, BlockElementFace>>> faces = new HashMap<>();
    private final List<FurnitureData.Element> elements = new LinkedList<>();
    private final Map<String, Either<Material, String>> textures = new HashMap<>();
    private final Map<FurnitureData.Element, Integer> elementToIndex = new HashMap<>();
    private final Map<Integer, FurnitureData.Element> indexToElement = new HashMap<>();

    private FurnitureModelFactory(FurnitureData data, DynamicAtlas atlas) {
        this.inEditor = Minecraft.getInstance().screen instanceof ArtisansWorkstationEditorScreen;
        this.data = data;
        this.atlas = atlas;

        splitSprites();

        // Populate AO lookup
        aos = new HashMap<>();
        for (int state : data.getUniqueSolidStates()) {
            AmbientOcclusion ao = new AmbientOcclusion();
            for (FurnitureData.Element element : elements) {
                if (element.type == FurnitureData.ElementType.ELEMENT && element.isMasked(state)) {
                    ao.place(element, element.material.transparency == TransparencyType.SOLID ? 1.0f : 0.25f);
                }
            }
            aos.put(state, ao);
        }

        // Fetch all textures
        textures.put("0", Either.left(new Material(InventoryMenu.BLOCK_ATLAS, Common.locate("block/furniture"))));
        elements.stream().filter(e -> e.type == FurnitureData.ElementType.SPRITE)
                .map(e -> e.sprite.sprite).distinct().forEach(source ->
                        textures.put(source.toString(), Either.left(new Material(InventoryMenu.BLOCK_ATLAS, source))));

        // Fetch all faces
        for (int state : data.getUniqueSolidStates()) {
            faces.put(state, elements.stream()
                    .filter(FurnitureModelFactory::hasFaces)
                    .filter(e -> e.isMasked(state))
                    .collect(Collectors.toMap(e -> e, e -> getFaces(e, state))));
        }
    }

    float mod(float a, float b) {
        return (a % b + b) % b;
    }

    private BlockElementFace getFace(FurnitureData.Element element, Direction direction, int state) {
        // Cull fully invisible faces
        float[] fs = ClientModelUtils.getShapeData(element);
        Vector3f[] vertices = ClientModelUtils.getVertices(element, direction, fs, null);
        for (FurnitureData.Element otherElement : elements) {
            if (otherElement == element) continue;
            if (!otherElement.isMasked(state)) continue;
            if (otherElement.material.transparency != TransparencyType.SOLID) continue;
            if (otherElement.type != FurnitureData.ElementType.ELEMENT) continue;
            if (theSame(element, otherElement) && otherElement.hashCode() < element.hashCode()) continue;
            if (fullyContained(otherElement, vertices)) return null;
        }

        // Allocate pixels
        Vector2i dimensions = ClientModelUtils.getFaceDimensions(element, direction);

        // Padding
        int level = 1;
        if (atlas == DynamicAtlas.BAKED) {
            int i = Minecraft.getInstance().options.mipmapLevels().get();
            int panic = atlas.getUsage() > 0.5 ? (atlas.getUsage() > 0.75 ? 2 : 1) : 0;
            level = (int) Math.pow(2, Math.max(0, Math.min(i - panic, Config.getInstance().maxMipLevel)));
        }
        DynamicAtlas.Quad quad = atlas.allocate(
                (int) (Math.ceil(dimensions.x / (double) level) * level),
                (int) (Math.ceil(dimensions.y / (double) level) * level)
        );

        if (quad.w() > 0 && quad.h() > 0) {
            // Render
            NativeImage pixels = atlas.getPixels();
            assert pixels != null;

            // Use baked texture if available
            boolean useBaked = true;
            int[] baked = inEditor ? null : element.bakedTextures.get(direction, state);
            if (baked == null || baked.length != dimensions.x * dimensions.y) {
                baked = new int[dimensions.x * dimensions.y];
                useBaked = false;
            }

            for (int x = 0; x < dimensions.x; x++) {
                for (int y = 0; y < dimensions.y; y++) {
                    int color;
                    if (!useBaked) {
                        color = MaterialSource.fromCube(element.material, direction, element.getCenter(), x, y, dimensions.x, dimensions.y);
                        int r = color >> 16 & 0xFF;
                        int g = color >> 8 & 0xFF;
                        int b = color & 0xFF;
                        int a = color >> 24 & 0xFF;

                        ElementRotation rotation = element.getRotation();
                        Vector3f pos = new Vector3f(ClientModelUtils.to3D(element, direction, x, y));
                        ModelUtils.applyElementRotation(pos, rotation);

                        Vector3f normal = ModelUtils.getElementRotation(rotation).transform(direction.step());

                        FurnitureData.LightMaterialEffect lightEffect = element.material.lightEffect;

                        // Apply HSV
                        float[] hsv = Utils.rgbToHsv(r / 255.0f, g / 255.0f, b / 255.0f);
                        if (Math.abs(element.material.lightEffect.hue) >= 1.0f) {
                            hsv[0] = mod(element.material.lightEffect.hue * 1.8f, 360f);
                        }
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
                        float ao = Math.min(1.0f, Math.max(0.0f, 1.0f - this.aos.get(state).sample(pos, normal) * 1.5f));
                        float emission = element.emission / 15.0f;
                        light *= (ao * (1.0f - emission) + emission);

                        // Contrast
                        float contrast = lightEffect.contrast / 100.0f;
                        r = (int) Math.max(0.0, Math.min(255.0, ((r - 128) * (1.0f + contrast) + 128) * light));
                        g = (int) Math.max(0.0, Math.min(255.0, ((g - 128) * (1.0f + contrast) + 128) * light));
                        b = (int) Math.max(0.0, Math.min(255.0, ((b - 128) * (1.0f + contrast) + 128) * light));

                        color = a << 24 | r << 16 | g << 8 | b;
                        baked[x + y * dimensions.x] = color;
                    } else {
                        color = baked[x + y * dimensions.x];
                    }
                    pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
                }
            }

            // Padding
            for (int x = dimensions.x(); x < quad.w(); x++) {
                for (int y = 0; y < dimensions.y(); y++) {
                    int color = pixels.getPixelRGBA(quad.x() + x - 1, quad.y() + y);
                    pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
                }
            }
            for (int y = dimensions.y(); y < quad.h(); y++) {
                for (int x = 0; x < quad.w(); x++) {
                    int color = pixels.getPixelRGBA(quad.x() + x, quad.y() + y - 1);
                    pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
                }
            }

            atlas.setDirty();

            // Save baked texture
            if (inEditor) {
                element.bakedTextures.put(direction, state, baked);
            }
        }

        float uvScale = 16.0f / atlas.size;
        return new BlockElementFace(
                getCulledDirection(vertices),
                elementToIndex.get(element),
                "0",
                new BlockFaceUV(
                        new float[]{
                                quad.x() * uvScale,
                                quad.y() * uvScale,
                                (quad.x() + dimensions.x()) * uvScale,
                                (quad.y() + dimensions.y()) * uvScale
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

    private boolean mightZFight(FurnitureData.Element element, Map<Direction, BlockElementFace> faces, int state) {
        for (Direction direction : faces.keySet()) {
            float[] fs = ClientModelUtils.getShapeData(element);
            Vector3f[] vertices = ClientModelUtils.getVertices(element, direction, fs, null);

            for (FurnitureData.Element otherElement : elements) {
                if (otherElement == element) continue;
                if (otherElement.getVolume() < element.getVolume()) continue;
                if (!hasFaces(otherElement)) continue;

                // Convert to another element's local space
                Vector3f[] localVerts = new Vector3f[vertices.length];
                for (int i = 0; i < vertices.length; i++) {
                    Vector3f v = vertices[i];
                    Vector3f lv = new Vector3f(v);
                    ModelUtils.applyInverseElementRotation(lv, otherElement.getRotation());
                    lv.mul(16.0f);
                    localVerts[i] = lv;
                }

                Map<Direction, BlockElementFace> otherFaces = this.faces.get(state).getOrDefault(otherElement, Collections.emptyMap());

                float fromX = Math.min(localVerts[0].x, localVerts[2].x);
                float toX = Math.max(localVerts[0].x, localVerts[2].x);
                float fromY = Math.min(localVerts[0].y, localVerts[2].y);
                float toY = Math.max(localVerts[0].y, localVerts[2].y);
                float fromZ = Math.min(localVerts[0].z, localVerts[2].z);
                float toZ = Math.max(localVerts[0].z, localVerts[2].z);

                float m = 0.01f;

                if (Math.abs(fromX - toX) < m && fromY < otherElement.to.y - m && toY > otherElement.from.y + m && fromZ < otherElement.to.z - m && toZ > otherElement.from.z + m) {
                    if (otherFaces.containsKey(Direction.WEST) && Math.abs(fromX - otherElement.from.x) < m)
                        return true;
                    if (otherFaces.containsKey(Direction.EAST) && Math.abs(toX - otherElement.to.x) < m)
                        return true;
                }

                if (Math.abs(fromY - toY) < m && fromX < otherElement.to.x - m && toX > otherElement.from.x + m && fromZ < otherElement.to.z - m && toZ > otherElement.from.z + m) {
                    if (otherFaces.containsKey(Direction.DOWN) && Math.abs(fromY - otherElement.from.y) < m)
                        return true;
                    if (otherFaces.containsKey(Direction.UP) && Math.abs(toY - otherElement.to.y) < m)
                        return true;
                }

                if (Math.abs(fromZ - toZ) < m && fromX < otherElement.to.x - m && toX > otherElement.from.x + m && fromY < otherElement.to.y - m && toY > otherElement.from.y + m) {
                    if (otherFaces.containsKey(Direction.NORTH) && Math.abs(fromZ - otherElement.from.z) < m)
                        return true;
                    if (otherFaces.containsKey(Direction.SOUTH) && Math.abs(toZ - otherElement.to.z) < m)
                        return true;
                }
            }
        }
        return false;
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
        light = (float) Math.round(light * levels + n) / levels;
        return light;
    }

    private BlockElement getElement(FurnitureData.Element element, int state) {
        Vector3f from = new Vector3f(element.from);
        Vector3f to = new Vector3f(element.to);

        Map<Direction, BlockElementFace> faces = this.faces.get(state).getOrDefault(element, Collections.emptyMap());

        // If that element causes Z-fighting with another element, offset it slightly
        if (!inEditor && mightZFight(element, faces, state)) {
            zFightingCounter++;
            float offset = 0.01f * (zFightingCounter % 7);
            from.sub(offset, offset, offset);
            to.add(offset, offset, offset);
        }

        BlockElementRotation rotation = ClientModelUtils.toBlockElementRotation(element.getRotation());
        return new BlockElement(from, to, faces, rotation, true);
    }

    private Map<Direction, BlockElementFace> getFaces(FurnitureData.Element element, int state) {
        if (element.type == FurnitureData.ElementType.SPRITE) {
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            TextureAtlasSprite sprite = atlas.getSprite(element.sprite.sprite);
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                return Map.of();
            }
            return Map.of(
                    Direction.NORTH, getSpriteFace(element, true),
                    Direction.SOUTH, getSpriteFace(element, false)
            );
        } else {
            return EnumSet.allOf(Direction.class).stream()
                    .map(dir -> Optional.ofNullable(getFace(element, dir, state))
                            .map(face -> Map.entry(dir, face)))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private BlockElementFace getSpriteFace(FurnitureData.Element element, boolean front) {
        Vector3i size = element.getSize();
        return new BlockElementFace(
                null,
                elementToIndex.get(element),
                element.sprite.sprite.toString(),
                new BlockFaceUV(
                        new float[]{
                                front ? 0 : size.x / element.sprite.size,
                                0,
                                front ? size.x / element.sprite.size : 0,
                                size.y / element.sprite.size
                        },
                        element.sprite.rotation
                )
        );
    }

    /**
     * Returns a block model for a specific render type
     */
    private BlockModel getModel(TransparencyType type, int state) {
        return new BlockModel(
                null,
                elements.stream()
                        .filter(FurnitureModelFactory::hasFaces)
                        .filter(e -> e.isMasked(state))
                        .filter(e -> type == null || getTransparencyType(e) == type)
                        .map(e -> getElement(e, state))
                        .toList(),
                textures,
                false,
                BlockModel.GuiLight.SIDE,
                getTransforms(),
                List.of()
        );
    }

    /**
     * Returns a block model for every state for a specific render type
     */
    private Map<Integer, BlockModel> getModels(TransparencyType type) {
        Map<Integer, BlockModel> models = new HashMap<>();
        for (Integer state : data.getUniqueSolidStates()) {
            models.put(state, getModel(type, state));
        }
        return models;
    }

    private void splitSprites() {
        int index = 0;
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.SPRITE && element.sprite.tiled) {
                Vector3i size = element.getSize();
                int px = Math.round(16 * element.sprite.size);
                for (int x = 0; x < size.x; x += px) {
                    for (int y = 0; y < size.y; y += px) {
                        FurnitureData.Element tiledElement = new FurnitureData.Element(element);

                        // Resize to chunk
                        float w = Math.min(size.x - x, px);
                        float h = Math.min(size.y - y, px);
                        tiledElement.from.add(x, y, 0);
                        tiledElement.to = new Vector3f(tiledElement.from).add(w, h, 0);

                        // Readjust position
                        Vector3f east = element.getGlobalDirectionNormal(Direction.EAST);
                        Vector3f up = element.getGlobalDirectionNormal(Direction.DOWN);
                        float hx = x + w / 2.0f - size.x / 2.0f;
                        float hy = y + h / 2.0f - size.y / 2.0f;
                        Vector3f offset = new Vector3f(
                                east.x * hx + up.x * hy,
                                east.y * hx + up.y * hy,
                                east.z * hx + up.z * hy
                        );
                        offset.add(element.getCenter().sub(tiledElement.getCenter()));
                        tiledElement.from.add(offset);
                        tiledElement.to.add(offset);

                        elements.add(tiledElement);
                        elementToIndex.put(tiledElement, index);
                    }
                }
            } else if (element.type != FurnitureData.ElementType.SPRITE || !element.sprite.item) {
                elements.add(element);
                elementToIndex.put(element, index);
            }
            indexToElement.put(index, element);
            index++;
        }
    }

    private TransparencyType getTransparencyType(FurnitureData.Element e) {
        return e.type == FurnitureData.ElementType.SPRITE ? TransparencyManager.fromSprite(e) : e.material.transparency;
    }

    private static boolean hasFaces(FurnitureData.Element e) {
        return e.type == FurnitureData.ElementType.ELEMENT || e.type == FurnitureData.ElementType.SPRITE;
    }

    private ItemTransforms getTransforms() {
        float scale = (float) (1.0 / (Math.max(16.0, data.getSize()) / 16.0));
        float sqrtScale = (float) Math.sqrt(scale);
        Vector3f o = new Vector3f(0.5f - data.size.x / 2.0f, 0.5f - data.size.y / 2.0f, 0.5f - data.size.z / 2.0f);
        return new ItemTransforms(
                getItemTransform(o, 75, 225, 0, 0, 2.5f, 0, 0.375f * sqrtScale),
                getItemTransform(o, 75, 45, 0, 0, 2.5f, 0, 0.375f * sqrtScale),
                getItemTransform(o, 0, 225, 0, 0, 0, 0, 0.4f * sqrtScale),
                getItemTransform(o, 0, 45, 0, 0, 0, 0, 0.4f * sqrtScale),
                getItemTransform(o, 0, 0, 0, 0, 0, 0, 1.0f),
                getItemTransform(o, 30, 225, 0, 0, 0, 0, 0.625f * scale),
                getItemTransform(o, 0, 0, 0, 0, 3.0f, 0, 0.25f * sqrtScale),
                getItemTransform(o, 0, 0, 0, 0, 0, 0, 0.5f * scale)
        );
    }

    private ItemTransform getItemTransform(Vector3f offset, float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {
        Vector3f offsetScaled = new Vector3f(offset).mul(scale);
        Quaternionf rotation = new Quaternionf()
                .rotateXYZ(
                        (float) Math.toRadians(rotX),
                        (float) Math.toRadians(rotY),
                        (float) Math.toRadians(rotZ)
                );
        offsetScaled.rotate(rotation);
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                offsetScaled.add(x / 16.0f, y / 16.0f, z / 16.0f),
                new Vector3f(scale)
        );
    }


    /**
     * Create a furniture model and its texture.
     * This function is somewhat slow, call async whenever possible
     */
    public static CompositeBlockModel getModel(FurnitureData data, DynamicAtlas atlas) {
        FurnitureModelFactory factory = new FurnitureModelFactory(data, atlas);

        // Create a model for each transparency type
        CompositeBlockModel composite = new CompositeBlockModel(factory.indexToElement);
        composite.addModel(RenderType.solid(), factory.getModels(TransparencyType.SOLID));
        composite.addModel(RenderType.cutout(), factory.getModels(TransparencyType.CUTOUT));
        composite.addModel(RenderType.cutoutMipped(), factory.getModels(TransparencyType.CUTOUT_MIPPED));
        composite.addModel(RenderType.translucent(), factory.getModels(TransparencyType.TRANSLUCENT));
        return composite;
    }
}
