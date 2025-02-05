package immersive_furniture.data;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class FurnitureModel {
    private static BlockElementFace getEmptyFace() {
        return new BlockElementFace(
                null,
                -1,
                "#0",
                new BlockFaceUV(
                        new float[]{0, 0, 16, 16},
                        90
                )
        );
    }

    private static BlockElement getEmptyElement() {
        return new BlockElement(
                new Vector3f(0, 0, 0),
                new Vector3f(10, 10, 10),
                Map.of(
                        Direction.UP, getEmptyFace(),
                        Direction.DOWN, getEmptyFace(),
                        Direction.NORTH, getEmptyFace(),
                        Direction.SOUTH, getEmptyFace(),
                        Direction.WEST, getEmptyFace(),
                        Direction.EAST, getEmptyFace()
                ),
                new BlockElementRotation(
                        new Vector3f(0, 0, 0),
                        Direction.Axis.X,
                        0,
                        false
                ),
                true
        );
    }

    public static BlockModel getEmptyModel() {
        return new BlockModel(
                null,
                List.of(
                        getEmptyElement()
                ),
                Map.of(
                        "0", Either.left(new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/stone")))
                ),
                true,
                BlockModel.GuiLight.SIDE,
                ItemTransforms.NO_TRANSFORMS,
                List.of()
        );
    }
}
