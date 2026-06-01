package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.utils.SubBlockGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double x, double y,
            double z, float red, float green, float blue, float alpha) {
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$onRenderLevel(PoseStack poseStack, VertexConsumer consumer, Entity entity,
            double camX, double camY, double camZ, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (entity instanceof Player player && minecraft.hitResult instanceof BlockHitResult blockHitResult) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof FurnitureItem) {
                FurnitureData data = FurnitureItem.getData(stack);
                if (data == null)
                    return;

                Direction direction = player.getDirection().getOpposite();
                VoxelShape shape = data.getShapeLazy(direction);
                if (shape == null)
                    return;

                BlockPos clickedPos = new BlockPlaceContext(
                        new UseOnContext(player, InteractionHand.MAIN_HAND, blockHitResult)).getClickedPos();

                // Check if the player is sneaking (default Shift) for sub-block precision
                boolean precisionMode = player.isShiftKeyDown();

                double renderX = (double) clickedPos.getX() - camX;
                double renderY = (double) clickedPos.getY() - camY;
                double renderZ = (double) clickedPos.getZ() - camZ;

                if (precisionMode) {
                    // Get the exact hit location within the clicked block
                    Vec3 hitLocation = blockHitResult.getLocation();

                    // Calculate offset within the clicked block (0.0 to 1.0 range)
                    double blockX = hitLocation.x - clickedPos.getX();
                    double blockZ = hitLocation.z - clickedPos.getZ();

                    // Snap to the 16x16 grid within the block and recenter around block midpoint
                    double offsetX = SubBlockGrid.snapCoordinateToGrid(blockX) - 0.5;
                    double offsetZ = SubBlockGrid.snapCoordinateToGrid(blockZ) - 0.5;

                    // Clamp to ensure we stay within the block footprint
                    offsetX = Math.max(-0.5, Math.min(0.5, offsetX));
                    offsetZ = Math.max(-0.5, Math.min(0.5, offsetZ));

                    // Calculate the final world position with sub-block offset
                    double finalWorldX = clickedPos.getX() + offsetX;
                    double finalWorldZ = clickedPos.getZ() + offsetZ;

                    // Apply to render position (relative to camera)
                    renderX = finalWorldX - camX;
                    renderY = (double) clickedPos.getY() - camY; // Keep original Y position
                    renderZ = finalWorldZ - camZ;

                    // Render with a different color to indicate precision mode
                    renderShape(poseStack,
                            consumer,
                            shape,
                            renderX,
                            renderY,
                            renderZ,
                            0.0F,
                            1.0F, // Green tint for precision mode
                            0.0F,
                            0.4F // Slightly more opaque
                    );

                    immersiveFurniture$renderSubBlockGrid(
                            poseStack,
                            consumer,
                            clickedPos,
                            blockHitResult.getDirection().getOpposite(),
                            camX,
                            camY,
                            camZ);
                } else {
                    // Normal rendering
                    renderShape(poseStack,
                            consumer,
                            shape,
                            renderX,
                            renderY,
                            renderZ,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.4F);
                }

                ci.cancel();
            }
        }
    }

    // Renders a 16x16 placement grid over the targeted block face for precise
    // placement feedback.
    private static void immersiveFurniture$renderSubBlockGrid(PoseStack poseStack, VertexConsumer consumer,
            BlockPos blockPos, Direction face, double camX, double camY, double camZ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float red = 0.0F;
        float green = 1.0F;
        float blue = 0.0F;
        float alpha = 0.4F;
        float normalX = (float) face.getStepX();
        float normalY = (float) face.getStepY();
        float normalZ = (float) face.getStepZ();

        double step = SubBlockGrid.GRID_CELL_SIZE;
        double epsilon = 0.002;

        double baseX = blockPos.getX();
        double baseY = blockPos.getY();
        double baseZ = blockPos.getZ();

        int lines = SubBlockGrid.GRID_RESOLUTION;

        switch (face) {
            case UP -> {
                double y = baseY + 1.0 + epsilon;
                double minX = baseX;
                double maxX = baseX + 1.0;
                double minZ = baseZ;
                double maxZ = baseZ + 1.0;
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double z = minZ + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            minX, y, z, maxX, y, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double x = minX + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, y, minZ, x, y, maxZ,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
            }
            case DOWN -> {
                double y = baseY - epsilon;
                double minX = baseX;
                double maxX = baseX + 1.0;
                double minZ = baseZ;
                double maxZ = baseZ + 1.0;
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double z = minZ + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            minX, y, z, maxX, y, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double x = minX + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, y, minZ, x, y, maxZ,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
            }
            case NORTH -> {
                double z = baseZ - epsilon;
                double minX = baseX;
                double maxX = baseX + 1.0;
                double minY = baseY;
                double maxY = baseY + 1.0;
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double y = minY + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            minX, y, z, maxX, y, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double x = minX + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, minY, z, x, maxY, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
            }
            case SOUTH -> {
                double z = baseZ + 1.0 + epsilon;
                double minX = baseX;
                double maxX = baseX + 1.0;
                double minY = baseY;
                double maxY = baseY + 1.0;
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double y = minY + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            minX, y, z, maxX, y, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double x = minX + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, minY, z, x, maxY, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
            }
            case WEST -> {
                double x = baseX - epsilon;
                double minZ = baseZ;
                double maxZ = baseZ + 1.0;
                double minY = baseY;
                double maxY = baseY + 1.0;
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double y = minY + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, y, minZ, x, y, maxZ,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double z = minZ + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, minY, z, x, maxY, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
            }
            case EAST -> {
                double x = baseX + 1.0 + epsilon;
                double minZ = baseZ;
                double maxZ = baseZ + 1.0;
                double minY = baseY;
                double maxY = baseY + 1.0;
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double y = minY + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, y, minZ, x, y, maxZ,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
                for (int i = 0; i <= lines; i++) {
                    double offset = i * step;
                    double z = minZ + offset;
                    immersiveFurniture$emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                            x, minY, z, x, maxY, z,
                            red, green, blue, alpha, normalX, normalY, normalZ);
                }
            }
        }
    }

    private static void immersiveFurniture$emitGridLine(VertexConsumer consumer, Matrix4f poseMatrix,
            Matrix3f normalMatrix,
            double camX, double camY, double camZ,
            double startX, double startY, double startZ,
            double endX, double endY, double endZ,
            float red, float green, float blue, float alpha,
            float normalX, float normalY, float normalZ) {
        consumer.vertex(poseMatrix, (float) (startX - camX), (float) (startY - camY), (float) (startZ - camZ))
                .color(red, green, blue, alpha)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
        consumer.vertex(poseMatrix, (float) (endX - camX), (float) (endY - camY), (float) (endZ - camZ))
                .color(red, green, blue, alpha)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }
}