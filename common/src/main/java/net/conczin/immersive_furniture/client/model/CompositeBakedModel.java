package net.conczin.immersive_furniture.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class CompositeBakedModel implements BakedModel {
    private final Map<RenderType, BakedModel> models;
    private final BakedModel any;
    private final List<BakedQuad> quads;

    public CompositeBakedModel(Map<RenderType, BakedModel> models) {
        this.models = models;
        this.any = models.values().stream().findAny().orElse(getMissing());

        this.quads = models.values().stream()
                .flatMap(model -> model.getQuads(null, null, RandomSource.create()).stream())
                .toList();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState blockState, Direction direction, RandomSource randomSource) {
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return any.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return any.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return any.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return any.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return any.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return any.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return any.getOverrides();
    }

    private BakedModel getMissing() {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
        return new SimpleBakedModel(
                List.of(), Map.of(), false, false, false, sprite, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY
        );
    }

    public Map<RenderType, BakedModel> getModels() {
        return models;
    }
}
