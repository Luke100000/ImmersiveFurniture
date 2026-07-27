package net.conczin.immersive_furniture.client.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.conczin.immersive_furniture.data.ElementRotation;
import net.conczin.immersive_furniture.data.FurnitureData;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

import static net.conczin.immersive_furniture.data.ModelUtils.getElementRotation;

public class AmbientOcclusion {
    private static final double SAMPLE_RESOLUTION = Math.sqrt(3);
    private static final float RESOLUTION = 0.25f;
    private static final float BOUNDS_EPSILON = 0.0001f;
    private static final long COORDINATE_MASK = (1L << 20) - 1L;

    private static final class PrecomputedElement {
        private final float originX;
        private final float originY;
        private final float originZ;
        private final float m00;
        private final float m01;
        private final float m02;
        private final float m10;
        private final float m11;
        private final float m12;
        private final float m20;
        private final float m21;
        private final float m22;
        private final float minX;
        private final float minY;
        private final float minZ;
        private final float maxX;
        private final float maxY;
        private final float maxZ;
        private final float opacity;

        private PrecomputedElement(FurnitureData.Element element, Quaternionf rotation, Vector3f origin, float opacity) {
            originX = origin.x;
            originY = origin.y;
            originZ = origin.z;

            float xx = rotation.x * rotation.x;
            float yy = rotation.y * rotation.y;
            float zz = rotation.z * rotation.z;
            float ww = rotation.w * rotation.w;
            float xy = rotation.x * rotation.y;
            float xz = rotation.x * rotation.z;
            float yz = rotation.y * rotation.z;
            float xw = rotation.x * rotation.w;
            float zw = rotation.z * rotation.w;
            float yw = rotation.y * rotation.w;
            float k = 1.0f / (xx + yy + zz + ww);

            m00 = (xx - yy - zz + ww) * k;
            m01 = 2.0f * (xy - zw) * k;
            m02 = 2.0f * (xz + yw) * k;
            m10 = 2.0f * (xy + zw) * k;
            m11 = (yy - xx - zz + ww) * k;
            m12 = 2.0f * (yz - xw) * k;
            m20 = 2.0f * (xz - yw) * k;
            m21 = 2.0f * (yz + xw) * k;
            m22 = (zz - xx - yy + ww) * k;

            minX = element.from.x + BOUNDS_EPSILON;
            minY = element.from.y + BOUNDS_EPSILON;
            minZ = element.from.z + BOUNDS_EPSILON;
            maxX = element.to.x - BOUNDS_EPSILON;
            maxY = element.to.y - BOUNDS_EPSILON;
            maxZ = element.to.z - BOUNDS_EPSILON;
            this.opacity = opacity;
        }

    }

    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<PrecomputedElement>> placementCache = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<PrecomputedElement[]> elementCache = new Long2ObjectOpenHashMap<>();
    private boolean prepared;

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

    private static long getKey(float x, float y, float z) {
        int gx = Math.round(x * RESOLUTION);
        int gy = Math.round(y * RESOLUTION);
        int gz = Math.round(z * RESOLUTION);
        return ((long) gx & COORDINATE_MASK) << 40 |
               ((long) gy & COORDINATE_MASK) << 20 |
               ((long) gz & COORDINATE_MASK);
    }

    private ObjectOpenHashSet<PrecomputedElement> getElementsForPlacement(float x, float y, float z) {
        return placementCache.computeIfAbsent(getKey(x, y, z), ignored -> new ObjectOpenHashSet<>());
    }

    private void prepare() {
        if (prepared) return;

        for (Long2ObjectMap.Entry<ObjectOpenHashSet<PrecomputedElement>> entry : placementCache.long2ObjectEntrySet()) {
            elementCache.put(entry.getLongKey(), entry.getValue().toArray(new PrecomputedElement[0]));
        }
        placementCache.clear();
        prepared = true;
    }

    public void place(FurnitureData.Element element, float opacity) {
        if (prepared) throw new IllegalStateException("Cannot place elements after AO sampling has started");

        Vector3f center = element.getCenter();
        Vector3i size = element.getSize();

        ElementRotation elementRotation = element.getRotation();
        Quaternionf rotation = getElementRotation(elementRotation);
        PrecomputedElement precomputed = new PrecomputedElement(
                element,
                new Quaternionf(rotation).conjugate(), element.getOrigin().mul(16.0f),
                opacity
        );

        Vector3f nx = rotation.transform(new Vector3f(size.x(), 0, 0));
        Vector3f ny = rotation.transform(new Vector3f(0, size.y(), 0));
        Vector3f nz = rotation.transform(new Vector3f(0, 0, size.z()));

        float buffer = (float) (2.0f * SAMPLE_RESOLUTION * RESOLUTION);
        int width = (int) Math.ceil(size.x() * SAMPLE_RESOLUTION * RESOLUTION + buffer);
        int height = (int) Math.ceil(size.y() * SAMPLE_RESOLUTION * RESOLUTION + buffer);
        int depth = (int) Math.ceil(size.z() * SAMPLE_RESOLUTION * RESOLUTION + buffer);

        for (int ix = 0; ix <= width; ix++) {
            for (int iy = 0; iy <= height; iy++) {
                for (int iz = 0; iz <= depth; iz++) {
                    float sx = (ix - width / 2.0f) / (width - buffer);
                    float sy = (iy - height / 2.0f) / (height - buffer);
                    float sz = (iz - depth / 2.0f) / (depth - buffer);

                    float x = nx.x * sx + ny.x * sy + nz.x * sz + center.x;
                    float y = nx.y * sx + ny.y * sy + nz.y * sz + center.y;
                    float z = nx.z * sx + ny.z * sy + nz.z * sz + center.z;

                    getElementsForPlacement(x, y, z).add(precomputed);
                }
            }
        }
    }

    private float is(float x, float y, float z) {
        PrecomputedElement[] elements = elementCache.get(getKey(x, y, z));
        if (elements == null) return 0.0f;

        for (PrecomputedElement p : elements) {
            float localX = x - p.originX;
            float localY = y - p.originY;
            float localZ = z - p.originZ;

            float transformedX = p.m00 * localX + (p.m01 * localY + p.m02 * localZ) + p.originX;
            if (!(transformedX > p.minX && transformedX < p.maxX)) continue;

            float transformedY = p.m10 * localX + (p.m11 * localY + p.m12 * localZ) + p.originY;
            if (!(transformedY > p.minY && transformedY < p.maxY)) continue;

            float transformedZ = p.m20 * localX + (p.m21 * localY + p.m22 * localZ) + p.originZ;
            if (transformedZ > p.minZ && transformedZ < p.maxZ) return p.opacity;
        }
        return 0.0f;
    }

    Sampler createSampler(Vector3f normal) {
        prepare();

        List<Vector3f> offsets = new ArrayList<>();
        for (Vector3f offset : kernel) {
            float dot = normal.x * offset.x + normal.y * offset.y + normal.z * offset.z;
            if (dot <= 0) continue;
            offsets.add(offset);
        }
        return new Sampler(offsets.toArray(new Vector3f[0]));
    }

    final class Sampler {
        private final Vector3f[] offsets;

        private Sampler(Vector3f[] offsets) {
            this.offsets = offsets;
        }

        float sample(Vector3f pos) {
            float value = 0.0f;
            for (Vector3f offset : offsets) {
                value += is(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
            }
            return value / offsets.length;
        }
    }
}
