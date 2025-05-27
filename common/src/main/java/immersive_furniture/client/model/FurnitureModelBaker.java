package immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.utils.CachedSupplier;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Supplier;

public class FurnitureModelBaker {
    public static final ResourceLocation LOCATION = Common.locate("block/furniture");

    static class ModelBakerImpl implements ModelBaker {
        ModelBakerImpl() {
        }

        @Override
        public UnbakedModel getModel(ResourceLocation location) {
            // Unsafe, but my models do not contain overrides or parents.
            //noinspection DataFlowIssue
            return null;
        }

        @Override
        public BakedModel bake(ResourceLocation location, ModelState transform) {
            // Unsafe, but my models do not contain overrides or parents.
            return null;
        }
    }

    static final ModelBakerImpl modelBaker = new ModelBakerImpl();

    public static class CachedBakedModelSet {
        public final Supplier<BakedModel> R0;
        public final Supplier<BakedModel> R90;
        public final Supplier<BakedModel> R180;
        public final Supplier<BakedModel> R270;

        public CachedBakedModelSet(DynamicAtlas atlas, BlockModel model) {
            this.R0 = new CachedSupplier<>(() -> bakeModel(atlas, model, 0));
            this.R90 = new CachedSupplier<>(() -> bakeModel(atlas, model, 90));
            this.R180 = new CachedSupplier<>(() -> bakeModel(atlas, model, 180));
            this.R270 = new CachedSupplier<>(() -> bakeModel(atlas, model, 270));
        }

        public BakedModel get(int yRot) {
            return switch (yRot) {
                case 0 -> R0.get();
                case 90 -> R90.get();
                case 180 -> R180.get();
                case 270 -> R270.get();
                default -> throw new IllegalArgumentException("Invalid rotation: " + yRot);
            };
        }
    }

    public static BakedModel getModel(FurnitureData data, DynamicAtlas atlas) {
        return getModel(data, atlas, 0);
    }

    public static BakedModel getModel(FurnitureData data, DynamicAtlas atlas, int yRot) {
        String hash = data.getHash();
        return atlas.knownFurniture.computeIfAbsent(hash, k -> {
            BlockModel model = FurnitureModelFactory.getModel(data, atlas);
            return new CachedBakedModelSet(atlas, model);
        }).get(yRot);
    }

    private static BakedModel bakeModel(DynamicAtlas atlas, BlockModel model, int yRot) {
        return model.bake(modelBaker,
                material -> atlas == DynamicAtlas.BAKED ? material.sprite() : atlas.sprite,
                BlockModelRotation.by(0, yRot),
                LOCATION
        );
    }

    public static void bakeTexture(FurnitureData data) {
        DynamicAtlas atlas = DynamicAtlas.SCRATCH;
        getModel(data, atlas);

        NativeImage pixels = atlas.getPixels();
        if (pixels == null) return;

        BlockModel model = FurnitureModelFactory.getModel(data, atlas);
        for (int elementIndex = 0; elementIndex < model.getElements().size(); elementIndex++) {
            Map<Direction, int[]> bakedTexture = data.elements.get(elementIndex).bakedTexture;
            bakedTexture.clear();
            for (Map.Entry<Direction, BlockElementFace> entry : model.getElements().get(elementIndex).faces.entrySet()) {
                BlockFaceUV uv = entry.getValue().uv;

                int w = (int) ((uv.uvs[2] - uv.uvs[0]) * 16.0f);
                int h = (int) ((uv.uvs[3] - uv.uvs[1]) * 16.0f);
                try (NativeImage buffer = new NativeImage(w, h, false)) {
                    pixels.copyRect(buffer, (int) uv.uvs[0], (int) uv.uvs[1], 0, 0, w, h, false, false);
                    bakedTexture.put(entry.getKey(), buffer.getPixelsRGBA());
                }
            }
        }
    }
}
