package net.conczin.immersive_furniture.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import net.conczin.immersive_furniture.Client;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.utils.ScopedLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class DynamicAtlas extends DynamicTexture {
    public static final DynamicAtlas BAKED = new DynamicAtlas(Client.getBakedAtlasSize(), "baked");
    public static DynamicAtlas ENTITY = new DynamicAtlas(Config.getInstance().getEntityAtlasSize(), "entity");
    public static final DynamicAtlas SCRATCH = new DynamicAtlas(Config.getInstance().getScratchAtlasSize(), "scratch");

    public final ScopedLock bakeLock = new ScopedLock();

    int lastStateId;
    boolean full;
    int allocated;
    int size;
    ResourceLocation location;
    List<Quad> quads = new LinkedList<>();

    public Map<String, FurnitureModelBaker.CachedBakedModelSet> knownFurniture = new ConcurrentHashMap<>();
    public Set<String> asyncRequestedFurniture = new ConcurrentSkipListSet<>();

    public final TextureAtlasSpriteAccessor sprite;
    private boolean dirty = false;

    public DynamicAtlas(int size, String name) {
        super(Math.min(size, Client.getMaxAtlasSize()), Math.min(size, Client.getMaxAtlasSize()), false);

        this.size = Objects.requireNonNull(getPixels()).getWidth();

        // Register the texture with the Minecraft texture manager
        location = Common.locate("immersive_furniture_atlas/" + name);
        Minecraft.getInstance().getTextureManager().register(location, this);

        SpriteContents contents = new SpriteContents(location, new FrameSize(this.size, this.size), Objects.requireNonNull(getPixels()), ResourceMetadata.EMPTY);
        sprite = new TextureAtlasSpriteAccessor(location, contents, this.size, this.size, 0, 0);

        clear();
    }

    public static void boostrap() {
        // No-op
    }

    public static void resizeEntity(int size) {
        ENTITY = new DynamicAtlas(size, "entity");
    }

    public Quad allocate(int w, int h) {
        try (var ignored = bakeLock.acquire()) {
            // Find the best fitting quad
            Quad best = getBestQuad(w, h);

            // And split it
            if (best != null) {
                quads.remove(best);

                // Split
                int dw = best.w - w;
                int dh = best.h - h;

                if (dw > dh) {
                    if (dw > 0) quads.add(new Quad(best.x + w, best.y, dw, best.h));
                    if (dh > 0) quads.add(new Quad(best.x, best.y + h, w, dh));
                } else {
                    if (dw > 0) quads.add(new Quad(best.x + w, best.y, dw, h));
                    if (dh > 0) quads.add(new Quad(best.x, best.y + h, best.w, dh));
                }

                allocated += w * h;
                return new Quad(best.x, best.y, w, h);
            }

            full = true;
            return new Quad(0, 0, 0, 0);
        }
    }

    private Quad getBestQuad(int w, int h) {
        Quad best = null;
        int bestLoss = Integer.MAX_VALUE;
        for (Quad quad : quads) {
            if (quad.w >= w && quad.h >= h) {
                int waste = (quad.w * quad.h) - (w * h);
                float aspect = (float) Math.max(quad.w, quad.h) / Math.min(quad.w, quad.h);
                int loss = (int) (waste * aspect);

                if (loss < bestLoss) {
                    best = quad;
                    bestLoss = loss;
                }
            }
        }
        return best;
    }

    public void clear() {
        try (var ignored = bakeLock.acquire()) {
            quads.clear();
            quads.add(new Quad(0, 0, size, size));
            allocated = 0;
            full = false;
            knownFurniture.clear();
            asyncRequestedFurniture.clear();
        }
    }

    public boolean isFull() {
        return full;
    }

    public float getUsage() {
        return (float) allocated / (size * size);
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public int getSize() {
        return size;
    }

    public void setDirty() {
        this.dirty = true;
        lastStateId++;
    }

    public void uploadIfDirty() {
        if (dirty && RenderSystem.isOnRenderThreadOrInit()) {
            this.upload();
            dirty = false;
        }
    }

    public int getLastStateId() {
        return lastStateId;
    }

    public record Quad(int x, int y, int w, int h) {
    }
}
