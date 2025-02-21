package immersive_furniture.client;

import org.joml.Vector4f;

public class Utils {
    /**
     * Checks if the point (x, y) is inside the quadrilateral formed by v0, v1, v2, v3.
     *
     * @param x    The x-coordinate of the point.
     * @param y    The y-coordinate of the point.
     * @param quad Quadrilateral.
     * @return True if the point (x, y) is inside the quadrilateral, false otherwise.
     */
    public static boolean isWithinQuad(float x, float y, Vector4f[] quad) {
        // Create an array of the vertices
        int count = 0;

        // Iterate over each edge of the quadrilateral
        for (int i = 0; i < 4; i++) {
            Vector4f p1 = quad[i];
            Vector4f p2 = quad[(i + 1) % 4]; // Next vertex (wrap around at the end)

            // Extract coordinates
            float x1 = p1.x, y1 = p1.y;
            float x2 = p2.x, y2 = p2.y;

            // Check if the point is within the vertical bounds of the edge
            if (y > Math.min(y1, y2) && y <= Math.max(y1, y2)) {
                // Find the x-coordinate of the intersection of the ray with the edge
                if (y2 != y1) {
                    float xIntersection = (y - y1) * (x2 - x1) / (y2 - y1) + x1;
                    if (x <= xIntersection) {
                        count++;
                    }
                }
            }
        }

        // If the ray crosses an odd number of times, the point is inside the quad
        return count % 2 != 0;
    }
}
