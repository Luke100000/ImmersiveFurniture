package net.conczin.immersive_furniture.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.PreviewParticleEngine;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.client.model.MaterialRegistry;
import net.conczin.immersive_furniture.client.renderer.FurnitureBlockEntityRenderer;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public abstract class ArtisansWorkstationScreen extends Screen {
    public static final Component TITLE = Component.translatable("item.immersive_furniture.artisans_workstation");
    public static final ResourceLocation TEXTURE = Common.locate("textures/gui/gui.png");
    public static final int TEXTURE_SIZE = 256;
    protected Component error;
    protected long lastErrorTime = 0;
    protected long lastCriticalActionAttempt = 0;

    int windowWidth = 280;
    int windowHeight = 180;
    int leftPos;
    int topPos;

    public ArtisansWorkstationScreen() {
        super(TITLE);

        if (MaterialRegistry.INSTANCE.materials.isEmpty()) {
            new Thread(MaterialRegistry.INSTANCE::sync).start();
        }
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

    static void renderModel(GuiGraphics graphics, FurnitureData data, double x, double y, double size, float yaw, float pitch) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 100.0);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling((float) (size / Math.max(1.0, data.getSize() / 16.0) * 0.4)));
        graphics.pose().mulPose(new Quaternionf().rotateX(pitch).rotateY(yaw));
        Vec3 center = data.boundingBox().getCenter();
        graphics.pose().translate(-data.size.x / 2.0f, data.size.y / 2.0f - 0.5f + center.y / 16.0f, -data.size.z / 2.0f);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));
        renderModel(graphics, data, yaw, pitch, false);
        graphics.pose().popPose();
    }

    private static BakedModel lastBakedModel = null;

    static void renderModel(GuiGraphics graphics, FurnitureData data, float yaw, float pitch, boolean inEditor) {
        BakedModel bakedModel = FurnitureModelBaker.getAsyncModel(data, DynamicAtlas.SCRATCH);
        if (inEditor) {
            if (bakedModel == null) {
                bakedModel = lastBakedModel;
            } else {
                lastBakedModel = bakedModel;
            }
        }

        if (bakedModel != null) {
            FurnitureBlockEntityRenderer.renderFurniture(null, graphics.pose(), graphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, data, bakedModel, DynamicAtlas.SCRATCH);
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
                data.tick(level, player.getOnPos(), level.getRandom(), getParticleEngine(data)::addParticle, true, inEditor);
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
