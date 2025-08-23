package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.utils.CachedSupplier;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class FurnitureModelBaker {
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

        @SuppressWarnings("unused") // NeoForge compatibility
        public UnbakedModel getTopLevelModel(ModelResourceLocation location) {
            return null;
        }

        @SuppressWarnings("unused") // NeoForge compatibility
        public BakedModel bakeUncached(UnbakedModel model, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
            return null;
        }
    }

    private static final ModelBakerImpl modelBaker = new ModelBakerImpl();
    private final static RandomSource random = RandomSource.create();

    private static CompositeBakedModel bakeModel(DynamicAtlas atlas, CompositeBlockModel model, int yRot, int state) {
        Map<RenderType, BakedModel> bakedModels = new LinkedHashMap<>();
        for (RenderType type : model.models.get(state).keySet()) {
            bakedModels.put(type, bakeModel(atlas, model, type, yRot, state));
        }
        return new CompositeBakedModel(bakedModels);
    }

    private static BakedModel bakeModel(DynamicAtlas atlas, CompositeBlockModel model, RenderType type, int yRot, int state) {
        BakedModel bake = model.models.get(state).get(type).bake(modelBaker,
                material -> atlas == DynamicAtlas.BAKED || !material.texture().getNamespace().equals("immersive_furniture") ? material.sprite() : atlas.sprite,
                BlockModelRotation.by(0, yRot)
        );

        // Copy color and emission from elements
        for (BakedQuad quad : bake.getQuads(null, null, random)) {
            int[] vertices = quad.getVertices();
            for (int i = 0; i < vertices.length; i += 8) {
                FurnitureData.Element element = model.getElement(quad.getTintIndex());
                vertices[i + 3] = element.color;
                vertices[i + 6] = (element.emission << 20) | (element.emission << 4);
            }
        }

        return bake;
    }

    /**
     * Maintains a cached set of baked models for each rotation and state.
     */
    public static class CachedBakedModelSet {
        private static final Supplier<CompositeBakedModel> EMPTY = () -> new CompositeBakedModel(Map.of());

        public final Map<Integer, Supplier<CompositeBakedModel>> variations = new HashMap<>();

        public CachedBakedModelSet(DynamicAtlas atlas, CompositeBlockModel model) {
            for (int rot = 0; rot < 360; rot += 90) {
                for (Integer state : model.models.keySet()) {
                    int finalRot = rot;
                    variations.put(rot + state, new CachedSupplier<>(() -> bakeModel(atlas, model, finalRot, state)));
                }
            }
        }

        public CompositeBakedModel get(int yRot, int state) {
            return variations.getOrDefault(yRot + state, variations.getOrDefault(yRot, EMPTY)).get();
        }
    }

    /**
     * Builds and bake in the background, returns only if already done.
     */
    public static CompositeBakedModel getAsyncModel(FurnitureData data, DynamicAtlas atlas, int state) {
        String hash = data.getHash();
        if (atlas.knownFurniture.containsKey(hash)) {
            return getModel(data, data.getHash(), atlas, 0, state, false);
        } else if (!atlas.asyncRequestedFurniture.contains(hash)) {
            atlas.asyncRequestedFurniture.add(hash);
            Common.EXECUTOR.execute(() -> {
                getModel(data, hash, atlas, 0, state, false);
                atlas.asyncRequestedFurniture.remove(hash);
            });
        }
        return null;
    }

    /**
     * Builds the default state usually used for items or previews.
     */
    public static CompositeBakedModel getModel(FurnitureData data, DynamicAtlas atlas) {
        return getModel(data, data.getHash(), atlas, 0, 0, true);
    }

    /**
     * Build, bake, and cache a specific state and rotation.
     * Can return null if atlas is full unless forced.
     */
    public static CompositeBakedModel getModel(FurnitureData data, String hash, DynamicAtlas atlas, int yRot, int state, boolean force) {
        CachedBakedModelSet cachedBakedModelSet = atlas.knownFurniture.get(hash);
        boolean exist = cachedBakedModelSet != null;

        // The atlas is full, cannot continue
        if (!force && !exist && atlas.isFull()) {
            return null;
        }

        if (exist) {
            atlas.uploadIfDirty();
            return cachedBakedModelSet.get(yRot, state);
        } else {
            float previousUsage = atlas.getUsage();
            CompositeBlockModel model = FurnitureModelFactory.getModel(data, atlas);
            atlas.uploadIfDirty();

            // Only cache if the hash is still the same
            CachedBakedModelSet modelSet = new CachedBakedModelSet(atlas, model);
            if (data.getHash().equals(hash)) {
                atlas.knownFurniture.put(hash, modelSet);
            }

            // Only add when forced or the atlas had space
            if (force || !atlas.isFull() && atlas.getUsage() >= previousUsage) {
                return modelSet.get(yRot, state);
            } else {
                atlas.knownFurniture.remove(hash);
            }
        }
        return null;
    }
}
