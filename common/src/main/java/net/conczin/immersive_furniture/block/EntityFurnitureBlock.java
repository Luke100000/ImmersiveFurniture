package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.block.entity.FurnitureBlockEntity;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class EntityFurnitureBlock extends BaseFurnitureBlock implements EntityBlock {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    public EntityFurnitureBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(LIGHT, 0)
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isCrouching()) {
            if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity blockEntity && blockEntity.getContainerSize() > 0) {
                player.openMenu(blockEntity);
                return InteractionResult.CONSUME;
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockPos = context.getClickedPos();
        Level levelAccessor = context.getLevel();
        boolean waterlogged = levelAccessor.getFluidState(blockPos).getType() == Fluids.WATER;
        return this.defaultBlockState()
                .setValue(WATERLOGGED, waterlogged)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT, WATERLOGGED, FACING, ACTIVE);
    }

    public FurnitureData getData(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity blockEntity) {
            return blockEntity.getData();
        }
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnitureBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FurnitureBlockEntity) {
            Containers.dropContents(level, pos, ((FurnitureBlockEntity) blockEntity).getItems());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState toggleLight(FurnitureData data, BlockState state, Level level, BlockPos pos) {
        return state.setValue(LIGHT, data.toggleLight && state.getValue(ACTIVE) ? 0 : data.lightLevel);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FurnitureBlockEntity furnitureBlockEntity) {
                furnitureBlockEntity.setData(FurnitureItem.getData(stack));
            }
        }
    }
}
