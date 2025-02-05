package immersive_furniture.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class FurnitureData {
    public static final FurnitureData EMPTY = new FurnitureData();

    private final String name;

    public FurnitureData() {
        name = "Empty";
    }

    public FurnitureData(CompoundTag tag) {
        name = tag.getString("Name");
    }

    public String getName() {
        return name;
    }

    public Tag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }
}
