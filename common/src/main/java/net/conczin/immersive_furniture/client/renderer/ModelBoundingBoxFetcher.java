package net.conczin.immersive_furniture.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelBoundingBoxFetcher {
    public static final ModelBoundingBoxFetcher INSTANCE = new ModelBoundingBoxFetcher();

    private final Map<Long, AABB> CACHE = new HashMap<>();
    private final RandomSource random = RandomSource.create();

    public AABB getModelBoundingBox(BakedModel bakedmodel) {
        long modelKey = System.identityHashCode(bakedmodel);
        return CACHE.computeIfAbsent(modelKey, k -> computeModelBoundingBox(bakedmodel));
    }

    private AABB computeModelBoundingBox(BakedModel bakedmodel) {
        float minX = 3.0f;
        float minY = 3.0f;
        float minZ = 3.0f;
        float maxX = -3.0f;
        float maxY = -3.0f;
        float maxZ = -3.0f;

        // Transform to actual item space
        ItemTransform transform = bakedmodel.getTransforms().getTransform(ItemDisplayContext.FIXED);
        PoseStack poseStack = new PoseStack();
        transform.apply(false, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Matrix4f pose = poseStack.last().pose();

        for (int d = 0; d < 7; d++) {
            Direction direction = d == 6 ? null : Direction.from3DDataValue(d);
            List<BakedQuad> quads = bakedmodel.getQuads(null, direction, random);
            for (BakedQuad quad : quads) {
                if (quad.getDirection() == Direction.SOUTH) continue;
                if (quad.getDirection() == Direction.NORTH) continue;

                int[] vertices = quad.getVertices();
                for (int i = 0; i < 4; i++) {
                    int offset = i * 8;
                    float x = Float.intBitsToFloat(vertices[offset]);
                    float y = Float.intBitsToFloat(vertices[offset + 1]);
                    float z = Float.intBitsToFloat(vertices[offset + 2]);

                    Vector4f pos = pose.transform(x, y, z, 1.0f, new Vector4f());

                    if (pos.x < minX) minX = pos.x;
                    if (pos.y < minY) minY = pos.y;
                    if (pos.z < minZ) minZ = pos.z;
                    if (pos.x > maxX) maxX = pos.x;
                    if (pos.y > maxY) maxY = pos.y;
                    if (pos.z > maxZ) maxZ = pos.z;
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
