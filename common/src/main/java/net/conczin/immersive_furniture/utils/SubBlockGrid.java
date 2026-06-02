package net.conczin.immersive_furniture.utils;

/**
 * Utility class for managing a 16x16 sub-block grid system within a block space.
 * This allows for precise placement of furniture within fractional positions of a block.
 */
public class SubBlockGrid {
    /**
     * The number of subdivisions per block axis (16x16 grid)
     */
    public static final int GRID_RESOLUTION = 16;

    /**
     * The size of each grid cell as a fraction of a block (1/16 = 0.0625)
     */
    public static final double GRID_CELL_SIZE = 1.0 / GRID_RESOLUTION;


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
     * Gets the grid index for a coordinate (0 to GRID_RESOLUTION).
     *
     * @param coord The coordinate within a block (0.0 to 1.0)
     * @return The grid index
     */
    public static int getGridIndex(double coord) {
        return (int) Math.round(coord * GRID_RESOLUTION);
    }
}