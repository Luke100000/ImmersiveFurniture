package net.conczin.immersive_furniture.neoforge.client;

import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.client.model.CompositeBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.List;
import java.util.Map;

public class NeoForgeFurnitureBakedModelWrapper extends FurnitureBakedModelWrapper implements IBakedModelExtension {
    public static final ModelProperty<CompositeBakedModel> PROPERTY = new ModelProperty<>();

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        CompositeBakedModel model = getBakedModel(pos, state);
        if (model == null) return modelData;
        return modelData.derive()
                .with(PROPERTY, model)
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        CompositeBakedModel model = data.get(PROPERTY);
        if (model == null) return List.of();
        Map<RenderType, BakedModel> models = model.getModels();
        if (!models.containsKey(renderType)) return List.of();
        return models.get(renderType).getQuads(state, side, rand, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        CompositeBakedModel model = data.get(PROPERTY);
        if (model == null) {
            return ChunkRenderTypeSet.of(RenderType.solid());
        }
        return ChunkRenderTypeSet.of(model.getModels().keySet());
    }
}
