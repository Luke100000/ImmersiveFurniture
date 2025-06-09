package net.conczin.immersive_furniture.item;

import net.conczin.immersive_furniture.block.Blocks;
import net.conczin.immersive_furniture.block.EntityFurnitureBlock;
import net.conczin.immersive_furniture.block.FurnitureBlock;
import net.conczin.immersive_furniture.block.LightFurnitureBlock;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FurnitureItem extends BlockItem {
    public static final String FURNITURE = "Furniture";
    public static final String FURNITURE_HASH = "FurnitureHash";

    public FurnitureItem(Properties settings) {
        super(Blocks.FURNITURE, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getData(stack).name);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        FurnitureData data = getData(stack);
        tooltip.addAll(data.getTooltip(Screen.hasShiftDown()));
        super.appendHoverText(stack, world, tooltip, context);
    }

    private final static Map<Integer, FurnitureData> cache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, FurnitureData> eldest) {
            return size() > 100;
        }
    };

    public static FurnitureData getData(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(BLOCK_ENTITY_TAG);
        if (tag == null) return FurnitureData.EMPTY;
        tag = tag.getCompound(FURNITURE);
        int hash = System.identityHashCode(tag); // Use identity hash since it's way faster
        if (!cache.containsKey(hash)) {
            cache.put(hash, new FurnitureData(tag));
        }
        return cache.get(hash);
    }

    public static void setData(ItemStack stack, FurnitureData data) {
        CompoundTag tag = stack.getOrCreateTagElement(BLOCK_ENTITY_TAG);
        tag.put(FURNITURE, data.toTag());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Because the client cannot predict what exact furniture type will be used, we skip here.
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        // Keep track of placed furniture to estimate usage
        if (context.getLevel() instanceof ServerLevel level) {
            ServerFurnitureRegistry.increase(level, getData(context.getItemInHand()));
        }

        return super.placeBlock(context, state);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        // If the block has been placed often enough, it will have an identifier.
        int identifier = -1;
        ItemStack stack = context.getItemInHand();
        FurnitureData data = FurnitureItem.getData(stack);
        if (!data.requiresBlockEntity() && context.getLevel() instanceof ServerLevel level) {
            int from = data.lightLevel > 0 ? 65536 : 0;
            int size = data.lightLevel > 0 ? 256 : 1024;
            identifier = ServerFurnitureRegistry.registerIdentifier(level, data, from, from + size - 1);
        }

        BlockState state;
        if (identifier < 0) {
            state = Objects.requireNonNull(Blocks.FURNITURE_ENTITY.getStateForPlacement(context))
                    .setValue(EntityFurnitureBlock.LIGHT, data.lightLevel);
        } else if (data.lightLevel > 0) {
            state = Objects.requireNonNull(Blocks.FURNITURE_LIGHT.getStateForPlacement(context))
                    .setValue(LightFurnitureBlock.IDENTIFIER, identifier - 65536)
                    .setValue(LightFurnitureBlock.LIGHT, (int) Math.ceil(data.lightLevel / 3.0f));
        } else {
            state = Objects.requireNonNull(Blocks.FURNITURE.getStateForPlacement(context))
                    .setValue(FurnitureBlock.IDENTIFIER, identifier);
        }

        return this.canPlace(context, state) ? state : null;
    }
}
