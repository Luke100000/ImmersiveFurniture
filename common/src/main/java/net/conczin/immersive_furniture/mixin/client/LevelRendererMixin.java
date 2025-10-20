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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double x, double y, double z, float red, float green, float blue, float alpha) {
    }

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void immersiveFurniture$onRenderLevel(PoseStack poseStack, VertexConsumer consumer, Entity entity, double camX, double camY, double camZ, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (entity instanceof Player player && minecraft.hitResult instanceof BlockHitResult blockHitResult) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof FurnitureItem) {
                FurnitureData data = FurnitureItem.getData(stack);
                if (data == null) return;

                Direction direction = player.getDirection().getOpposite();
                VoxelShape shape = data.getShapeLazy(direction);
                if (shape == null) return;

                BlockPos clickedPos = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, blockHitResult)).getClickedPos();
                
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
                            1.0F,  // Green tint for precision mode
                            0.0F,
                            0.6F   // Slightly more opaque
                    );
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
                            0.4F
                    );
                }

                ci.cancel();
            }
        }
    }
}