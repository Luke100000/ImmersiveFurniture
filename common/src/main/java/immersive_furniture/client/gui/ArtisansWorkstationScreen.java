package immersive_furniture.client.gui;

import immersive_furniture.Common;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ArtisansWorkstationScreen extends Screen {
    private static final Component TITLE = Component.translatable("item.immersive_furniture.artisans_workstation");
    private static final ResourceLocation TEXTURE = Common.locate("textures/gui/gui.png");

    protected int imageWidth = 280;
    protected int imageHeight = 180;
    protected int leftPos;
    protected int topPos;

    public ArtisansWorkstationScreen() {
        super(TITLE);
    }

    protected void drawRectangle(GuiGraphics context, int x, int y, int h, int w) {
        int originY = 0;
        int originX = 0;

        //corners
        context.blit(TEXTURE, x, y, originX, originY, 16, 16, 128, 128);
        context.blit(TEXTURE, x + w - 16, y, originX + 32, originY, 16, 16, 128, 128);
        context.blit(TEXTURE, x + w - 16, y + h - 16, originX + 32, originY + 32, 16, 16, 128, 128);
        context.blit(TEXTURE, x, y + h - 16, originX, originY + 32, 16, 16, 128, 128);

        //edges
        context.blit(TEXTURE, x + 16, y, w - 32, 16, originX + 16, originY, 16, 16, 128, 128);
        context.blit(TEXTURE, x + 16, y + h - 16, w - 32, 16, originX + 16, originY + 32, 16, 16, 128, 128);
        context.blit(TEXTURE, x, y + 16, 16, h - 32, originX, originY + 16, 16, 16, 128, 128);
        context.blit(TEXTURE, x + w - 16, y + 16, 16, h - 32, originX + 32, originY + 16, 16, 16, 128, 128);

        //center
        context.blit(TEXTURE, x + 16, y + 16, w - 32, h - 32, originX + 16, originY + 16, 16, 16, 128, 128);
    }

    @Override
    public void renderBackground(GuiGraphics context) {
        super.renderBackground(context);

        drawRectangle(context, this.leftPos, this.topPos, this.imageHeight, this.imageWidth);


    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }
}
