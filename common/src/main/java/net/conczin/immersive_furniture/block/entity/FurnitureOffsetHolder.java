package net.conczin.immersive_furniture.block.entity;

public interface FurnitureOffsetHolder {
    int DEFAULT_OFFSET = 8;

    int getSubOffsetX();

    int getSubOffsetY();

    int getSubOffsetZ();

    default void setSubOffset(int subOffsetX, int subOffsetY, int subOffsetZ) {
        setSubOffset(subOffsetX, subOffsetY, subOffsetZ, true);
    }

    void setSubOffset(int subOffsetX, int subOffsetY, int subOffsetZ, boolean sync);
}
