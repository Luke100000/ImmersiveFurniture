package immersive_furniture.block;

import immersive_furniture.BlockEntityTypes;
import immersive_furniture.config.Config;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.data.FurnitureDataManager;
import immersive_furniture.item.FurnitureItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FurnitureBlockEntity extends BlockEntity implements Container, MenuProvider {
    private String hash;
    private FurnitureData data;

    private final NonNullList<ItemStack> items = NonNullList.withSize(81, ItemStack.EMPTY);

    public FurnitureBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityTypes.FURNITURE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FurnitureBlockEntity blockEntity) {

    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FurnitureBlockEntity blockEntity) {
        // TODO: If required, this ticker is of higher precision than the random animate tick
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ContainerHelper.loadAllItems(tag, this.items);

        if (tag.contains(FurnitureItem.FURNITURE)) {
            this.data = new FurnitureData(tag.getCompound(FurnitureItem.FURNITURE));
        } else if (tag.contains(FurnitureItem.FURNITURE_HASH)) {
            // Delay loading
            hash = tag.getString(FurnitureItem.FURNITURE_HASH);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        ContainerHelper.saveAllItems(tag, this.items);

        if (this.data != null) {
            if (Config.getInstance().saveAsHash) {
                FurnitureDataManager.save(data, new ResourceLocation("hash", this.data.getHash()));
                tag.putString(FurnitureItem.FURNITURE_HASH, this.data.getHash());
            } else {
                tag.put(FurnitureItem.FURNITURE, this.data.toTag());
            }
        } else if (hash != null) {
            tag.putString(FurnitureItem.FURNITURE_HASH, hash);
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public FurnitureData getData() {
        if (hash != null) {
            data = FurnitureDataManager.getData(new ResourceLocation("hash", hash), true);
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
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        int rows = getData().inventorySize;
        if (rows == 0) {
            return null;
        }

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
}

