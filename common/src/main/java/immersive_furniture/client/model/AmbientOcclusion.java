package immersive_furniture.client.model;

import org.joml.*;

import java.lang.Math;
import java.util.*;

public class AmbientOcclusion {
    public static final AmbientOcclusion INSTANCE = new AmbientOcclusion();
    private static final float STEP_SIZE = (float) (1.0f / Math.sqrt(3));

    final boolean[] solid;
    final Map<Long, Float> cache = new HashMap<>();

    final int width;
    final int height;
    final int depth;

    final int offsetX;
    final int offsetY;
    final int offsetZ;

    int totalKernelWeight = 0;
    final List<Vector4i> kernel = new ArrayList<>();

    public AmbientOcclusion() {
        width = 52;
        height = 52;
        depth = 52;
        offsetX = 18;
        offsetY = 18;
        offsetZ = 18;
        solid = new boolean[width * height * depth];

        int radius = 4;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distance = x * x + y * y + z * z;
                    if (distance > 0 && distance <= radius * radius) {
                        int weight = radius * radius - distance;
                        kernel.add(new Vector4i(x, y, z, weight));
                        totalKernelWeight += weight;
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
        cache.clear();
    }

    public void place(Vector3i size, Vector3f origin, Quaternionf rotation) {
        Vector3f nx = rotation.transform(new Vector3f(size.x(), 0, 0));
        Vector3f ny = rotation.transform(new Vector3f(0, size.y(), 0));
        Vector3f nz = rotation.transform(new Vector3f(0, 0, size.z()));

        int width = (int) Math.ceil(size.x() / STEP_SIZE);
        int height = (int) Math.ceil(size.y() / STEP_SIZE);
        int depth = (int) Math.ceil(size.z() / STEP_SIZE);

        for (int ix = 0; ix <= width; ix++) {
            for (int iy = 0; iy <= height; iy++) {
                for (int iz = 0; iz <= depth; iz++) {
                    float x = (float) ix / width;
                    float y = (float) iy / height;
                    float z = (float) iz / depth;

                    int gx = Math.round(nx.x * x + ny.x * y + nz.x * z + origin.x);
                    int gy = Math.round(nx.y * x + ny.y * y + nz.y * z + origin.y);
                    int gz = Math.round(nx.z * x + ny.z * y + nz.z * z + origin.z);

                    set(gx, gy, gz);
                }
            }
        }
    }


    public float getValue(Vector3f pos) {
        // TODO: Tri-linear filtering
        return getValue(
                (int) Math.floor(pos.x + 0.5f),
                (int) Math.floor(pos.y + 0.5f),
                (int) Math.floor(pos.z + 0.5f)
        );
    }

    public float getValue(int x, int y, int z) {
        long key = (long) x << 32 | (long) y << 16 | (long) z;
        return cache.computeIfAbsent(key, k -> getUncachedValue(x, y, z));
    }

    private float getUncachedValue(int x, int y, int z) {
        float value = 0.0f;
        for (Vector4i offset : kernel) {
            if (is(x + offset.x, y + offset.y, z + offset.z)) {
                value += offset.w;
            }
        }
        return value / totalKernelWeight;
    }
}
