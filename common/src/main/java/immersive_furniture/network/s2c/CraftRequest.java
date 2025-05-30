package immersive_furniture.network.s2c;

import immersive_furniture.utils.Utils;
import immersive_furniture.cobalt.network.Message;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.item.FurnitureItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static immersive_furniture.Items.FURNITURE;

public class CraftRequest extends Message {
    FurnitureData data;
    boolean shift;

    public CraftRequest(FurnitureData data, boolean shift) {
        this.data = data;
        this.shift = shift;
    }

    public CraftRequest(FriendlyByteBuf b) {
        data = new FurnitureData(Utils.fromBytes(b.readByteArray()));
        shift = b.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        CompoundTag tag = data.toTag();

        b.writeByteArray(Utils.toBytes(tag));
        b.writeBoolean(shift);
    }

    @Override
    public void receive(Player e) {
        if (!isValid(data)) return;

        int cost = data.getCost();
        int available = getResources(e);

        int amount = 1;
        if (shift) amount = Math.min(available / cost, 64);
        if (amount == 0) return;

        if (available < cost * amount) {
            e.displayClientMessage(Component.translatable("immersive_furniture.not_enough_material"), true);
        } else {
            useResources(e, amount * cost);
            giveFurniture(e, data, amount);
        }
    }

    private static boolean isValid(FurnitureData data) {
        return data != null;
    }

    private static boolean isValidItem(ItemStack stack) {
        return stack.getItem() == Items.OAK_PLANKS;
    }

    private int getResources(Player e) {
        int amount = 0;
        Inventory inventory = e.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isValidItem(stack)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    private void useResources(Player e, int amount) {
        Inventory inventory = e.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isValidItem(stack)) {
                if (stack.getCount() >= amount) {
                    stack.shrink(amount);
                    return;
                } else {
                    amount -= stack.getCount();
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private void giveFurniture(Player e, FurnitureData data, int count) {
        ItemStack stack = new ItemStack(FURNITURE.get(), count);
        FurnitureItem.setData(stack, data);
        if (!e.getInventory().add(stack)) {
            e.drop(stack, false);
        }
    }
}
