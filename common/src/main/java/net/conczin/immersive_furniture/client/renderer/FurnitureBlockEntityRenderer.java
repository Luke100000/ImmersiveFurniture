package net.conczin.immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.block.FurnitureBlockEntity;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class FurnitureBlockEntityRenderer<T extends FurnitureBlockEntity> implements BlockEntityRenderer<T> {
    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ignoredContext) {
        // NO-OP
    }

    public static void renderItem(ItemStack itemStack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FurnitureData data = FurnitureItem.getData(itemStack);
        renderFurniture(null, poseStack, buffer, packedLight, packedOverlay, data);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FurnitureData data = blockEntity.getData();
        if (data == null) return;

        // If the texture has been baked, we assume it got rendered via the block renderer
        if (DynamicAtlas.BAKED.knownFurniture.containsKey(data.getHash())) {
            return;
        }

        poseStack.pushPose();

        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof BaseFurnitureBlock) {
            float yaw = -blockState.getValue(BaseFurnitureBlock.FACING).getOpposite().toYRot();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        BlockState state = blockEntity.getBlockState();
        renderFurniture(state, poseStack, buffer, packedLight, packedOverlay, data);

        poseStack.popPose();
    }

    private static void renderFurniture(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, FurnitureData data) {
        boolean translucent = data.isTranslucent();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel bakedModel = FurnitureModelBaker.getModel(data, DynamicAtlas.ENTITY);
        ResourceLocation location = DynamicAtlas.ENTITY.getLocation();
        VertexConsumer consumer = buffer.getBuffer(translucent ? RenderType.entityTranslucent(location) : RenderType.entityCutout(location));
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), consumer, state, bakedModel, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
    }
}