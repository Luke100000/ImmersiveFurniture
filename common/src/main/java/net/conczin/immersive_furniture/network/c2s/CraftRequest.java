package net.conczin.immersive_furniture.network.c2s;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.Sounds;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static net.conczin.immersive_furniture.item.Items.CRAFTING_MATERIAL;
import static net.conczin.immersive_furniture.item.Items.FURNITURE;

public record CraftRequest(FurnitureData data, boolean shift) implements ImmersivePayload {
    private static long lastSoundTime = 0;

    public static final CustomPacketPayload.Type<CraftRequest> TYPE = new CustomPacketPayload.Type<>(Common.locate("craft_request"));
    public static final StreamCodec<FriendlyByteBuf, CraftRequest> STREAM_CODEC = StreamCodec.composite(
            FurnitureData.STREAM_CODEC, CraftRequest::data,
            ByteBufCodecs.BOOL, CraftRequest::shift,
            CraftRequest::new
    );

    @Override
    public void handle(Player e) {
        if (!isValid(data)) return;
        if (!e.hasPermissions(Config.getInstance().requiredCraftingPermissionLevel)) {
            e.displayClientMessage(Component.translatable("immersive_furniture.no_permission"), true);
            return;
        }

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

            // Play crafting sound
            long time = System.currentTimeMillis();
            float volume = Math.min(1.0f, Math.abs(lastSoundTime - time) / 1000.0f);
            if (volume > 0.05f) {
                lastSoundTime = time;
                e.level().playSound(
                        null,
                        e.getOnPos(),
                        Sounds.ASSEMBLE,
                        SoundSource.BLOCKS,
                        (e.getRandom().nextFloat() * 0.2f + 0.8f) * volume,
                        e.getRandom().nextFloat() * 0.5f + 0.75f
                );
            }
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

    @Override
    public Type<CraftRequest> type() {
        return TYPE;
    }
}
