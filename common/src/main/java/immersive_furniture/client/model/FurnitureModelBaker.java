package immersive_furniture.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

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

    public static BakedModel getModel(FurnitureData data, DynamicAtlas atlas) {
        String hash = data.getHash();
        return atlas.knownFurniture.computeIfAbsent(hash, k -> {
            BlockModel model = FurnitureModelFactory.getModel(data, atlas);
            return model
                    .bake(modelBaker,
                            material -> atlas == DynamicAtlas.BAKED ? material.sprite() : atlas.sprite,
                            BlockModelRotation.by(0, 0),
                            LOCATION
                    );
        });
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

                int w = (int) (uv.uvs[2] - uv.uvs[0]);
                int h = (int) (uv.uvs[3] - uv.uvs[1]);
                try (NativeImage buffer = new NativeImage(w, h, false)) {
                    pixels.copyRect(buffer, (int) uv.uvs[0], (int) uv.uvs[1], 0, 0, w, h, false, false);
                    bakedTexture.put(entry.getKey(), buffer.getPixelsRGBA());
                }
            }
        }
    }
}
