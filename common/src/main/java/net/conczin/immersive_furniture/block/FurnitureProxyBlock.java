package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A proxy block used for multi-block furniture structures.
 * It forwards interactions to the base furniture block and
 * gets destroyed when the base block is destroyed.
 */
public class FurnitureProxyBlock extends Block {
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 3);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 0, 3);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 3);
    public static final DirectionProperty FACING = BaseFurnitureBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BaseFurnitureBlock.WATERLOGGED;

    public FurnitureProxyBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                this.stateDefinition.any()
                        .setValue(OFFSET_X, 0)
                        .setValue(OFFSET_Y, 0)
                        .setValue(OFFSET_Z, 0)
                        .setValue(FACING, Direction.NORTH)
                        .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OFFSET_X, OFFSET_Y, OFFSET_Z, FACING, WATERLOGGED);
    }

    /**
     * Get the base block position from this proxy block
     */
    public BlockPos getBasePos(BlockState state, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        int offsetX = state.getValue(OFFSET_X);
        int offsetY = state.getValue(OFFSET_Y);
        int offsetZ = state.getValue(OFFSET_Z);
        return BaseFurnitureBlock.getProxyPosition(pos, direction, -offsetX, -offsetY, -offsetZ);
    }

    /**
     * Get the base furniture block if it exists
     */
    protected BaseFurnitureBlock getBaseBlock(LevelAccessor level, BlockState state, BlockPos pos) {
        BlockPos basePos = getBasePos(state, pos);
        BlockState baseState = level.getBlockState(basePos);
        if (baseState.getBlock() instanceof BaseFurnitureBlock baseBlock) {
            return baseBlock;
        }
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level instanceof Level) {
            BlockPos basePos = getBasePos(state, pos);
            BlockState baseState = level.getBlockState(basePos);
            if (baseState.getBlock() instanceof BaseFurnitureBlock baseBlock) {
                return baseBlock.getShape(baseState, level, basePos, context).move(
                        basePos.getX() - pos.getX(),
                        basePos.getY() - pos.getY(),
                        basePos.getZ() - pos.getZ()
                );
            }
        }

        // Fallback shape
        return Block.box(4, 4, 4, 12, 12, 12);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Forward the interaction to the base block
        BaseFurnitureBlock baseBlock = getBaseBlock(level, state, pos);
        if (baseBlock != null) {
            BlockPos basePos = getBasePos(state, pos);
            BlockState baseState = level.getBlockState(basePos);

            // Adjust the hit position
            BlockHitResult adjustedHit = new BlockHitResult(
                    hit.getLocation(),
                    hit.getDirection(),
                    basePos,
                    hit.isInside()
            );

            return baseBlock.use(baseState, level, basePos, player, hand, adjustedHit);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // When proxy is destroyed, destroy the base block too if it exists
        if (!level.isClientSide) {
            BlockPos basePos = getBasePos(state, pos);
            BlockState baseState = level.getBlockState(basePos);
            if (baseState.getBlock() instanceof BaseFurnitureBlock) {
                level.destroyBlock(basePos, !player.isCreative());
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        // Check if the base block exists, if not, remove this proxy
        if (getBaseBlock(level, state, currentPos) == null) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }
}
