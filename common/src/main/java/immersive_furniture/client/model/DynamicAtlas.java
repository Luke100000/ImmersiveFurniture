package immersive_furniture.client.model;

import immersive_furniture.Common;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicAtlas extends DynamicTexture {
    public static final DynamicAtlas BAKED = new DynamicAtlas(512, "baked");
    public static final DynamicAtlas ENTITY = new DynamicAtlas(512, "entity");
    public static final DynamicAtlas SCRATCH = new DynamicAtlas(512, "scratch");

    boolean full;
    int allocated;
    int size;
    ResourceLocation location;
    List<Quad> quads = new LinkedList<>();

    public Map<String, FurnitureModelBaker.CachedBakedModelSet> knownFurniture = new ConcurrentHashMap<>();

    public final TextureAtlasSpriteAccessor sprite;

    public DynamicAtlas(int size, String name) {
        super(size, size, false);

        this.size = size;

        // Register the texture with the Minecraft texture manager
        location = Common.locate("immersive_furniture_atlas/" + name);
        Minecraft.getInstance().getTextureManager().register(location, this);

        SpriteContents contents = new SpriteContents(location, new FrameSize(size, size), Objects.requireNonNull(getPixels()), AnimationMetadataSection.EMPTY);
        sprite = new TextureAtlasSpriteAccessor(location, contents, size, size, 0, 0);

        clear();
    }

    public static void boostrap() {
        // No-op
    }

    synchronized public Quad allocate(int w, int h) {
        for (Quad quad : quads) {
            if (quad.w >= w && quad.h >= h) {
                quads.remove(quad);
                if (quad.w > w) {
                    quads.add(new Quad(quad.x + w, quad.y, quad.w - w, h));
                }
                if (quad.h > h) {
                    quads.add(new Quad(quad.x, quad.y + h, w, quad.h - h));
                }
                if (quad.w > w && quad.h > h) {
                    quads.add(new Quad(quad.x + w, quad.y + h, quad.w - w, quad.h - h));
                }
                allocated += w * h;
                return new Quad(quad.x, quad.y, w, h);
            }
        }
        full = true;
        return new Quad(0, 0, 0, 0);
    }

    synchronized public void clear() {
        quads.clear();
        quads.add(new Quad(0, 0, size, size));
        allocated = 0;
        full = false;
        knownFurniture.clear();
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

    public record Quad(int x, int y, int w, int h) {
    }
}
