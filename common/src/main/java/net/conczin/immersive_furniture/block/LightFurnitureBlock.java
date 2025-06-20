package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.FurnitureRegistry;
import net.conczin.immersive_furniture.data.TransparencyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;

public class LightFurnitureBlock extends BaseFurnitureBlock {
    public static final IntegerProperty IDENTIFIER = IntegerProperty.create("identifier", 0, 255);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 5);

    public LightFurnitureBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(IDENTIFIER, 0)
                .setValue(LIGHT, 0)
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(TRANSPARENCY, TransparencyType.SOLID));
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
        builder.add(IDENTIFIER, LIGHT, WATERLOGGED, FACING, TRANSPARENCY);
    }

    @Override
    public FurnitureData getData(BlockState state, BlockGetter level, BlockPos pos) {
        int identifier = state.getValue(IDENTIFIER) + 65536;
        String hash = FurnitureRegistry.resolve(identifier);
        return hash != null ? FurnitureDataManager.getData(hash) : null;
    }
}
