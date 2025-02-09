package immersive_furniture.client.model;

import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FurnitureBakedModel implements BakedModel {
    private static final Material MISSING_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation());
    public static final ResourceLocation LOCATION = Common.locate("block/furniture");

    static class ModelBakerImpl implements ModelBaker {
        ModelBakerImpl() {
        }

        @Override
        public UnbakedModel getModel(ResourceLocation location) {
            // Unsafe, but my models do not contain overrides or parents.
            return null;
        }

        @Override
        public BakedModel bake(ResourceLocation location, ModelState transform) {
            // Unsafe, but my models do not contain overrides or parents.
            return null;
        }
    }

    static ModelBakerImpl modelBaker = new ModelBakerImpl();

    public static BakedModel getModel() {
        // TODO: Cache
        //Integer value = state.getValue(FurnitureBlock.IDENTIFIER);
        FurnitureData data = FurnitureData.EMPTY;
        return FurnitureModelFactory.getModel(data, DynamicAtlas.ENTITY).bake(modelBaker, material -> {
            // return material.sprite(); // TODO: Here, use the full atlas sprite when in entity mode
            return DynamicAtlas.ENTITY.sprite;
        }, BlockModelRotation.by(0, 0), LOCATION);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return MISSING_TEXTURE.sprite();
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
