package immersive_furniture.block;

import com.mojang.blaze3d.vertex.PoseStack;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class FurnitureBlockEntityRenderer<T extends FurnitureBlockEntity> implements BlockEntityRenderer<T> {
    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // NO-OP
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FurnitureData data = blockEntity.getData();


    }
}
