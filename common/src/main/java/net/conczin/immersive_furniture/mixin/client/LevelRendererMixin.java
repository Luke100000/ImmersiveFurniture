package net.conczin.immersive_furniture.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
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
                if (data != null) {
                    Direction direction = player.getDirection().getOpposite();
                    VoxelShape shape = data.getShape(direction);
                    BlockPos clickedPos = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, blockHitResult)).getClickedPos();

                    renderShape(poseStack,
                            consumer,
                            shape,
                            (double) clickedPos.getX() - camX,
                            (double) clickedPos.getY() - camY,
                            (double) clickedPos.getZ() - camZ,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.4F
                    );

                    ci.cancel();
                }
            }
        }
    }
}