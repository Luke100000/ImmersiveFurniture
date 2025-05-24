package immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
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

                        Vector3f pos = new Vector3f(ModelUtils.to3D(element, direction, x, y));
                        ModelUtils.applyElementRotation(pos.mul(1.0f / 16.0f), element.getRotation());
                        pos.mul(16.0f);
                        float ao = Math.min(1.0f, Math.max(0.0f, 2.0f - AmbientOcclusion.INSTANCE.getValue(pos) * 2.0f));
                        r = (int) (r * ao);
                        g = (int) (g * ao);
                        b = (int) (b * ao);

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
        // Render AO lookup
        AmbientOcclusion ao = AmbientOcclusion.INSTANCE;
        ao.clear();
        for (FurnitureData.Element element : data.elements) {
            Quaternionf rotation = ModelUtils.getElementRotation(element.getRotation());
            ao.place(element.getSize(), element.getCenter(), rotation);
        }

        atlas.clear(); // TODO
        return new FurnitureModelFactory(data, atlas).getModel();
    }
}
