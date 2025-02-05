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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FurnitureItem extends BlockItem {
    public static final String FURNITURE = "Furniture";

    public FurnitureItem(Properties settings) {
        super(Blocks.FURNITURE.get(), settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        // TODO: Overview of the item
        tooltip.add(Component.translatable("item.immersive_furniture.todo").withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, world, tooltip, context);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getData(stack).getName());
    }

    private static @NotNull FurnitureData getData(ItemStack stack) {
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        return tag == null ? FurnitureData.EMPTY : new FurnitureData(tag.getCompound(FURNITURE));
    }
}
