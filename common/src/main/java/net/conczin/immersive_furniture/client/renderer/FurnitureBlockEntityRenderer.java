package net.conczin.immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.block.entity.FurnitureBlockEntity;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.minecraft.world.level.SignalGetter.DIRECTIONS;

public class FurnitureBlockEntityRenderer<T extends FurnitureBlockEntity> implements BlockEntityRenderer<T> {
    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context ignoredContext) {
        // NO-OP
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Common.entityRendersTotal++;

        FurnitureData data = blockEntity.getData();
        if (data == null) return;

        // If the texture has been baked, we assume it got rendered via the block renderer
        if (DynamicAtlas.BAKED.knownFurniture.containsKey(data.getHash())) {
            return;
        }

        Common.entityRenders++;

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

    public static void renderFurniture(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, FurnitureData data) {
        renderFurniture(state, poseStack, buffer, packedLight, packedOverlay, data, FurnitureModelBaker.getModel(data, DynamicAtlas.ENTITY), DynamicAtlas.ENTITY);
    }

    public static void renderFurniture(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, FurnitureData data, BakedModel bakedModel, DynamicAtlas atlas) {
        // Render in two passes since, unliked baked textures, the textures can be on up to two atlases
        for (int i = 0; i < 2; i++) {
            ResourceLocation location = i == 0 ? atlas.getLocation() : InventoryMenu.BLOCK_ATLAS;

            // Use the transparency type to determine the render type
            VertexConsumer consumer = switch (data.transparency) {
                case TRANSLUCENT -> buffer.getBuffer(RenderType.entityTranslucentCull(location));
                case CUTOUT, CUTOUT_MIPPED -> buffer.getBuffer(RenderType.entityCutout(location));
                default -> buffer.getBuffer(RenderType.entitySolid(location));
            };

            renderModel(poseStack.last(), consumer, state, bakedModel, packedLight, packedOverlay, i == 1);
        }
    }

    private static final RandomSource randomsource = RandomSource.create();

    private static void renderModel(PoseStack.Pose pose, VertexConsumer consumer, BlockState state, BakedModel model, int packedLight, int packedOverlay, boolean blocksAtlas) {
        for (Direction direction : DIRECTIONS) {
            renderQuadList(pose, consumer, model.getQuads(state, direction, randomsource), packedLight, packedOverlay, blocksAtlas);
        }
        renderQuadList(pose, consumer, model.getQuads(state, null, randomsource), packedLight, packedOverlay, blocksAtlas);
    }

    private static void renderQuadList(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int packedLight, int packedOverlay, boolean blocksAtlas) {
        for (BakedQuad quad : quads) {
            ResourceLocation resourceLocation = quad.getSprite().atlasLocation();
            if (resourceLocation.getNamespace().equals("minecraft") != blocksAtlas) continue;
            consumer.putBulkData(pose, quad, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, 1.0f, 1.0f, 1.0f, new int[]{packedLight, packedLight, packedLight, packedLight}, packedOverlay, true);
        }
    }
}
