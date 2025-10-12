package net.conczin.immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.block.entity.FurnitureBlockEntity;
import net.conczin.immersive_furniture.client.model.CompositeBakedModel;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.SignalGetter.DIRECTIONS;

public class FurnitureBlockEntityRenderer<T extends FurnitureBlockEntity> implements BlockEntityRenderer<T> {
    private final ItemRenderer itemRenderer;

    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Common.entityRendersTotal++;

        FurnitureData data = blockEntity.getData();
        if (data == null) return;

        // Draw item slots
        BlockState blockState = blockEntity.getBlockState();
        int state = blockState.getValue(BaseFurnitureBlock.ACTIVE) ? 1 : 0;
        if (hasItems(blockEntity, data, state)) {
            poseStack.pushPose();
            rotate(blockState, poseStack);
            drawItems(itemRenderer, blockEntity.getLevel(), blockEntity, state, poseStack, buffer, packedLight, packedOverlay, data);
            poseStack.popPose();
        }

        // If the texture has been baked, we assume it got rendered via the block renderer
        if (DynamicAtlas.BAKED.knownFurniture.containsKey(data.getHash())) {
            return;
        }

        // Otherwise render it expensively
        Common.entityRenders++;
        poseStack.pushPose();
        rotate(blockState, poseStack);
        renderFurniture(blockState, poseStack, buffer, packedLight, packedOverlay, data);
        poseStack.popPose();
    }

    private static void rotate(BlockState blockState, PoseStack poseStack) {
        if (blockState.getBlock() instanceof BaseFurnitureBlock) {
            float yaw = -blockState.getValue(BaseFurnitureBlock.FACING).getOpposite().toYRot();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    public static boolean hasItems(FurnitureBlockEntity blockEntity, FurnitureData data, int state) {
        int slot = 0;
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.SPRITE && element.sprite.item && element.isMasked(state)) {
                ItemStack itemstack = blockEntity == null ? Items.APPLE.getDefaultInstance() : blockEntity.getItem(slot);
                if (!itemstack.isEmpty()) {
                    return true;
                }
                slot++;
            }
        }
        return false;
    }

    public static void drawItems(ItemRenderer itemRenderer, Level level, FurnitureBlockEntity blockEntity, int state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, FurnitureData data) {
        int slot = 0;
        for (FurnitureData.Element element : data.elements) {
            if (element.type == FurnitureData.ElementType.SPRITE && element.sprite.item && element.isMasked(state)) {
                ItemStack itemStack = blockEntity == null ? (
                        slot % 4 == 0 ? Items.APPLE.getDefaultInstance()
                                : slot % 4 == 1 ? Items.FURNACE.getDefaultInstance()
                                : slot % 4 == 2 ? Items.DIAMOND_PICKAXE.getDefaultInstance()
                                : Items.OAK_FENCE.getDefaultInstance()
                ) : blockEntity.getItem(slot);
                if (!itemStack.isEmpty()) {
                    Vector3f center = element.getCenter();
                    Quaternionf quaternion = ModelUtils.getElementRotation(element.getRotation());

                    poseStack.pushPose();
                    poseStack.translate(center.x / 16.0, center.y / 16.0, center.z / 16.0);
                    poseStack.mulPose(quaternion);
                    poseStack.scale(element.sprite.size, element.sprite.size, element.sprite.size);

                    // Align the item by its y min bound
                    BakedModel bakedmodel = itemRenderer.getModel(itemStack, level, null, slot);
                    AABB box = ModelBoundingBoxFetcher.INSTANCE.getModelBoundingBox(bakedmodel);
                    if (element.sprite.align) {
                        poseStack.translate(0.0, -0.5 - box.minY, 0);
                    } else {
                        poseStack.translate(0.0, 0, box.minZ);
                    }

                    itemRenderer.render(itemStack, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, packedOverlay, bakedmodel);
                    poseStack.popPose();
                }
                slot++;
            }
        }
    }

    public static void renderFurniture(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, FurnitureData data) {
        renderFurniture(state, poseStack, buffer, packedLight, packedOverlay, FurnitureModelBaker.getModel(data, DynamicAtlas.ENTITY), DynamicAtlas.ENTITY);
    }

    public static void renderFurniture(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, CompositeBakedModel bakedModel, DynamicAtlas atlas) {
        // Render in two passes since; unliked baked textures, the textures can be on up to two atlases
        for (int i = 0; i < 2; i++) {
            ResourceLocation location = i == 0 ? atlas.getLocation() : InventoryMenu.BLOCK_ATLAS;
            for (Map.Entry<RenderType, BakedModel> entry : bakedModel.getModels().entrySet()) {
                VertexConsumer consumer;
                if (entry.getKey() == RenderType.cutout() || entry.getKey() == RenderType.cutoutMipped()) {
                    consumer = buffer.getBuffer(RenderType.entityCutout(location));
                } else if (entry.getKey() == RenderType.translucent()) {
                    consumer = buffer.getBuffer(RenderType.entityTranslucentCull(location));
                } else {
                    consumer = buffer.getBuffer(RenderType.entitySolid(location));
                }
                renderModel(poseStack.last(), consumer, state, entry.getValue(), packedLight, packedOverlay, i == 1);
            }
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
            putBulkData(consumer, pose, quad, packedLight, packedOverlay);
        }
    }

    static void putBulkData(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, int packedLight, int packedOverlay) {
        int[] vertices = quad.getVertices();
        Vec3i quadNormal = quad.getDirection().getNormal();
        Matrix4f matrix4f = pose.pose();
        Vector3f normal = pose.normal().transform(new Vector3f((float) quadNormal.getX(), (float) quadNormal.getY(), (float) quadNormal.getZ()));
        int vertexCount = vertices.length / 8;

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int i = 0; i < vertexCount; ++i) {
                intbuffer.clear();
                intbuffer.put(vertices, i * 8, 8);

                float x = bytebuffer.getFloat(0);
                float y = bytebuffer.getFloat(4);
                float z = bytebuffer.getFloat(8);
                Vector4f pos = matrix4f.transform(new Vector4f(x, y, z, 1.0f));

                float red = (float) (bytebuffer.get(12) & 255) / 255.0F;
                float green = (float) (bytebuffer.get(13) & 255) / 255.0F;
                float blue = (float) (bytebuffer.get(14) & 255) / 255.0F;

                float u = bytebuffer.getFloat(16);
                float v = bytebuffer.getFloat(20);

                int light = blend(packedLight, quad.getVertices()[i * 8 + 6]);

                consumer.vertex(pos.x(), pos.y(), pos.z(), red, green, blue, 1.0f, u, v, packedOverlay, light, normal.x(), normal.y(), normal.z());
            }
        }
    }

    private static int blend(int worldLight, int vertexLight) {
        return Math.max(worldLight & 0xFFFF, vertexLight & 0xFFFF) | (Math.max((worldLight >> 16) & 0xFFFF, (vertexLight >> 16) & 0xFFFF) << 16);
    }
}
