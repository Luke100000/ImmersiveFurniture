package immersive_furniture.client.model;

import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.utils.CachedSupplier;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

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
        return getModel(data, atlas, 0, true);
    }

    public static BakedModel getModel(FurnitureData data, DynamicAtlas atlas, int yRot, boolean force) {
        String hash = data.getHash();
        boolean exist = atlas.knownFurniture.containsKey(hash);

        // The atlas is full, cannot continue
        if (!force && !exist && atlas.isFull()) {
            return null;
        }

        if (exist) {
            return atlas.knownFurniture.get(hash).get(yRot);
        } else {
            BlockModel model = FurnitureModelFactory.getModel(data, atlas);
            CachedBakedModelSet modelSet = new CachedBakedModelSet(atlas, model);
            atlas.knownFurniture.put(hash, modelSet);

            // Only add when forced or the atlas had space
            if (force || !atlas.isFull()) {
                return modelSet.get(yRot);
            } else {
                atlas.knownFurniture.remove(hash);
            }
        }
        return null;
    }

    private static BakedModel bakeModel(DynamicAtlas atlas, BlockModel model, int yRot) {
        return model.bake(modelBaker,
                material -> atlas == DynamicAtlas.BAKED ? material.sprite() : atlas.sprite,
                BlockModelRotation.by(0, yRot),
                LOCATION
        );
    }
}
