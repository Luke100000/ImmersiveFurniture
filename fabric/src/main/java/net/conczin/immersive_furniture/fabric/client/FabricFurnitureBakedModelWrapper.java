package net.conczin.immersive_furniture.fabric.client;

import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
import net.conczin.immersive_furniture.client.model.MergedBakedModel;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FabricFurnitureBakedModelWrapper extends FurnitureBakedModelWrapper implements FabricBakedModel {
    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        MergedBakedModel model = getBakedModel(pos, state);
        if (model != null) {
            for (Map.Entry<RenderType, BakedModel> entry : model.getModels().entrySet()) {
                emitBlockQuads(entry.getValue(), BlendMode.fromRenderLayer(entry.getKey()), state, randomSupplier, context, context.getEmitter());
            }
        }
    }

    public static void emitBlockQuads(BakedModel model, BlendMode blendMode, BlockState state, Supplier<RandomSource> randomSupplier, RenderContext context, QuadEmitter emitter) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) return;
        final RenderMaterial material = renderer.materialFinder().blendMode(blendMode).find();

        for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
            final Direction cullFace = ModelHelper.faceFromIndex(i);

            if (!context.hasTransform() && context.isFaceCulled(cullFace)) {
                continue;
            }

            final List<BakedQuad> quads = model.getQuads(state, cullFace, randomSupplier.get());
            for (final BakedQuad quad : quads) {
                emitter.fromVanilla(quad, material, cullFace);
                emitter.emit();
            }
        }
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }
}
