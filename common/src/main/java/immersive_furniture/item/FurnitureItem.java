package immersive_furniture.item;

import immersive_furniture.Blocks;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.data.ServerFurnitureRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FurnitureItem extends BlockItem {
    public static final String FURNITURE = "Furniture";
    public static final String FURNITURE_HASH = "FurnitureHash";

    public FurnitureItem(Properties settings) {
        super(Blocks.FURNITURE.get(), settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        FurnitureData data = getData(stack);
        if (!data.author.isEmpty()) {
            tooltip.add(Component.translatable("gui.immersive_furniture.author", data.author).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, world, tooltip, context);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getData(stack).name);
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
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        // Keep track of placed furniture to estimate usage
        if (context.getLevel() instanceof ServerLevel level) {
            ServerFurnitureRegistry.increase(level, getData(context.getItemInHand()));
        }

        return super.placeBlock(context, state);
    }
}
