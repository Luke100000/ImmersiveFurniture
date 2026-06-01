package net.conczin.immersive_furniture.block.entity;

public interface FurnitureOffsetHolder {
    int DEFAULT_OFFSET = 8;

    int getSubOffsetX();

    int getSubOffsetZ();

    default void setSubOffset(int subOffsetX, int subOffsetZ) {
        setSubOffset(subOffsetX, subOffsetZ, true);
    }

    void setSubOffset(int subOffsetX, int subOffsetZ, boolean sync);
}
