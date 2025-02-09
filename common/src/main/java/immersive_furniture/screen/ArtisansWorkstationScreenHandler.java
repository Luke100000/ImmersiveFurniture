package immersive_furniture.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArtisansWorkstationScreenHandler extends AbstractContainerMenu {
    private final Container inventory;

    public ArtisansWorkstationScreenHandler(int syncId, Inventory playerInventory) {
        super(null, syncId);

        this.inventory = playerInventory;

        // The player inventory
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18,  y * 18));
            }
        }

        // The player Hotbar
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18,  58));
        }
    }

    public boolean stillValid(Player player) {
        return true;
    }

    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (index < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    // Overwritten since max stack size isn't considered in vanilla
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean unused) {
        boolean inserted = false;

        // try to stack
        if (stack.isStackable()) {
            int i = startIndex;
            while (!stack.isEmpty() && (i < endIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack target = slot.getItem();
                if (!target.isEmpty() && ItemStack.isSameItemSameTags(stack, target)) {
                    int diff = target.getCount() + stack.getCount();
                    int maxCount = slot.getMaxStackSize(stack);
                    if (diff <= maxCount) {
                        stack.setCount(0);
                        target.setCount(diff);
                        slot.setChanged();
                        inserted = true;
                    } else if (target.getCount() < maxCount) {
                        stack.shrink(maxCount - target.getCount());
                        target.setCount(maxCount);
                        slot.setChanged();
                        inserted = true;
                    }
                }
                i++;
            }
        }

        // use a new slot
        if (!stack.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                Slot slot = this.slots.get(i);
                ItemStack target = slot.getItem();
                int maxCount = slot.getMaxStackSize(target);
                if (target.isEmpty() && slot.mayPlace(stack)) {
                    if (stack.getCount() > maxCount) {
                        slot.setByPlayer(stack.split(maxCount));
                    } else {
                        slot.setByPlayer(stack.split(stack.getCount()));
                    }
                    slot.setChanged();
                    inserted = true;
                    break;
                }
            }
        }
        return inserted;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
