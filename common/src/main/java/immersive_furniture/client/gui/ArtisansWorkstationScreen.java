package immersive_furniture.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import immersive_furniture.Common;
import immersive_furniture.screen.ArtisansWorkstationScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ArtisansWorkstationScreen extends AbstractContainerScreen<ArtisansWorkstationScreenHandler> {
    private static final ResourceLocation TEXTURE = Common.locate("textures/gui/container/inventory.png");

    public static final int TITLE_HEIGHT = 10;
    public static final int BASE_HEIGHT = 86;

    public int containerSize;

    public ArtisansWorkstationScreen(ArtisansWorkstationScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        imageHeight = BASE_HEIGHT + containerSize + TITLE_HEIGHT * 2;
        inventoryLabelY = containerSize + TITLE_HEIGHT;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics context, float delta, int mouseX, int mouseY) {
        //nop
    }

    protected void drawCustomBackground(GuiGraphics context) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        context.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, containerSize + TITLE_HEIGHT * 2, 512, 256);
        context.blit(TEXTURE, leftPos, topPos + containerSize + TITLE_HEIGHT * 2 - 4, 0, 222 - BASE_HEIGHT, imageWidth, BASE_HEIGHT, 512, 256);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        drawCustomBackground(context);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        super.init();

        titleLabelX = (imageWidth - font.width(title)) / 2;
    }
}
