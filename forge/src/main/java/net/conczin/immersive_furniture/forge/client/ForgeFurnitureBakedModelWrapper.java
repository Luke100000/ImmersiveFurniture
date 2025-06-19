package net.conczin.immersive_furniture.forge.client;

import net.conczin.immersive_furniture.client.FurnitureBakedModelWrapper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;

public class ForgeFurnitureBakedModelWrapper extends FurnitureBakedModelWrapper implements IForgeBakedModel {
    public static final ModelProperty<BakedModel> PROPERTY = new ModelProperty<>();
    public static final ModelProperty<RenderType> RENDER_TYPE = new ModelProperty<>();

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelAndRenderType model = getBakedModel(pos, state);
        if (model == null) return modelData;
        return modelData.derive()
                .with(PROPERTY, model.model())
                .with(RENDER_TYPE, model.renderType())
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        BakedModel model = data.get(PROPERTY);
        if (model == null) return List.of();
        return model.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BakedModel model = data.get(PROPERTY);
        if (model == null) {
            return ChunkRenderTypeSet.of(RenderType.solid());
        }
        return ChunkRenderTypeSet.of(data.get(RENDER_TYPE));
    }
}
