package immersive_furniture.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import immersive_furniture.client.model.MaterialSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class MaterialButton extends StateImageButton {
    MaterialSource material;

    public MaterialButton(int x, int y, int width, int height, int xTexStart, int yTexStart, OnPress onPress) {
        super(x, y, width, height, xTexStart, yTexStart, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, onPress, Component.literal(""));
    }

    public MaterialSource getMaterial() {
        return material;
    }

    public void setMaterial(MaterialSource material) {
        this.material = material;
        if (material != null) {
            setMessage(material.name());
            setTooltip(Tooltip.create(material.name()));
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (material == null) {
            return;
        }

        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        // TODO: Sprites do not necessarily have to be on the block atlas!
        TextureAtlasSprite up = material.up().sprite();
        TextureAtlasSprite north = material.north().sprite();
        TextureAtlasSprite east = material.east().sprite();

        // Cube vertices in isometric perspective
        float size = width * 0.4f * (mouseX > getX() && mouseX < getX() + width && mouseY > getY() && mouseY < getY() + height ? 1.1f : 1.0f);
        float x = getX() + width / 2.0f;
        float y = getY() + height / 2.0f - size / 4 * 2.5f;

        RenderSystem.setShaderTexture(0, up.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        float lx = x - size;
        float rx = x + size;
        float py = y - size / 2;
        float cy = y + size / 2;
        float by = y + size / 4 * 5;
        float ry = y + size / 4 * 7;

        // Up
        bufferBuilder.vertex(matrix4f, x, py, 0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(up.getU0(), up.getV1()).endVertex();
        bufferBuilder.vertex(matrix4f, lx, y, 0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(up.getU1(), up.getV1()).endVertex();
        bufferBuilder.vertex(matrix4f, x, cy, 0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(up.getU1(), up.getV0()).endVertex();
        bufferBuilder.vertex(matrix4f, rx, y, 0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(up.getU0(), up.getV0()).endVertex();

        // North
        float ng = 0.8f;
        bufferBuilder.vertex(matrix4f, rx, y, 0).color(ng, ng, ng, 1.0f).uv(north.getU1(), north.getV0()).endVertex();
        bufferBuilder.vertex(matrix4f, x, cy, 0).color(ng, ng, ng, 1.0f).uv(north.getU0(), north.getV0()).endVertex();
        bufferBuilder.vertex(matrix4f, x, ry, 0).color(ng, ng, ng, 1.0f).uv(north.getU0(), north.getV1()).endVertex();
        bufferBuilder.vertex(matrix4f, rx, by, 0).color(ng, ng, ng, 1.0f).uv(north.getU1(), north.getV1()).endVertex();

        // East
        float eg = 0.6f;
        bufferBuilder.vertex(matrix4f, x, cy, 0).color(eg, eg, eg, 1.0f).uv(east.getU1(), east.getV0()).endVertex();
        bufferBuilder.vertex(matrix4f, lx, y, 0).color(eg, eg, eg, 1.0f).uv(east.getU0(), east.getV0()).endVertex();
        bufferBuilder.vertex(matrix4f, lx, by, 0).color(eg, eg, eg, 1.0f).uv(east.getU0(), east.getV1()).endVertex();
        bufferBuilder.vertex(matrix4f, x, ry, 0).color(eg, eg, eg, 1.0f).uv(east.getU1(), east.getV1()).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}
