package net.conczin.immersive_furniture.client.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.conczin.immersive_furniture.data.ModelUtils.getElementRotation;

public class AmbientOcclusion {
    private static final double SAMPLE_RESOLUTION = Math.sqrt(3);
    private static final float RESOLUTION = 0.25f;

    record PrecomputedElement(FurnitureData.Element element, Quaternionf rotation, Vector3f origin) {

    }

    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<PrecomputedElement>> elementCache = new Long2ObjectOpenHashMap<>();

    final static List<Vector3f> kernel = new ArrayList<>();

    static {
        int radius = 4;
        for (float x = -radius; x <= radius; x++) {
            for (float y = -radius; y <= radius; y++) {
                for (float z = -radius; z <= radius; z++) {
                    float distance = x * x + y * y + z * z;
                    if (distance > 0 && distance <= radius * radius) {
                        kernel.add(new Vector3f(x, y, z));
                    }
                }
            }
        }
    }

    private Set<PrecomputedElement> getElements(float x, float y, float z) {
        int gx = Math.round(x * RESOLUTION);
        int gy = Math.round(y * RESOLUTION);
        int gz = Math.round(z * RESOLUTION);
        long key = (long) gx << 40 | (long) gy << 20 | (long) gz;
        return elementCache.computeIfAbsent(key, k -> new ObjectOpenHashSet<>());
    }

    public void place(FurnitureData.Element element) {
        Vector3f center = element.getCenter();
        Vector3i size = element.getSize();

        ElementRotation elementRotation = element.getRotation();
        Quaternionf rotation = getElementRotation(elementRotation);
        PrecomputedElement precomputed = new PrecomputedElement(element, new Quaternionf(rotation).conjugate(), element.getOrigin().mul(16.0f));

        Vector3f nx = rotation.transform(new Vector3f(size.x(), 0, 0));
        Vector3f ny = rotation.transform(new Vector3f(0, size.y(), 0));
        Vector3f nz = rotation.transform(new Vector3f(0, 0, size.z()));

        int width = (int) Math.ceil(size.x() * SAMPLE_RESOLUTION * RESOLUTION);
        int height = (int) Math.ceil(size.y() * SAMPLE_RESOLUTION * RESOLUTION);
        int depth = (int) Math.ceil(size.z() * SAMPLE_RESOLUTION * RESOLUTION);

        for (int ix = 0; ix <= width; ix++) {
            for (int iy = 0; iy <= height; iy++) {
                for (int iz = 0; iz <= depth; iz++) {
                    float buffer = (float) (2.0f * SAMPLE_RESOLUTION * RESOLUTION);
                    float sx = (ix - width / 2.0f) / (width - buffer);
                    float sy = (iy - height / 2.0f) / (height - buffer);
                    float sz = (iz - depth / 2.0f) / (depth - buffer);

                    float x = nx.x * sx + ny.x * sy + nz.x * sz + center.x;
                    float y = nx.y * sx + ny.y * sy + nz.y * sz + center.y;
                    float z = nx.z * sx + ny.z * sy + nz.z * sz + center.z;

                    getElements(x, y, z).add(precomputed);
                }
            }
        }
    }

    private boolean is(float x, float y, float z) {
        float e = 0.0001f;
        Vector3f pos = new Vector3f();
        for (PrecomputedElement p : getElements(x, y, z)) {
            pos.set(x - p.origin.x, y - p.origin.y, z - p.origin.z);
            p.rotation.transform(pos);
            pos.add(p.origin);

            if (pos.x > p.element.from.x + e && pos.x < p.element.to.x - e &&
                pos.y > p.element.from.y + e && pos.y < p.element.to.y - e &&
                pos.z > p.element.from.z + e && pos.z < p.element.to.z - e) {
                return true;
            }
        }
        return false;
    }

    public float sample(Vector3f pos, Vector3f normal) {
        float value = 0.0f;
        float totalWeight = 0.0f;
        for (Vector3f offset : kernel) {
            float dot = normal.x * offset.x + normal.y * offset.y + normal.z * offset.z;
            if (dot <= 0) continue;
            if (is(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z)) {
                value += 1.0f;
            }
            totalWeight += 1.0f;
        }
        return value / totalWeight;
    }
}
