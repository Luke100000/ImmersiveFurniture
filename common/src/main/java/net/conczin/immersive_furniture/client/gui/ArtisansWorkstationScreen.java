package net.conczin.immersive_furniture.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_furniture.Client;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.PreviewParticleEngine;
import net.conczin.immersive_furniture.client.model.CompositeBakedModel;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.client.model.TransparencyManager;
import net.conczin.immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public abstract class ArtisansWorkstationScreen extends Screen {
    public static final Component TITLE = Component.translatable("item.immersive_furniture.artisans_workstation");

    public static final ResourceLocation TEXTURE = Common.locate("textures/gui/gui.png");
    public static final int TEXTURE_SIZE = 256;

    protected Component error;
    protected long lastErrorTime = 0;
    protected long lastCriticalActionAttempt = 0;

    private static final int MIN_WINDOW_WIDTH = 280;
    private static final int MIN_WINDOW_HEIGHT = 180;
    private static final int HORIZONTAL_MARGIN = 64;
    private static final int VERTICAL_MARGIN = 48;
    private static final int TAB_SPACE = 24;

    protected enum WindowSize {
        ORIGINAL,
        LARGE,
        FULLSCREEN;

        WindowSize cycle() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    protected static WindowSize windowSize = WindowSize.LARGE;
    int windowWidth = MIN_WINDOW_WIDTH;
    int windowHeight = MIN_WINDOW_HEIGHT;
    int leftPos;
    int topPos;

    // TODO: There is no button yet in the library screen
    boolean nightMode = false;
    public int currentState = 0;

    public ArtisansWorkstationScreen() {
        super(TITLE);
    }

    protected void cycleWindowSize() {
        windowSize = windowSize.cycle();
    }

    protected int getWindowSizeButtonX() {
        return windowSize == WindowSize.FULLSCREEN ? leftPos + windowWidth - 19 : leftPos + windowWidth + 1;
    }

    protected Component getWindowSizeTooltip() {
        String name = switch (windowSize) {
            case ORIGINAL -> "Original size";
            case LARGE -> "Large";
            case FULLSCREEN -> "Fullscreen";
        };
        return Component.translatableWithFallback("gui.immersive_furniture.window_size." + windowSize.name().toLowerCase(Locale.ROOT), name);
    }

    protected void drawRectangle(GuiGraphics graphics, int x, int y, int w, int h) {
        drawRectangle(graphics, x, y, w, h, 0, 0);
    }

    protected void drawRectangle(GuiGraphics graphics, int x, int y, int w, int h, int originY, int originX) {
        //corners
        graphics.blit(TEXTURE, x, y, originX, originY, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(TEXTURE, x + w - 16, y, originX + 32, originY, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(TEXTURE, x + w - 16, y + h - 16, originX + 32, originY + 32, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(TEXTURE, x, y + h - 16, originX, originY + 32, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

        //edges
        graphics.blit(TEXTURE, x + 16, y, w - 32, 16, originX + 16, originY, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(TEXTURE, x + 16, y + h - 16, w - 32, 16, originX + 16, originY + 32, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(TEXTURE, x, y + 16, 16, h - 32, originX, originY + 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(TEXTURE, x + w - 16, y + 16, 16, h - 32, originX + 32, originY + 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

        //center
        graphics.blit(TEXTURE, x + 16, y + 16, w - 32, h - 32, originX + 16, originY + 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void removed() {
        super.removed();

        // The scratch atlas only serves these screens, shrink it back and drop their previews
        DynamicAtlas.tryResizeScratch(Config.getInstance().getScratchAtlasSize());
    }

    void renderModel(GuiGraphics graphics, FurnitureData data, double x, double y, double width, double height, float yaw, float pitch, boolean anyPitch) {
        // Library content is unchecked, refuse absurd models instead of growing the atlas for them
        long maxPixels = (long) Client.getMaxAtlasSize() * Client.getMaxAtlasSize() / 10;
        if (data.getAtlasPixels() > maxPixels) return;

        // Fit the projected mesh into the frame, at any yaw since previews spin around the y-axis
        AABB bounds = data.meshBoundingBox();
        double footprintRadius = Math.hypot(bounds.getXsize(), bounds.getZsize()) / 32.0;
        double halfMeshHeight = bounds.getYsize() / 32.0;
        double halfHeight = anyPitch
                ? Math.hypot(halfMeshHeight, footprintRadius)
                : halfMeshHeight * Math.abs(Math.cos(pitch)) + footprintRadius * Math.abs(Math.sin(pitch));
        float scale = (float) Math.min(
                width * 0.45 / Math.max(1.0, footprintRadius),
                height * 0.45 / Math.max(1.0, halfHeight)
        );

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 100.0);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(scale));
        graphics.pose().mulPose(new Quaternionf().rotateX(pitch).rotateY(yaw));
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));
        Vec3 center = bounds.getCenter();
        graphics.pose().translate(-center.x / 16.0f, -center.y / 16.0f, -center.z / 16.0f);
        renderModel(graphics, data, yaw, pitch, false);
        renderFloorGrid(graphics, bounds, scale);
        graphics.pose().popPose();
    }

    private static final float GRID_MARGIN = 0.25f;
    private static final float GRID_SPACING = 0.25f;
    private static final float GRID_SEGMENT = 1 / 16.0f;

    private void renderFloorGrid(GuiGraphics graphics, AABB bounds, float scale) {
        float minX = (float) bounds.minX / 16.0f, maxX = (float) bounds.maxX / 16.0f;
        float minZ = (float) bounds.minZ / 16.0f, maxZ = (float) bounds.maxZ / 16.0f;
        float y = (float) bounds.minY / 16.0f - 0.002f;
        float halfWidth = 0.5f / scale;

        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());

        // Lines along the z-axis, then along the x-axis
        for (int axis = 0; axis < 2; axis++) {
            float lineMin = axis == 0 ? minX : minZ, lineMax = axis == 0 ? maxX : maxZ;
            float runMin = axis == 0 ? minZ : minX, runMax = axis == 0 ? maxZ : maxX;
            float first = (float) Math.ceil((lineMin - GRID_MARGIN) / GRID_SPACING) * GRID_SPACING;
            for (float line = first; line <= lineMax + GRID_MARGIN; line += GRID_SPACING) {
                boolean blockLine = Math.abs(line - Math.round(line)) < 0.001f;
                float alpha = blockLine ? 0.2f : 0.05f;
                for (float run = runMin - GRID_MARGIN; run < runMax + GRID_MARGIN; run += GRID_SEGMENT) {
                    float next = Math.min(run + GRID_SEGMENT, runMax + GRID_MARGIN);
                    float a0 = alpha * getGridFade(line, lineMin, lineMax, run, runMin, runMax);
                    float a1 = alpha * getGridFade(line, lineMin, lineMax, next, runMin, runMax);
                    if (axis == 0) {
                        gridQuad(consumer, matrix, line - halfWidth, line + halfWidth, run, next, y, a0, a1, false);
                    } else {
                        gridQuad(consumer, matrix, run, next, line - halfWidth, line + halfWidth, y, a0, a1, true);
                    }
                }
            }
        }
    }

    private static float getGridFade(float line, float lineMin, float lineMax, float run, float runMin, float runMax) {
        float dx = Math.max(0, Math.max(lineMin - line, line - lineMax));
        float dz = Math.max(0, Math.max(runMin - run, run - runMax));
        return Math.max(0, 1 - (float) Math.sqrt(dx * dx + dz * dz) / GRID_MARGIN);
    }

    private static void gridQuad(VertexConsumer consumer, Matrix4f matrix, float x0, float x1, float z0, float z1, float y, float a0, float a1, boolean runAlongX) {
        float[][] corners = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        float[] alphas = runAlongX ? new float[]{a0, a0, a1, a1} : new float[]{a0, a1, a1, a0};
        for (int i = 0; i < 4; i++) {
            consumer.vertex(matrix, corners[i][0], y, corners[i][1]).color(1.0f, 1.0f, 1.0f, alphas[i]).endVertex();
        }
        for (int i = 3; i >= 0; i--) {
            consumer.vertex(matrix, corners[i][0], y, corners[i][1]).color(1.0f, 1.0f, 1.0f, alphas[i]).endVertex();
        }
    }

    private static CompositeBakedModel lastBakedModel = null;

    void renderModel(GuiGraphics graphics, FurnitureData data, float yaw, float pitch, boolean inEditor) {
        TransparencyManager.heySodiumImInUse(data);

        if (inEditor) TransparencyManager.prepare(data);
        CompositeBakedModel bakedModel = FurnitureModelBaker.getAsyncModel(data, DynamicAtlas.SCRATCH, currentState);
        if (inEditor) {
            if (bakedModel == null) {
                bakedModel = lastBakedModel;
            } else {
                lastBakedModel = bakedModel;
            }
        }

        if (bakedModel != null) {
            int light = nightMode ? data.lightLevel : 15;
            int packedLight = light << 20 | light << 4 | light;
            FurnitureBlockEntityRenderer.renderFurniture(
                    null,
                    graphics.pose(),
                    graphics.bufferSource(),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    bakedModel,
                    DynamicAtlas.SCRATCH
            );

            // Draw example items
            assert this.minecraft != null;
            ItemRenderer itemRenderer = this.minecraft.getItemRenderer();
            FurnitureBlockEntityRenderer.drawItems(itemRenderer, this.minecraft.level, null, currentState, graphics.pose(), graphics.bufferSource(), packedLight, OverlayTexture.NO_OVERLAY, data);
        }

        // Particles
        long time = System.currentTimeMillis() / 50;
        float partialTicks = System.currentTimeMillis() % 50 / 50.0f;
        if (time != data.lastTick) {
            data.lastTick = time;

            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if (level == null || player == null) return;

            // We use the animation tick, which is a triangle distribution based on distance to the player,
            // 0.2f is roughly 4 blocks away
            if (level.getRandom().nextFloat() < 0.2f) {
                data.tick(level, player.getOnPos(), currentState, null, level.getRandom(), getParticleEngine(data)::addParticle, true, inEditor);
            }

            getParticleEngine(data).tick();
        }

        getParticleEngine(data).renderParticles(graphics, yaw, pitch, partialTicks);

        graphics.flush();
    }

    void line(GuiGraphics graphics, float x0, float y0, float z0, float x1, float y1, float z1, float width, boolean overlay, float r, float g, float b, float a) {
        float length = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        float nx = (y1 - y0) / length * width * 0.5f;
        float ny = (x0 - x1) / length * width * 0.5f;

        Matrix4f matrix4f = graphics.pose().last().pose();
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(overlay ? RenderType.guiOverlay() : RenderType.gui());

        float z = 2.0f;

        vertexConsumer.vertex(matrix4f, x1 - nx + 0.5f, y1 - ny + 0.5f, z1 + z).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix4f, x1 + nx + 0.5f, y1 + ny + 0.5f, z1 + z).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix4f, x0 + nx + 0.5f, y0 + ny + 0.5f, z0 + z).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix4f, x0 - nx + 0.5f, y0 - ny + 0.5f, z0 + z).color(r, g, b, a).endVertex();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        clearWidgets();

        super.init();

        switch (windowSize) {
            case ORIGINAL -> {
                this.windowWidth = MIN_WINDOW_WIDTH;
                this.windowHeight = MIN_WINDOW_HEIGHT;
            }
            case LARGE -> {
                this.windowWidth = Math.max(MIN_WINDOW_WIDTH, this.width - HORIZONTAL_MARGIN * 2);
                this.windowHeight = Math.max(MIN_WINDOW_HEIGHT, this.height - VERTICAL_MARGIN * 2);
            }
            case FULLSCREEN -> {
                this.windowWidth = this.width;
                this.windowHeight = this.height - TAB_SPACE;
                this.leftPos = 0;
                this.topPos = TAB_SPACE;
                return;
            }
        }
        this.leftPos = (this.width - this.windowWidth) / 2;
        this.topPos = (this.height - this.windowHeight) / 2;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        return super.addRenderableWidget(widget);
    }

    private static final Map<Object, PreviewParticleEngine> objectToParticleEngine =
            Collections.synchronizedMap(new LinkedHashMap<>(30, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Object, PreviewParticleEngine> eldest) {
                    return size() > 30;
                }
            });

    public static PreviewParticleEngine getParticleEngine(FurnitureData data) {
        return objectToParticleEngine.computeIfAbsent(data, d -> new PreviewParticleEngine());
    }

    public void clearError() {
        this.error = null;
    }

    public void setError(String text) {
        this.error = Component.translatable(text);
        this.lastErrorTime = System.currentTimeMillis();
    }

    protected void renderError(GuiGraphics graphics, int y) {
        if (error == null) return;

        graphics.fill(width / 2 - 80, y - 3, width / 2 + 80, y + 10, 0x80000000);
        graphics.drawCenteredString(font, error, width / 2, y, 0xFFFF0000);

        if (System.currentTimeMillis() - lastErrorTime > 5000) {
            clearError();
        }
    }
}
