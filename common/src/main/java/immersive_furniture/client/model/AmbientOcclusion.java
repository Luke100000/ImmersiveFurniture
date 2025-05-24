package immersive_furniture.client.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AmbientOcclusion {
    public static final AmbientOcclusion INSTANCE = new AmbientOcclusion();
    private static final float STEP_SIZE = (float) (1.0f / Math.sqrt(3));

    final boolean[] solid;

    final int width;
    final int height;
    final int depth;

    final int offsetX;
    final int offsetY;
    final int offsetZ;

    float totalKernelWeight = 0;
    final List<Vector4f> kernel = new ArrayList<>();

    public AmbientOcclusion() {
        width = 52;
        height = 52;
        depth = 52;
        offsetX = 18;
        offsetY = 18;
        offsetZ = 18;
        solid = new boolean[width * height * depth];

        int radius = 4;
        for (float x = -radius; x <= radius; x++) {
            for (float y = -radius; y <= radius; y++) {
                for (float z = -radius; z <= radius; z++) {
                    float distance = x * x + y * y + z * z;
                    if (distance > 0 && distance <= radius * radius) {
                        float weight = radius * radius - distance;
                        if (weight > 0) {
                            kernel.add(new Vector4f(x, y, z, weight));
                            totalKernelWeight += weight;
                        }
                    }
                }
            }
        }
    }

    public void set(int x, int y, int z) {
        x += offsetX;
        y += offsetY;
        z += offsetZ;
        if (x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth) {
            solid[x + y * width + z * width * height] = true;
        }
    }

    public float random(float a) {
        int seed = Float.floatToIntBits(a) * 73428767;
        seed ^= seed >>> 13;
        seed *= 0x5bd1e995;
        seed ^= seed >>> 15;
        return (seed & 0xFFFFFF) / (float) 0x1000000 - 0.5f;
    }

    public boolean is(float x, float y, float z) {
        float s = x + y + z;
        float rx = random(s * 0.7f);
        float ry = random(s * 1.7f);
        float rz = random(s * 2.3f);
        return is(Math.round(x + rx), Math.round(y + ry), Math.round(z + rz));
    }

    public boolean is(int x, int y, int z) {
        x += offsetX;
        y += offsetY;
        z += offsetZ;
        if (x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth) {
            return solid[x + y * width + z * width * height];
        } else {
            return false;
        }
    }

    public void clear() {
        Arrays.fill(solid, false);
    }

    public void place(Vector3i size, Vector3f center, Quaternionf rotation) {
        Vector3f nx = rotation.transform(new Vector3f(size.x(), 0, 0));
        Vector3f ny = rotation.transform(new Vector3f(0, size.y(), 0));
        Vector3f nz = rotation.transform(new Vector3f(0, 0, size.z()));

        int width = (int) Math.ceil(size.x() / STEP_SIZE);
        int height = (int) Math.ceil(size.y() / STEP_SIZE);
        int depth = (int) Math.ceil(size.z() / STEP_SIZE);

        for (int ix = 0; ix <= width; ix++) {
            for (int iy = 0; iy <= height; iy++) {
                for (int iz = 0; iz <= depth; iz++) {
                    float buffer = 1.0f / STEP_SIZE;
                    float x = (ix - width / 2.0f) / (width + buffer);
                    float y = (iy - height / 2.0f) / (height + buffer);
                    float z = (iz - depth / 2.0f) / (depth + buffer);

                    int gx = Math.round(nx.x * x + ny.x * y + nz.x * z + center.x);
                    int gy = Math.round(nx.y * x + ny.y * y + nz.y * z + center.y);
                    int gz = Math.round(nx.z * x + ny.z * y + nz.z * z + center.z);

                    set(gx, gy, gz);
                }
            }
        }
    }

    public float getValue(Vector3f pos) {
        float value = 0.0f;
        for (Vector4f offset : kernel) {
            if (is(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z)) {
                value += offset.w;
            }
        }
        return value / totalKernelWeight;
    }
}
