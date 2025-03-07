package immersive_furniture.client;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Utils {
    public static boolean isWithinQuad(float x, float y, Vector3f[] quad) {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            Vector3f p1 = quad[i];
            Vector3f p2 = quad[(i + 1) % 4];

            float x1 = p1.x, y1 = p1.y;
            float x2 = p2.x, y2 = p2.y;

            if (y > Math.min(y1, y2) && y <= Math.max(y1, y2)) {
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
