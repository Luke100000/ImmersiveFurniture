package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.InteractionManager;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.conczin.immersive_furniture.entity.SittingEntity;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.s2c.PoseOffsetMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public abstract class BaseFurnitureBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<TransparencyType> TRANSPARENCY = EnumProperty.create("transparency", TransparencyType.class);

    public BaseFurnitureBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        FurnitureData data = getData(state, level, pos);
        if (data != null) {
            // Find closest pose element
            Vec3 click = new Vec3(hit.getLocation().x - pos.getX(), hit.getLocation().y - pos.getY(), hit.getLocation().z - pos.getZ());
            FurnitureData.PoseOffset offset = data.getClosestPose(click, state.getValue(FACING));

            if (offset != null) {
                // Remember interaction for the player for some injection purposes
                InteractionManager.INSTANCE.addInteraction(player, pos, offset);

                if (offset.pose() == Pose.SLEEPING) {
                    startSleeping(pos, player, offset);
                } else if (offset.pose() == Pose.SITTING) {
                    startSitting(level, pos, player, offset);
                }

                return InteractionResult.CONSUME;
            }

            return level.isClientSide && (data.playInteractSound(level, pos, player) | data.emitInteractParticles(pos, player, level::addParticle, false)) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    private static void startSleeping(BlockPos pos, Player player, FurnitureData.PoseOffset offset) {
        if (player.level().isClientSide) return;

        if (player instanceof ServerPlayer serverPlayer) {
            PoseOffsetMessage message = new PoseOffsetMessage(pos, offset, serverPlayer);
            Network.sendToAllPlayers(serverPlayer.serverLevel().getServer(), message);
        }

        player.startSleepInBed(pos).ifLeft(problem -> {
            if (problem.getMessage() != null) {
                player.displayClientMessage(problem.getMessage(), true);
            }
        });
    }

    private static void startSitting(Level level, BlockPos pos, Player player, FurnitureData.PoseOffset offset) {
        // Create an entity to fake sitting
        if (!level.isClientSide) {
            SittingEntity sittingEntity = new SittingEntity(level, new Vec3(
                    pos.getX() + offset.offset().x,
                    pos.getY() + offset.offset().y,
                    pos.getZ() + offset.offset().z
            ), new Vec3(player.getX(), player.getY(), player.getZ()));
            sittingEntity.setYRot(offset.rotation());
            player.startRiding(sittingEntity);
            sittingEntity.clampRotation(player);
            level.addFreshEntity(sittingEntity);
        }
        player.hasImpulse = true;
    }

    abstract public FurnitureData getData(BlockState state, BlockGetter level, BlockPos pos);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        FurnitureData data = getData(state, level, pos);
        if (data != null) {
            data.tick(level, pos, random, level::addParticle, false, false);
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
            return data.getShape(state.getValue(FACING));
        }
        return Block.box(2, 2, 2, 14, 14, 14);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            // Remove all proxy blocks when the base block is destroyed
            FurnitureData data = getData(state, level, pos);
            if (data != null) {
                Direction facing = state.getValue(FACING);

                // Check and destroy all possible proxy blocks
                for (int x = 0; x < data.size.x; x++) {
                    for (int y = 0; y < data.size.y; y++) {
                        for (int z = 0; z < data.size.z; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            BlockPos proxyPos = getProxyPosition(pos, facing, x, y, z);
                            BlockState proxyState = level.getBlockState(proxyPos);
                            if (proxyState.getBlock() instanceof FurnitureProxyBlock) {
                                level.removeBlock(proxyPos, false);
                            }
                        }
                    }
                }
            }

            if (!player.isCreative()) {
                // Drop the furniture item with data
                ItemStack itemStack = getCloneItemStack(level, pos, state);
                Block.popResource(level, pos, itemStack);
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Get the position of a proxy block based on facing and offset
     */
    public static BlockPos getProxyPosition(BlockPos basePos, Direction facing, int offsetX, int offsetY, int offsetZ) {
        int dx = 0, dz = 0;

        switch (facing) {
            case NORTH:
                dz = offsetZ;
                dx = offsetX;
                break;
            case SOUTH:
                dz = -offsetZ;
                dx = -offsetX;
                break;
            case EAST:
                dx = -offsetZ;
                dz = offsetX;
                break;
            case WEST:
                dx = offsetZ;
                dz = -offsetX;
                break;
            default:
                break;
        }

        return basePos.offset(dx, offsetY, dz);
    }
}
