package net.conczin.immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.utils.SubBlockGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class FurniturePlacementPreviewRenderer {
    private FurniturePlacementPreviewRenderer() {
    }

    public static boolean render(PoseStack poseStack, VertexConsumer consumer, Entity entity, BlockHitResult blockHitResult,
                                 double camX, double camY, double camZ, ShapeRenderer shapeRenderer) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof FurnitureItem)) {
            return false;
        }

        FurnitureData data = FurnitureItem.getData(stack);
        if (data == null) {
            return false;
        }

        Direction direction = player.getDirection().getOpposite();
        VoxelShape shape = data.getShapeLazy(direction);
        if (shape == null) {
            return false;
        }

        BlockPos clickedPos = new BlockPlaceContext(
                new UseOnContext(player, InteractionHand.MAIN_HAND, blockHitResult)).getClickedPos();

        double renderX = (double) clickedPos.getX() - camX;
        double renderY = (double) clickedPos.getY() - camY;
        double renderZ = (double) clickedPos.getZ() - camZ;

        if (player.isShiftKeyDown()) {
            Vec3 hitLocation = blockHitResult.getLocation();
            double blockX = hitLocation.x - clickedPos.getX();
            double blockZ = hitLocation.z - clickedPos.getZ();
            double offsetX = SubBlockGrid.snapCoordinateToGrid(blockX) - 0.5;
            double offsetZ = SubBlockGrid.snapCoordinateToGrid(blockZ) - 0.5;

            offsetX = Math.max(-0.5, Math.min(0.5, offsetX));
            offsetZ = Math.max(-0.5, Math.min(0.5, offsetZ));

            renderX = clickedPos.getX() + offsetX - camX;
            renderZ = clickedPos.getZ() + offsetZ - camZ;

            shapeRenderer.render(poseStack, consumer, shape, renderX, renderY, renderZ, 0.0F, 1.0F, 0.0F, 0.4F);
            renderSubBlockGrid(poseStack, consumer, clickedPos, blockHitResult.getDirection().getOpposite(), camX, camY, camZ);
        } else {
            shapeRenderer.render(poseStack, consumer, shape, renderX, renderY, renderZ, 0.0F, 0.0F, 0.0F, 0.4F);
        }

        return true;
    }

    private static void renderSubBlockGrid(PoseStack poseStack, VertexConsumer consumer, BlockPos blockPos, Direction face,
                                           double camX, double camY, double camZ) {
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

        GridPlane plane = GridPlane.from(blockPos, face);
        for (int i = 0; i <= SubBlockGrid.GRID_RESOLUTION; i++) {
            double offset = i * SubBlockGrid.GRID_CELL_SIZE;
            double[] lineA = plane.lineAlongFirstAxis(offset);
            emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                    lineA[0], lineA[1], lineA[2], lineA[3], lineA[4], lineA[5],
                    red, green, blue, alpha, normalX, normalY, normalZ);

            double[] lineB = plane.lineAlongSecondAxis(offset);
            emitGridLine(consumer, poseMatrix, normalMatrix, camX, camY, camZ,
                    lineB[0], lineB[1], lineB[2], lineB[3], lineB[4], lineB[5],
                    red, green, blue, alpha, normalX, normalY, normalZ);
        }
    }

    private static void emitGridLine(VertexConsumer consumer, Matrix4f poseMatrix, Matrix3f normalMatrix,
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

    @FunctionalInterface
    public interface ShapeRenderer {
        void render(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double x, double y, double z,
                    float red, float green, float blue, float alpha);
    }

    private record GridPlane(
            double fixed,
            Direction.Axis fixedAxis,
            Direction.Axis firstAxis,
            Direction.Axis secondAxis,
            double firstMin,
            double firstMax,
            double secondMin,
            double secondMax) {
        private static final double EPSILON = 0.002;

        static GridPlane from(BlockPos blockPos, Direction face) {
            Direction.Axis fixedAxis = face.getAxis();
            Direction.Axis firstAxis = getFirstGridAxis(fixedAxis);
            Direction.Axis secondAxis = getSecondGridAxis(fixedAxis);
            double fixed = fixedCoordinate(blockPos, face);

            return new GridPlane(
                    fixed,
                    fixedAxis,
                    firstAxis,
                    secondAxis,
                    coordinate(blockPos, firstAxis),
                    coordinate(blockPos, firstAxis) + 1.0D,
                    coordinate(blockPos, secondAxis),
                    coordinate(blockPos, secondAxis) + 1.0D);
        }

        double[] lineAlongFirstAxis(double secondOffset) {
            double second = secondMin + secondOffset;
            return newLine(firstMin, second, firstMax, second);
        }

        double[] lineAlongSecondAxis(double firstOffset) {
            double first = firstMin + firstOffset;
            return newLine(first, secondMin, first, secondMax);
        }

        private double[] newLine(double startFirst, double startSecond, double endFirst, double endSecond) {
            double[] start = newPoint(startFirst, startSecond);
            double[] end = newPoint(endFirst, endSecond);
            return new double[]{start[0], start[1], start[2], end[0], end[1], end[2]};
        }

        private double[] newPoint(double first, double second) {
            double[] point = new double[3];
            point[index(fixedAxis)] = fixed;
            point[index(firstAxis)] = first;
            point[index(secondAxis)] = second;
            return point;
        }

        private static double fixedCoordinate(BlockPos blockPos, Direction face) {
            double coordinate = coordinate(blockPos, face.getAxis());
            return face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? coordinate + 1.0D + EPSILON
                    : coordinate - EPSILON;
        }

        private static Direction.Axis getFirstGridAxis(Direction.Axis fixedAxis) {
            return fixedAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }

        private static Direction.Axis getSecondGridAxis(Direction.Axis fixedAxis) {
            return fixedAxis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        }

        private static double coordinate(BlockPos blockPos, Direction.Axis axis) {
            return switch (axis) {
                case X -> blockPos.getX();
                case Y -> blockPos.getY();
                case Z -> blockPos.getZ();
            };
        }

        private static int index(Direction.Axis axis) {
            return switch (axis) {
                case X -> 0;
                case Y -> 1;
                case Z -> 2;
            };
        }
    }
}
