package net.conczin.immersive_furniture.data;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Enum representing different transparency types for furniture blocks.
 * Used to determine the render type for the block.
 */
public enum TransparencyType implements StringRepresentable {
    SOLID,
    CUTOUT_MIPPED,
    CUTOUT,
    TRANSLUCENT;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    public boolean isHigherPriorityThan(TransparencyType other) {
        return ordinal() > other.ordinal();
    }
}