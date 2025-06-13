package net.conczin.immersive_furniture.data;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModelUtils {
    public static Quaternionf getElementRotation(ElementRotation rotation) {
        Vector3f axis = switch (rotation.axis()) {
            case X -> new Vector3f(1.0f, 0.0f, 0.0f);
            case Y -> new Vector3f(0.0f, 1.0f, 0.0f);
            case Z -> new Vector3f(0.0f, 0.0f, 1.0f);
        };

        return new Quaternionf().rotationAxis(rotation.angle() * ((float) Math.PI / 180), axis);
    }

    // Expects pos to be in voxel space (16 units per block)
    public static void applyElementRotation(Vector3f pos, ElementRotation rotation) {
        if (rotation == null) return;
        Quaternionf quaternionf = getElementRotation(rotation);
        pos.mul(1.0f / 16.0f);
        pos.sub(rotation.origin());
        quaternionf.transform(pos);
        pos.add(rotation.origin());
        pos.mul(16.0f);
    }

    public static void applyInverseElementRotation(Vector3f pos, ElementRotation rotation) {
        Quaternionf quaternionf = getElementRotation(rotation).conjugate();
        pos.sub(rotation.origin());
        quaternionf.transform(pos);
        pos.add(rotation.origin());
    }

    public static Vector3f[] getCorners(FurnitureData.Element element) {
        Vector3f[] corners = new Vector3f[8];
        corners[0] = new Vector3f(element.from.x(), element.from.y(), element.from.z());
        corners[1] = new Vector3f(element.from.x(), element.from.y(), element.to.z());
        corners[2] = new Vector3f(element.from.x(), element.to.y(), element.from.z());
        corners[3] = new Vector3f(element.from.x(), element.to.y(), element.to.z());
        corners[4] = new Vector3f(element.to.x(), element.from.y(), element.from.z());
        corners[5] = new Vector3f(element.to.x(), element.from.y(), element.to.z());
        corners[6] = new Vector3f(element.to.x(), element.to.y(), element.from.z());
        corners[7] = new Vector3f(element.to.x(), element.to.y(), element.to.z());

        ElementRotation rotation = element.getRotation();
        for (int i = 0; i < 8; i++) {
            applyElementRotation(corners[i], rotation);
        }

        return corners;
    }
}
