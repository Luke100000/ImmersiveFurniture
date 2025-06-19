package net.conczin.immersive_furniture.client;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class FurnitureBakedModelWrapper implements BakedModel {
    public static BakedModel model = new FurnitureBakedModelWrapper();

    protected static ModelAndRenderType getBakedModel(BlockPos pos, BlockState state) {
        DelayedFurnitureRenderer.Status status = DelayedFurnitureRenderer.INSTANCE.getLoadedStatus(pos);

        // Render it
        if (status.data() != null) {
            int yRot = (int) state.getValue(BaseFurnitureBlock.FACING).getOpposite().toYRot();
            BakedModel model = FurnitureModelBaker.getModel(status.data(), DynamicAtlas.BAKED, yRot, false);
            if (model == null) return null;

            var renderType = getRenderType(status.data().transparency);
            return new ModelAndRenderType(model, renderType);
        } else if (!status.done()) {
            // Schedule a re-render
            DelayedFurnitureRenderer.INSTANCE.delayRendering(pos);
        }
        return null;
    }

    public static RenderType getRenderType(TransparencyType transparencyType) {
        return switch (transparencyType) {
            case SOLID -> RenderType.solid();
            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucent();
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
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
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("block/oak_planks"));
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    public record ModelAndRenderType(BakedModel model, RenderType renderType) {
    }
}
