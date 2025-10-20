package net.conczin.immersive_furniture.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for managing a 16x16 sub-block grid system within a block space.
 * This allows for precise placement of furniture within fractional positions of a block.
 */
public class SubBlockGrid {
    /** The number of sub-divisions per block axis (16x16 grid) */
    public static final int GRID_RESOLUTION = 16;
    
    /** The size of each grid cell as a fraction of a block (1/16 = 0.0625) */
    public static final double GRID_CELL_SIZE = 1.0 / GRID_RESOLUTION;
    
    /**
     * Snaps a world position to the nearest sub-block grid point.
     * Only snaps the X and Z coordinates, Y remains unchanged.
     * 
     * @param worldPos The world position to snap
     * @return A new Vec3 with X and Z snapped to the grid within their respective blocks
     */
    public static Vec3 snapToGrid(Vec3 worldPos) {
        // Get the block coordinates
        double blockX = Math.floor(worldPos.x);
        double blockZ = Math.floor(worldPos.z);
        
        // Get the fractional part within the block
        double fracX = worldPos.x - blockX;
        double fracZ = worldPos.z - blockZ;
        
        // Snap the fractional parts to the grid
        double snappedFracX = snapCoordinateToGrid(fracX);
        double snappedFracZ = snapCoordinateToGrid(fracZ);
        
        // Combine back to world coordinates
        double snappedX = blockX + snappedFracX;
        double snappedZ = blockZ + snappedFracZ;
        
        return new Vec3(snappedX, worldPos.y, snappedZ);
    }
    
    /**
     * Snaps a single coordinate to the nearest grid point.
     * For coordinates within a block (0.0 to 1.0 range).
     * 
     * @param coord The coordinate to snap (should be 0.0 to 1.0)
     * @return The snapped coordinate
     */
    public static double snapCoordinateToGrid(double coord) {
        // Ensure coordinate is within block bounds
        coord = Math.max(0.0, Math.min(1.0, coord));
        
        // Snap to the nearest grid cell
        double gridCell = Math.round(coord * GRID_RESOLUTION);
        gridCell = Math.max(0, Math.min(GRID_RESOLUTION, gridCell)); // Clamp to valid range
        return gridCell * GRID_CELL_SIZE;
    }
    
    /**
     * Gets the sub-block offset from a world position.
     * This returns the fractional position within the block (0.0 to 1.0).
     * 
     * @param worldPos The world position
     * @return A Vec3 containing the X and Z offsets within the block (Y is always 0)
     */
    public static Vec3 getSubBlockOffset(Vec3 worldPos) {
        double offsetX = worldPos.x - Math.floor(worldPos.x);
        double offsetZ = worldPos.z - Math.floor(worldPos.z);
        return new Vec3(offsetX, 0.0, offsetZ);
    }
    
    /**
     * Gets the sub-block offset snapped to the grid from a world position.
     * 
     * @param worldPos The world position
     * @return A Vec3 containing the snapped X and Z offsets within the block
     */
    public static Vec3 getSnappedSubBlockOffset(Vec3 worldPos) {
        Vec3 snapped = snapToGrid(worldPos);
        return getSubBlockOffset(snapped);
    }
    
    /**
     * Applies a sub-block offset to a block position to get a precise world position.
     * 
     * @param blockPos The base block position
     * @param offset The sub-block offset (should be in range 0.0 to 1.0)
     * @return The world position with the offset applied
     */
    public static Vec3 applySubBlockOffset(BlockPos blockPos, Vec3 offset) {
        return new Vec3(
            blockPos.getX() + offset.x,
            blockPos.getY() + offset.y,
            blockPos.getZ() + offset.z
        );
    }
    
    /**
     * Checks if a sub-block offset is valid (within 0.0 to 1.0 range for X and Z).
     * 
     * @param offset The offset to validate
     * @return true if the offset is valid
     */
    public static boolean isValidOffset(Vec3 offset) {
        return offset.x >= 0.0 && offset.x <= 1.0 && 
               offset.z >= 0.0 && offset.z <= 1.0;
    }
    
    /**
     * Gets the grid index for a coordinate (0 to GRID_RESOLUTION).
     * 
     * @param coord The coordinate within a block (0.0 to 1.0)
     * @return The grid index
     */
    public static int getGridIndex(double coord) {
        return (int) Math.round(coord * GRID_RESOLUTION);
    }
    
    /**
     * Gets the coordinate for a grid index.
     * 
     * @param gridIndex The grid index (0 to GRID_RESOLUTION)
     * @return The coordinate within a block
     */
    public static double getCoordinateFromGrid(int gridIndex) {
        return gridIndex * GRID_CELL_SIZE;
    }
}