package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.client.DelayedFurnitureRenderer;
import net.minecraft.client.Minecraft;
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

    protected static CompositeBakedModel getBakedModel(BlockPos pos, BlockState state) {
        DelayedFurnitureRenderer.Status status = DelayedFurnitureRenderer.INSTANCE.getLoadedStatus(pos);

        // Render it
        if (status.data() != null) {
            int yRot = (int) state.getValue(BaseFurnitureBlock.FACING).getOpposite().toYRot();
            boolean active = state.getValue(BaseFurnitureBlock.ACTIVE);
            return FurnitureModelBaker.getModel(status.data(), DynamicAtlas.BAKED, yRot, active ? 1 : 0, false);
        } else if (!status.done()) {
            // Schedule a re-render
            DelayedFurnitureRenderer.INSTANCE.delayRendering(pos);
        }
        return null;
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
}
