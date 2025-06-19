package net.conczin.immersive_furniture.fabric.client;

import net.conczin.immersive_furniture.client.FurnitureBakedModelWrapper;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class FabricFurnitureBakedModelWrapper extends FurnitureBakedModelWrapper implements FabricBakedModel {
    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        ModelAndRenderType model = getBakedModel(pos, state);
        if (model != null) {
            VanillaModelEncoder.emitBlockQuads(model.model(), state, randomSupplier, context, context.getEmitter());
        }
    }
}
