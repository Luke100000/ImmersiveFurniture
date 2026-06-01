package net.conczin.immersive_furniture.forge.client;

import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.client.model.CompositeBakedModel;
import net.conczin.immersive_furniture.block.entity.FurnitureOffsetHolder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;
import java.util.Map;

public class ForgeFurnitureBakedModelWrapper extends FurnitureBakedModelWrapper implements IForgeBakedModel {
    public static final ModelProperty<CompositeBakedModel> PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Vec3> OFFSET_PROPERTY = new ModelProperty<>();

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        CompositeBakedModel model = getBakedModel(pos, state);
        ModelData.Builder builder = modelData.derive();
        if (model != null) {
            builder.with(PROPERTY, model);
        }
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FurnitureOffsetHolder holder) {
                float dx = holder.getSubOffsetX() / 16.0f - 0.5f;
                float dz = holder.getSubOffsetZ() / 16.0f - 0.5f;
                builder.with(OFFSET_PROPERTY, new Vec3(dx, 0.0D, dz));
            }
        }
        return builder.build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        CompositeBakedModel model = data.get(PROPERTY);
        if (model == null) return List.of();
        Map<RenderType, BakedModel> models = model.getModels();
        if (!models.containsKey(renderType)) return List.of();
        List<BakedQuad> quads = models.get(renderType).getQuads(state, side, rand, data, renderType);
        Vec3 offset = data.get(OFFSET_PROPERTY);
        if (offset != null) {
            double dx = offset.x;
            double dz = offset.z;
            if (dx != 0.0D || dz != 0.0D) {
                return quads.stream().map(q -> translateQuad(q, (float) dx, (float) dz)).toList();
            }
        }
        return quads;
    }

    private static BakedQuad translateQuad(BakedQuad quad, float dx, float dz) {
        int[] v = quad.getVertices().clone();
        for (int i = 0; i < v.length; i += 8) {
            float x = Float.intBitsToFloat(v[i]);
            float z = Float.intBitsToFloat(v[i + 2]);
            v[i] = Float.floatToIntBits(x + dx);
            v[i + 2] = Float.floatToIntBits(z + dz);
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
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
