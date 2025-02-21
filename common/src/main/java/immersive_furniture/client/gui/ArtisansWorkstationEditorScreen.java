package immersive_furniture.client.gui;

import immersive_furniture.client.gui.components.MaterialsComponent;
import immersive_furniture.client.gui.components.ModelComponent;
import immersive_furniture.client.gui.widgets.StateImageButton;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ArtisansWorkstationEditorScreen extends ArtisansWorkstationScreen {
    public static int TOOLS_WIDTH = 100;

    float camYaw = (float) (Math.PI / 4);
    float camPitch = (float) (Math.PI / 4);

    public FurnitureData data = new FurnitureData();
    public FurnitureData.Element selectedElement = null;

    MaterialsComponent materialsComponent = new MaterialsComponent(this);
    ModelComponent modelComponent = new ModelComponent(this);

    Page currentPage = Page.MODEL;

    public enum Page {
        MODEL,
        MATERIALS,
        SHAPES,
        EFFECTS,
        INVENTORY,
        SETTINGS
    }

    @Override
    protected void init() {
        super.init();

        clearWidgets();

        minecraft = Minecraft.getInstance();

        switch (currentPage) {
            case MODEL -> modelComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
            case MATERIALS -> materialsComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
        }

        // Page buttons
        int x = 0;
        for (Page page : Page.values()) {
            StateImageButton button = pagePageButton(page, x);
            addRenderableWidget(button);
            x += 26;
        }
    }

    private StateImageButton pagePageButton(Page page, int x) {
        MutableComponent text = Component.translatable("gui.immersive_furniture.tab." + page.name().toLowerCase(Locale.ROOT));
        StateImageButton button = new StateImageButton(
                TOOLS_WIDTH + (windowWidth - TOOLS_WIDTH - 26 * Page.values().length) / 2 + leftPos + x, topPos - 24, 26, 28,
                x, 128, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> {
                    currentPage = page;
                    init();
                }, text);
        button.setTooltip(Tooltip.create(text));
        button.setEnabled(currentPage == page);
        return button;
    }

    @Override
    public void renderBackground(GuiGraphics context) {
        super.renderBackground(context);

        // Background
        drawRectangle(context, leftPos, topPos, windowHeight, TOOLS_WIDTH);
        drawRectangle(context, leftPos + TOOLS_WIDTH, topPos, windowHeight, windowWidth - TOOLS_WIDTH);

        switch (currentPage) {
            case MODEL -> modelComponent.render(context);
            case MATERIALS -> materialsComponent.render(context);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        // Model
        drawModel(graphics, FurnitureData.EMPTY, leftPos + TOOLS_WIDTH + (windowWidth - TOOLS_WIDTH) / 2, topPos + windowHeight / 2, 100, camYaw, camPitch, mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        camYaw -= (float) (dragX * 0.015f);
        camPitch += (float) (dragY * 0.015f);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredElement != null) {
            selectedElement = hoveredElement;
            init();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
