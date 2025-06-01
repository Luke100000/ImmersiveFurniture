package immersive_furniture.item;

import immersive_furniture.Blocks;
import immersive_furniture.block.EntityFurnitureBlock;
import immersive_furniture.block.FurnitureBlock;
import immersive_furniture.block.LightFurnitureBlock;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.ChatFormatting;
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
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FurnitureItem extends BlockItem {
    public static final String FURNITURE = "Furniture";
    public static final String FURNITURE_HASH = "FurnitureHash";

    public FurnitureItem(Properties settings) {
        super(Blocks.FURNITURE.get(), settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getData(stack).name);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        FurnitureData data = getData(stack);
        if (!data.author.isEmpty()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.author", data.author).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, world, tooltip, context);
    }

    private final static Map<CompoundTag, FurnitureData> cache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CompoundTag, FurnitureData> eldest) {
            return size() > 100;
        }
    };

    public static FurnitureData getData(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(BLOCK_ENTITY_TAG);
        return tag == null ? FurnitureData.EMPTY : cache.computeIfAbsent(tag.getCompound(FURNITURE), FurnitureData::new);
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
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
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
            state = Objects.requireNonNull(Blocks.FURNITURE_ENTITY.get().getStateForPlacement(context))
                    .setValue(EntityFurnitureBlock.LIGHT, data.lightLevel);
        } else if (data.lightLevel > 0) {
            state = Objects.requireNonNull(Blocks.FURNITURE_LIGHT.get().getStateForPlacement(context))
                    .setValue(LightFurnitureBlock.IDENTIFIER, identifier - 65536)
                    .setValue(LightFurnitureBlock.LIGHT, (int) Math.ceil(data.lightLevel / 3.0f));
        } else {
            state = Objects.requireNonNull(Blocks.FURNITURE.get().getStateForPlacement(context))
                    .setValue(FurnitureBlock.IDENTIFIER, identifier);
        }

        return this.canPlace(context, state) ? state : null;
    }
}
