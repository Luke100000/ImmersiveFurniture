package net.conczin.immersive_furniture.client.model;

import com.mojang.datafixers.util.Either;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.data.FurnitureData;
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
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

public class FurnitureModelFactory {
    private final boolean inEditor;

    private final FurnitureData data;
    private final Set<Integer> solidStates;
    private final Map<Integer, Map<FurnitureData.Element, Map<Direction, BlockElementFace>>> faces = new HashMap<>();
    private final List<FurnitureData.Element> elements = new LinkedList<>();
    private final FurnitureGeometryAnalyzer geometry;
    private final FurnitureFaceTextureBaker faceTextureBaker;
    private final Map<String, Either<Material, String>> textures = new HashMap<>();
    private final Map<FurnitureData.Element, Integer> elementToIndex = new HashMap<>();
    private final Map<Integer, FurnitureData.Element> indexToElement = new HashMap<>();

    private int zFightingCounter = 0;

    private FurnitureModelFactory(FurnitureData data, DynamicAtlas atlas) {
        this.inEditor = Minecraft.getInstance().screen instanceof ArtisansWorkstationEditorScreen;
        this.data = data;
        this.solidStates = data.getUniqueSolidStates();

        splitSprites();
        geometry = new FurnitureGeometryAnalyzer(elements);
        faceTextureBaker = new FurnitureFaceTextureBaker(atlas, inEditor, elements);

        // Fetch all textures
        textures.put("0", Either.left(new Material(InventoryMenu.BLOCK_ATLAS, Common.locate("block/furniture"))));
        for (FurnitureData.Element element : elements) {
            if (element.type == FurnitureData.ElementType.SPRITE) {
                textures.put(element.sprite.sprite.toString(), Either.left(new Material(InventoryMenu.BLOCK_ATLAS, element.sprite.sprite)));
            }
        }

        // Fetch all faces
        for (int state : solidStates) {
            Map<FurnitureData.Element, Map<Direction, BlockElementFace>> stateFaces = new HashMap<>();
            for (FurnitureData.Element element : elements) {
                if (FurnitureGeometryAnalyzer.hasFaces(element) && element.isMasked(state)) {
                    stateFaces.put(element, getFaces(element, state));
                }
            }
            faces.put(state, stateFaces);
        }
    }

    private BlockElementFace getFace(FurnitureData.Element element, Direction direction, int state) {
        if (geometry.isFaceFullyContained(element, direction, state)) return null;
        return faceTextureBaker.bake(
                element,
                direction,
                state,
                geometry.getCullDirection(element, direction),
                elementToIndex.get(element)
        );
    }

    private BlockElement getElement(FurnitureData.Element element, int state) {
        Vector3f from = new Vector3f(element.from);
        Vector3f to = new Vector3f(element.to);

        Map<FurnitureData.Element, Map<Direction, BlockElementFace>> stateFaces = faces.get(state);
        Map<Direction, BlockElementFace> elementFaces = stateFaces.getOrDefault(element, Collections.emptyMap());

        // If that element causes Z-fighting with another element, offset it slightly
        if (!inEditor && geometry.mightZFight(element, elementFaces, stateFaces)) {
            zFightingCounter++;
            float offset = 0.01f * (zFightingCounter % 7);
            from.sub(offset, offset, offset);
            to.add(offset, offset, offset);
        }

        BlockElementRotation rotation = ClientModelUtils.toBlockElementRotation(element.getRotation());
        return new BlockElement(from, to, elementFaces, rotation, true);
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
            Map<Direction, BlockElementFace> elementFaces = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                BlockElementFace face = getFace(element, direction, state);
                if (face != null) elementFaces.put(direction, face);
            }
            return elementFaces;
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
                        .filter(FurnitureGeometryAnalyzer::hasFaces)
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
        for (Integer state : solidStates) {
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
