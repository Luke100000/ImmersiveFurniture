package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.block.entity.FurnitureOffsetBlockEntity;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.FurnitureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class FurnitureBlock extends BaseFurnitureBlock implements EntityBlock {
    public static final int IDENTIFIER_COUNT = 128;
    public static final IntegerProperty IDENTIFIER = IntegerProperty.create("identifier", 0, IDENTIFIER_COUNT - 1);

    public FurnitureBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(IDENTIFIER, 0)
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(POWERED, false));
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
        builder.add(IDENTIFIER, WATERLOGGED, FACING, ACTIVE, POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnitureOffsetBlockEntity(pos, state);
    }

    public FurnitureData getData(BlockState state, BlockGetter level, BlockPos pos) {
        int identifier = state.getValue(IDENTIFIER);
        String hash = FurnitureRegistry.resolve(identifier);
        return hash != null ? FurnitureDataManager.getData(hash) : null;
    }
}
