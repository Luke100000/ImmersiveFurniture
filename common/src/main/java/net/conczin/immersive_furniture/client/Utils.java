package net.conczin.immersive_furniture.client;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Utils {
    public static Ray inverseTransformRay(float mouseX, float mouseY, Matrix4f modelViewMatrix) {
        Vector4f rayStart = new Vector4f(
                mouseX,
                mouseY,
                1.0f,
                1.0f
        );

        // Z is 1 (far plane)
        Vector4f rayEnd = new Vector4f(rayStart.x, rayStart.y, -1.0f, 1.0f);

        // Invert the model-view matrix
        Matrix4f inverseModelView = new Matrix4f(modelViewMatrix).invert();

        // Transform ray start and end to model-space
        Vector4f rayStartModel = inverseModelView.transform(rayStart);
        Vector4f rayEndModel = inverseModelView.transform(rayEnd);

        // Divide by w to get the actual coordinates
        rayStartModel.div(rayStartModel.w);
        rayEndModel.div(rayEndModel.w);

        // Create ray origin and direction
        Vector3f origin = new Vector3f(rayStartModel.x, rayStartModel.y, rayStartModel.z);
        Vector3f direction = new Vector3f(
                rayEndModel.x - rayStartModel.x,
                rayEndModel.y - rayStartModel.y,
                rayEndModel.z - rayStartModel.z
        ).normalize();

        return new Ray(origin, direction);
    }

    public static RaycastResult raycast(Ray ray, FurnitureData.Element element) {
        // Convert element bounds to model-space
        Vector3f min = new Vector3f(element.from.x() / 16.0f, element.from.y() / 16.0f, element.from.z() / 16.0f);
        Vector3f max = new Vector3f(element.to.x() / 16.0f, element.to.y() / 16.0f, element.to.z() / 16.0f);

        // Calculate intersection with each axis-aligned plane
        float tMinX = (min.x - ray.origin.x) / ray.direction.x;
        float tMaxX = (max.x - ray.origin.x) / ray.direction.x;
        if (tMinX > tMaxX) {
            float temp = tMinX;
            tMinX = tMaxX;
            tMaxX = temp;
        }

        float tMinY = (min.y - ray.origin.y) / ray.direction.y;
        float tMaxY = (max.y - ray.origin.y) / ray.direction.y;
        if (tMinY > tMaxY) {
            float temp = tMinY;
            tMinY = tMaxY;
            tMaxY = temp;
        }

        float tMinZ = (min.z - ray.origin.z) / ray.direction.z;
        float tMaxZ = (max.z - ray.origin.z) / ray.direction.z;
        if (tMinZ > tMaxZ) {
            float temp = tMinZ;
            tMinZ = tMaxZ;
            tMaxZ = temp;
        }

        // Find the largest minimum and the smallest maximum
        float tMin = Math.max(Math.max(tMinX, tMinY), tMinZ);
        float tMax = Math.min(Math.min(tMaxX, tMaxY), tMaxZ);

        // If tMin > tMax, the ray doesn't intersect the box
        if (tMin > tMax) {
            return null;
        }

        // Calculate the intersection point
        Vector3f intersection = new Vector3f(
                ray.origin.x + ray.direction.x * tMin,
                ray.origin.y + ray.direction.y * tMin,
                ray.origin.z + ray.direction.z * tMin
        );

        Direction face = getDirection(intersection, min, max);

        if (face == null) return null;

        return new RaycastResult(element, face, -tMin, intersection);
    }

    private static Direction getDirection(Vector3f intersection, Vector3f min, Vector3f max) {
        float epsilon = 0.0001f;
        if (Math.abs(intersection.x - min.x) < epsilon) {
            return Direction.WEST;
        } else if (Math.abs(intersection.x - max.x) < epsilon) {
            return Direction.EAST;
        } else if (Math.abs(intersection.y - min.y) < epsilon) {
            return Direction.DOWN;
        } else if (Math.abs(intersection.y - max.y) < epsilon) {
            return Direction.UP;
        } else if (Math.abs(intersection.z - min.z) < epsilon) {
            return Direction.NORTH;
        } else if (Math.abs(intersection.z - max.z) < epsilon) {
            return Direction.SOUTH;
        }
        return null;
    }

    public record Ray(Vector3f origin, Vector3f direction) {
    }

    public record RaycastResult(FurnitureData.Element element, Direction face, float distance, Vector3f intersection) {
    }

    public static float[] rgbToHsv(int color) {
        float r = FastColor.ABGR32.red(color) / 255f;
        float g = FastColor.ABGR32.green(color) / 255f;
        float b = FastColor.ABGR32.blue(color) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta == 0) h = 0;
        else if (max == r) h = 60 * (((g - b) / delta) % 6);
        else if (max == g) h = 60 * (((b - r) / delta) + 2);
        else h = 60 * (((r - g) / delta) + 4);
        if (h < 0) h += 360;

        float s = max == 0 ? 0 : delta / max;
        return new float[]{h, s, max};
    }

    public static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r, g, b;

        if (h < 60) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 120) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 180) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 240) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int ri = Math.round((r + m) * 255);
        int gi = Math.round((g + m) * 255);
        int bi = Math.round((b + m) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
