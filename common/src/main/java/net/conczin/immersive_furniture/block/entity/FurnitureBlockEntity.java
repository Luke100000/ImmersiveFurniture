package net.conczin.immersive_furniture.block.entity;

import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FurnitureBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final String FURNITURE = "Furniture";
    public static final String FURNITURE_HASH = "FurnitureHash";

    private String hash;
    private FurnitureData data;

    private final NonNullList<ItemStack> items = NonNullList.withSize(81, ItemStack.EMPTY);

    public FurnitureBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityTypes.FURNITURE, pos, blockState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);

        if (tag.contains("components") && tag.getCompound("components").contains("immersive_furniture:furniture")) {
            this.data = new FurnitureData(tag.getCompound("components").getCompound("immersive_furniture:furniture"));
        } else if (tag.contains(FURNITURE)) {
            this.data = new FurnitureData(tag.getCompound(FURNITURE));
        } else if (tag.contains(FURNITURE_HASH)) {
            // Delay loading
            hash = tag.getString(FURNITURE_HASH);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        ContainerHelper.saveAllItems(tag, this.items, registries);

        if (this.data != null) {
            if (Config.getInstance().saveAsHash) {
                FurnitureDataManager.saveHashData(this.data);
                tag.putString(FURNITURE_HASH, this.data.getHash());
            } else {
                tag.put(FURNITURE, this.data.toTag());
            }
        } else if (hash != null) {
            tag.putString(FURNITURE_HASH, hash);
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public FurnitureData getData() {
        if (hash != null) {
            if (level != null && level.isClientSide) {
                data = FurnitureDataManager.getCachedData(hash);
            } else {
                data = FurnitureDataManager.getData(hash);
            }
            if (data != null) {
                hash = null;
            }
        }
        return data;
    }

    @Override
    public int getContainerSize() {
        return getData().inventorySize * 9;
    }

    @Override
    public void clearContent() {
        getItems().clear();
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : getItems()) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getItems().get(slot);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        // Sync inventory changes to the client in case it has display slots
        if (level != null) {
            FurnitureData data = getData();
            if (data != null && data.hasDisplayItems()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(getItems(), slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(getItems(), slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getItems().set(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Component getDisplayName() {
        FurnitureData d = getData();
        return d == null ? Component.translatable("gui.immersive_furniture.furniture") : Component.literal(d.name);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        FurnitureData data = getData();
        if (data == null) return null;
        int rows = data.inventorySize;
        if (rows == 0) return null;

        MenuType<?> menuType = switch (rows) {
            case 6 -> MenuType.GENERIC_9x6;
            case 5 -> MenuType.GENERIC_9x5;
            case 4 -> MenuType.GENERIC_9x4;
            case 3 -> MenuType.GENERIC_9x3;
            case 2 -> MenuType.GENERIC_9x2;
            default -> MenuType.GENERIC_9x1;
        };

        return new ChestMenu(menuType, containerId, inventory, this, rows);
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            FurnitureBlockEntity.this.playSound(SoundEvents.BARREL_OPEN);
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            FurnitureBlockEntity.this.playSound(SoundEvents.BARREL_CLOSE);
        }
    }

    void playSound(SoundEvent sound) {
        if (level == null) return;
        double d = (double) this.worldPosition.getX() + 0.5;
        double e = (double) this.worldPosition.getY() + 0.5;
        double f = (double) this.worldPosition.getZ() + 0.5;
        this.level.playSound(null, d, e, f, sound, SoundSource.BLOCKS, 0.5f, this.level.random.nextFloat() * 0.1f + 0.9f);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }
}

