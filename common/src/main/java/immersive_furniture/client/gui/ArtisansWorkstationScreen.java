package immersive_furniture.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import immersive_furniture.Common;
import immersive_furniture.client.model.DynamicAtlas;
import immersive_furniture.client.model.FurnitureModelBaker;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;


public abstract class ArtisansWorkstationScreen extends Screen {
    public static final Component TITLE = Component.translatable("item.immersive_furniture.artisans_workstation");
    public static final ResourceLocation TEXTURE = Common.locate("textures/gui/gui.png");
    public static final int TEXTURE_SIZE = 256;

    int windowWidth = 280;
    int windowHeight = 180;
    int leftPos;
    int topPos;

    public ArtisansWorkstationScreen() {
        super(TITLE);
    }

    protected void drawRectangle(GuiGraphics graphics, int x, int y, int h, int w) {
        int originY = 0;
        int originX = 0;

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

    static void renderModel(GuiGraphics graphics, FurnitureData data) {
        Lighting.setupFor3DItems();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel bakedModel = FurnitureModelBaker.getModel(data);
        ResourceLocation location = DynamicAtlas.SCRATCH.getLocation();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.entityCutout(location));
        blockRenderer.getModelRenderer().renderModel(graphics.pose().last(), consumer, null, bakedModel, 1.0f, 1.0f, 1.0f, 0xFFFFFF, OverlayTexture.NO_OVERLAY);
    }

    void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, float width, float r, float g, float b, float a) {
        float length = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        float nx = (y1 - y0) / length * width * 0.5f;
        float ny = (x0 - x1) / length * width * 0.5f;
        int z = 30;

        Matrix4f matrix4f = graphics.pose().last().pose();
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());

        vertexConsumer.vertex(matrix4f, x1 - nx + 0.5f, y1 - ny + 0.5f, z).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix4f, x1 + nx + 0.5f, y1 + ny + 0.5f, z).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix4f, x0 + nx + 0.5f, y0 + ny + 0.5f, z).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix4f, x0 - nx + 0.5f, y0 - ny + 0.5f, z).color(r, g, b, a).endVertex();

        graphics.flush();
    }

    void checkerPlane(GuiGraphics graphics) {
        // TODO: Let user switch between different checker shapes
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(matrix4f, -0.25f, 0.0f, -0.25f).uv(244.0f / 256.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, -0.25f, 0.0f, 1.25f).uv(244.0f / 256.0f, 12.0f / 256.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, 1.25f, 0.0f, 1.25f).uv(1.0f, 12.0f / 256.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, 1.25f, 0.0f, -0.25f).uv(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.windowWidth) / 2;
        this.topPos = (this.height - this.windowHeight) / 2;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        return super.addRenderableWidget(widget);
    }
}
