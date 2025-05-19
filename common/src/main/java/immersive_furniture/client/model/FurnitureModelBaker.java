package immersive_furniture.client.model;

import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

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

    public static BakedModel getModel(FurnitureData data) {
        DynamicAtlas atlas = DynamicAtlas.SCRATCH;
        // TODO: Cache
        return FurnitureModelFactory.getModel(data, atlas)
                .bake(modelBaker,
                        material -> atlas == DynamicAtlas.ENTITY ? material.sprite() : atlas.sprite,
                        BlockModelRotation.by(0, 0),
                        LOCATION
                );
    }
}
