package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.InteractionManager;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.entity.SittingEntity;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFurnitureBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BaseFurnitureBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        FurnitureData data = getData(state, level, pos);
        if (data != null) {
            if (level.isClientSide) {
                data.playInteractSound(level, pos, player);
            } else {
                // Find closest pose element
                Vec3 click = new Vec3(hit.getLocation().x - pos.getX(), hit.getLocation().y - pos.getY(), hit.getLocation().z - pos.getZ());
                FurnitureData.PoseOffset offset = data.getClosestPose(click, state.getValue(FACING));

                if (offset != null) {
                    // Remember interaction for the player for some injection purposes
                    InteractionManager.INSTANCE.addInteraction(player, pos, offset);

                    if (offset.pose() == Pose.SLEEPING) {
                        // Start sleeping
                        player.startSleepInBed(pos).ifLeft(problem -> {
                            if (problem.getMessage() != null) {
                                player.displayClientMessage(problem.getMessage(), true);
                            }
                        });
                    } else if (offset.pose() == Pose.SITTING) {
                        // Create an entity to fake sitting
                        SittingEntity sittingEntity = new SittingEntity(level, new Vec3(
                                pos.getX() + offset.offset().x,
                                pos.getY() + offset.offset().y,
                                pos.getZ() + offset.offset().z
                        ), new Vec3(player.getX(), player.getY(), player.getZ()));
                        level.addFreshEntity(sittingEntity);
                        sittingEntity.setYRot(offset.rotation());
                        player.startRiding(sittingEntity);
                        sittingEntity.clampRotation(player);
                    }
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    abstract public FurnitureData getData(BlockState state, BlockGetter level, BlockPos pos);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        FurnitureData data = getData(state, level, pos);
        if (data != null) {
            data.tick(level, pos, random, level::addParticle, false);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        FurnitureData data = getData(state, level, pos);
        if (data != null) {
            return data.getShape(state.getValue(FACING).getOpposite());
        }
        return Block.box(2, 2, 2, 14, 14, 14);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            return true;
        }
        return false;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public Item asItem() {
        return Items.FURNITURE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack itemStack = new ItemStack(asItem());
        FurnitureData data = getData(state, level, pos);
        if (data != null) {
            FurnitureItem.setData(itemStack, data);
        }
        return itemStack;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}

