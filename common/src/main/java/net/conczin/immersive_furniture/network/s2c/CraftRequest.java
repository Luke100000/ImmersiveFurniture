package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Sounds;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static net.conczin.immersive_furniture.item.Items.CRAFTING_MATERIAL;
import static net.conczin.immersive_furniture.item.Items.FURNITURE;

public class CraftRequest implements ImmersivePayload {
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
    public void handle(Player e) {
        if (!isValid(data)) return;

        int cost = data.getCost();
        int available = getResources(e);

        if (e.isCreative()) available = Integer.MAX_VALUE;

        int amount = 1;
        if (shift) amount = Math.min(available / cost, 64);
        if (amount == 0) return;

        if (available < cost * amount) {
            e.displayClientMessage(Component.translatable("immersive_furniture.not_enough_material"), true);
        } else {
            if (!e.isCreative()) {
                useResources(e, amount * cost);
            }
            giveFurniture(e, data, amount);
            e.level().playLocalSound(e.getOnPos(), Sounds.REPAIR, SoundSource.BLOCKS, 1.0F, e.getRandom().nextFloat() * 0.5f + 0.75f, false);
        }
    }

    private static boolean isValid(FurnitureData data) {
        return data != null;
    }

    private static boolean isValidItem(ItemStack stack) {
        return stack.getItem() == CRAFTING_MATERIAL;
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
        ItemStack stack = new ItemStack(FURNITURE, count);
        FurnitureItem.setData(stack, data);
        if (!e.getInventory().add(stack)) {
            e.drop(stack, false);
        }
    }
}
