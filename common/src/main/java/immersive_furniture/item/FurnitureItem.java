package immersive_furniture.item;

import immersive_furniture.Blocks;
import immersive_furniture.data.FurnitureData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FurnitureItem extends BlockItem {
    public static final String FURNITURE = "Furniture";

    public FurnitureItem(Properties settings) {
        super(Blocks.FURNITURE.get(), settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        FurnitureData data = getData(stack);
        // TODO: Add tooltip for furniture data
        tooltip.add(Component.literal(String.valueOf(data.elements.size())).withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, world, tooltip, context);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getData(stack).name);
    }

    public static FurnitureData getData(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(BLOCK_ENTITY_TAG);
        return tag == null ? FurnitureData.EMPTY : new FurnitureData(tag.getCompound(FURNITURE));
    }

    public static void setData(ItemStack stack, FurnitureData data) {
        CompoundTag tag = stack.getOrCreateTagElement(BLOCK_ENTITY_TAG);
        tag.put(FURNITURE, data.toTag());
    }
}
