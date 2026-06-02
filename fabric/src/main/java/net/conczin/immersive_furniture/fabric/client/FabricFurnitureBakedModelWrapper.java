package net.conczin.immersive_furniture.fabric.client;

import net.conczin.immersive_furniture.block.entity.FurnitureOffsetHolder;
import net.conczin.immersive_furniture.client.model.CompositeBakedModel;
import net.conczin.immersive_furniture.client.model.FurnitureBakedModelWrapper;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FabricFurnitureBakedModelWrapper extends FurnitureBakedModelWrapper implements FabricBakedModel {
    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        CompositeBakedModel model = getBakedModel(pos, state);
        if (model != null) {
            float dx = 0.0f;
            float dy = 0.0f;
            float dz = 0.0f;
            if (blockView != null) {
                BlockEntity blockEntity = blockView.getBlockEntity(pos);
                if (blockEntity instanceof FurnitureOffsetHolder holder) {
                    dx = holder.getSubOffsetX() / 16.0f - 0.5f;
                    dy = holder.getSubOffsetY() / 16.0f - 0.5f;
                    dz = holder.getSubOffsetZ() / 16.0f - 0.5f;
                }
            }
            for (Map.Entry<RenderType, BakedModel> entry : model.getModels().entrySet()) {
                emitBlockQuads(entry.getValue(), BlendMode.fromRenderLayer(entry.getKey()), state, randomSupplier, context, context.getEmitter(), dx, dy, dz);
            }
        }
    }

    public static void emitBlockQuads(BakedModel model, BlendMode blendMode, BlockState state, Supplier<RandomSource> randomSupplier, RenderContext context, QuadEmitter emitter, float dx, float dy, float dz) {
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
                BakedQuad q = (dx != 0.0f || dy != 0.0f || dz != 0.0f) ? translateQuad(quad, dx, dy, dz) : quad;
                emitter.fromVanilla(q, material, cullFace);
                emitter.emit();
            }
        }
    }

    private static BakedQuad translateQuad(BakedQuad quad, float dx, float dy, float dz) {
        int[] v = quad.getVertices().clone();
        for (int i = 0; i < v.length; i += 8) {
            float x = Float.intBitsToFloat(v[i]);
            float y = Float.intBitsToFloat(v[i + 1]);
            float z = Float.intBitsToFloat(v[i + 2]);
            v[i] = Float.floatToIntBits(x + dx);
            v[i + 1] = Float.floatToIntBits(y + dy);
            v[i + 2] = Float.floatToIntBits(z + dz);
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }
}
