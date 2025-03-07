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
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class FurnitureModelFactory {
    private static final Map<@NotNull String, @NotNull Either<Material, String>> TEXTURE_MAP = Map.of(
            "0", Either.left(new Material(InventoryMenu.BLOCK_ATLAS, Common.locate("block/furniture")))
    );

    private static final ItemTransforms TRANSFORMS = new ItemTransforms(
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f)),
            new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f))
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

        // Render
        NativeImage pixels = atlas.getPixels();
        assert pixels != null;
        for (int x = 0; x < dimensions.x; x++) {
            for (int y = 0; y < dimensions.y; y++) {
                int color = MaterialSource.fromCube(element.material, direction, x, y, dimensions.x, dimensions.y);
                pixels.setPixelRGBA(quad.x() + x, quad.y() + y, color);
            }
        }

        atlas.upload();

        return new BlockElementFace(
                null,
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

    private BlockElement getEmptyElement(FurnitureData.Element element) {
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
                data.elements.stream().map(this::getEmptyElement).toList(),
                TEXTURE_MAP,
                true,
                BlockModel.GuiLight.SIDE,
                TRANSFORMS,
                List.of()
        );
    }

    public static BlockModel getModel(FurnitureData data, DynamicAtlas atlas) {
        atlas.clear();
        return new FurnitureModelFactory(data, atlas).getModel();
    }
}
