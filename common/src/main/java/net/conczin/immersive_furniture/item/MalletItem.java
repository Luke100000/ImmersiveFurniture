package net.conczin.immersive_furniture.item;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.block.FurnitureProxyBlock;
import net.conczin.immersive_furniture.block.entity.FurnitureOffsetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MalletItem extends Item {
    public MalletItem(Properties properties) {
        super(properties);
    }

    public static boolean isFurniture(BlockState state) {
        return state.getBlock() instanceof BaseFurnitureBlock || state.getBlock() instanceof FurnitureProxyBlock;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !isFurniture(state);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!isFurniture(level.getBlockState(pos))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            nudge(level, pos, context.getClickedFace());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void nudge(Level level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FurnitureProxyBlock proxy) {
            pos = proxy.getBasePos(state, pos);
        }
        if (!(level.getBlockEntity(pos) instanceof FurnitureOffsetHolder holder)) {
            return;
        }

        int x = holder.getSubOffsetX();
        int y = holder.getSubOffsetY();
        int z = holder.getSubOffsetZ();
        holder.setSubOffset(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());

        boolean moved = x != holder.getSubOffsetX() || y != holder.getSubOffsetY() || z != holder.getSubOffsetZ();
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0f, moved ? 1.0f : 0.5f);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.immersive_furniture.mallet.tooltip").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
