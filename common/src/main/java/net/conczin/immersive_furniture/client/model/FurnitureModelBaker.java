package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.CommonClient;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.utils.CachedSupplier;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
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

        @SuppressWarnings("unused")
        public BakedModel bake(ResourceLocation var1, ModelState var2, Function<Material, TextureAtlasSprite> var3) {
            return null;
        }

        @SuppressWarnings("unused")
        public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
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

    private final static Executor executor = Executors.newSingleThreadExecutor();

    public static BakedModel getAsyncModel(FurnitureData data, DynamicAtlas atlas) {
        String hash = data.getHash();
        if (atlas.knownFurniture.containsKey(hash)) {
            return getModel(data, atlas, 0, false);
        } else {
            if (!atlas.asyncRequestedFurniture.contains(hash)) {
                atlas.asyncRequestedFurniture.add(hash);
                executor.execute(() -> {
                    if (getModel(data, atlas, 0, false) == null) {
                        atlas.asyncRequestedFurniture.remove(hash);
                    }
                });
            }
            return null;
        }
    }

    public static BakedModel getModel(FurnitureData data, DynamicAtlas atlas) {
        return getModel(data, atlas, 0, true);
    }

    public static BakedModel getModel(FurnitureData data, DynamicAtlas atlas, int yRot, boolean force) {
        String hash = data.getHash();
        CachedBakedModelSet cachedBakedModelSet = atlas.knownFurniture.get(hash);
        boolean exist = cachedBakedModelSet != null;

        // The atlas is full, cannot continue
        if (!force && !exist && atlas.isFull()) {
            return null;
        }

        if (exist) {
            atlas.uploadIfDirty();
            return cachedBakedModelSet.get(yRot);
        } else {
            float previousUsage = atlas.getUsage();
            BlockModel model = FurnitureModelFactory.getModel(data, atlas);
            atlas.uploadIfDirty();

            CachedBakedModelSet modelSet = new CachedBakedModelSet(atlas, model);
            atlas.knownFurniture.put(hash, modelSet);

            // Only add when forced or the atlas had space
            if (force || !atlas.isFull() && atlas.getUsage() >= previousUsage) {
                return modelSet.get(yRot);
            } else {
                atlas.knownFurniture.remove(hash);
            }
        }
        return null;
    }

    private final static RandomSource random = RandomSource.create();

    private static BakedModel bakeModel(DynamicAtlas atlas, BlockModel model, int yRot) {
        BakedModel bake = model.bake(modelBaker,
                material -> atlas == DynamicAtlas.BAKED || !material.texture().getNamespace().equals("immersive_furniture") ? material.sprite() : atlas.sprite,
                BlockModelRotation.by(0, yRot),
                LOCATION
        );

        // Copy color (stored in tint index)
        for (BakedQuad quad : bake.getQuads(null, null, random)) {
            int[] vertices = quad.getVertices();
            for (int i = 0; i < vertices.length; i += 8) {
                vertices[i + 3] = quad.getTintIndex();
            }
        }

        return bake;
    }
}
