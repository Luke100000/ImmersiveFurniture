package immersive_furniture.client.model.effects;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.minecraft.nbt.CompoundTag;

public abstract class MaterialEffect {
    public abstract void load(CompoundTag tag);

    public abstract CompoundTag save();

    public abstract int initGUI(ArtisansWorkstationEditorScreen screen, int x, int y, int width);
}
