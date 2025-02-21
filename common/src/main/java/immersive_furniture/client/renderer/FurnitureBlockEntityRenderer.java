package immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import immersive_furniture.block.FurnitureBlockEntity;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.client.model.FurnitureModelBaker;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class FurnitureBlockEntityRenderer<T extends FurnitureBlockEntity> implements BlockEntityRenderer<T> {
    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // NO-OP
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FurnitureData data = blockEntity.getData();
        boolean translucent = true;

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BlockState state = blockEntity.getBlockState();
        BakedModel bakedModel = FurnitureModelBaker.getModel(data);
        // VertexConsumer consumer = buffer.getBuffer(translucent ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet());
        ResourceLocation location = DynamicAtlas.ENTITY.getLocation();
        VertexConsumer consumer = buffer.getBuffer(translucent ? RenderType.entityTranslucent(location) : RenderType.entityCutout(location));
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), consumer, state, bakedModel, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
    }
}
