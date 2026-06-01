package net.conczin.immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A much faster outline renderer than vanilla's.
 */
public final class CachedShapeRenderer {
    private static final Map<VoxelShape, Edge[]> EDGES = Collections.synchronizedMap(new WeakHashMap<>());

    public static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape,
                                   double x, double y, double z,
                                   float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (Edge edge : getEdges(shape)) {
            consumer.vertex(poseMatrix, (float) (edge.startX + x), (float) (edge.startY + y), (float) (edge.startZ + z))
                    .color(red, green, blue, alpha)
                    .normal(normalMatrix, edge.normalX, edge.normalY, edge.normalZ)
                    .endVertex();
            consumer.vertex(poseMatrix, (float) (edge.endX + x), (float) (edge.endY + y), (float) (edge.endZ + z))
                    .color(red, green, blue, alpha)
                    .normal(normalMatrix, edge.normalX, edge.normalY, edge.normalZ)
                    .endVertex();
        }
    }

    private static Edge[] getEdges(VoxelShape shape) {
        Edge[] edges = EDGES.get(shape);
        if (edges != null) {
            return edges;
        }

        synchronized (EDGES) {
            edges = EDGES.get(shape);
            if (edges == null) {
                List<Edge> collectedEdges = new ArrayList<>();
                shape.forAllEdges((startX, startY, startZ, endX, endY, endZ) -> {
                    float normalX = (float) (endX - startX);
                    float normalY = (float) (endY - startY);
                    float normalZ = (float) (endZ - startZ);
                    float length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
                    normalX /= length;
                    normalY /= length;
                    normalZ /= length;

                    collectedEdges.add(new Edge(
                            startX,
                            startY,
                            startZ,
                            endX,
                            endY,
                            endZ,
                            normalX,
                            normalY,
                            normalZ));
                });
                edges = collectedEdges.toArray(Edge[]::new);
                EDGES.put(shape, edges);
            }
            return edges;
        }
    }

    private record Edge(
            double startX, double startY, double startZ,
            double endX, double endY, double endZ,
            float normalX, float normalY, float normalZ
    ) {
    }
}
