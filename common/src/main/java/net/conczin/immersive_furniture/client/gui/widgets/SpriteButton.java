package net.conczin.immersive_furniture.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class SpriteButton extends StateImageButton {
    private ResourceLocation spriteLocation;
    private TextureAtlasSprite sprite;

    public SpriteButton(int x, int y, int width, int height, int xTexStart, int yTexStart, OnPress onPress) {
        super(x, y, width, height, xTexStart, yTexStart, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, onPress, Component.literal(""));
    }

    public ResourceLocation getSpriteLocation() {
        return spriteLocation;
    }

    public void setSpriteLocation(ResourceLocation spriteLocation) {
        this.spriteLocation = spriteLocation;
        if (spriteLocation != null) {
            MutableComponent message = Component.literal(Utils.capitalize(spriteLocation));
            setMessage(message);

            Component namespaceTooltip = Component.literal(Utils.capitalize(spriteLocation.getNamespace())).withStyle(ChatFormatting.GRAY);
            setTooltip(Tooltip.create(message.copy().append("\n").append(namespaceTooltip)));
        }
    }

    public void setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (sprite == null) {
            return;
        }

        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        // Draw the sprite as a flat square
        float size = width * 0.8f * (isHovered ? 1.1f : 1.0f);
        float x = getX() + width / 2.0f - size / 2.0f;
        float y = getY() + height / 2.0f - size / 2.0f;

        int blitOffset = 0;
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, x, y, (float) blitOffset).uv(sprite.getU0(), sprite.getV0()).endVertex();
        bufferbuilder.vertex(matrix4f, x, y + size, (float) blitOffset).uv(sprite.getU0(), sprite.getV1()).endVertex();
        bufferbuilder.vertex(matrix4f, x + size, y + size, (float) blitOffset).uv(sprite.getU1(), sprite.getV1()).endVertex();
        bufferbuilder.vertex(matrix4f, x + size, y, (float) blitOffset).uv(sprite.getU1(), sprite.getV0()).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }
}
