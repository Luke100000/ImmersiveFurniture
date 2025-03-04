package immersive_furniture.client.gui;

import immersive_furniture.client.Utils;
import immersive_furniture.client.gui.components.MaterialsComponent;
import immersive_furniture.client.gui.components.ModelComponent;
import immersive_furniture.client.gui.widgets.StateImageButton;
import immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ArtisansWorkstationEditorScreen extends ArtisansWorkstationScreen {
    public static int TOOLS_WIDTH = 100;

    float camYaw = (float) (-Math.PI / 4 * 3);
    float camPitch = (float) (-Math.PI / 4);
    float camZoom = 100.0f;

    public FurnitureData data = new FurnitureData();
    public FurnitureData.Element hoveredElement;
    public FurnitureData.Element selectedElement;
    public Direction hoveredDirection;

    DraggingContext draggingContext;
    boolean holdingShift = false;
    boolean holdingCtrl = false;
    boolean holdingSpace = false;

    MaterialsComponent materialsComponent = new MaterialsComponent(this);
    ModelComponent modelComponent = new ModelComponent(this);

    Page currentPage = Page.MODEL;

    public enum Page {
        MODEL,
        MATERIALS,
        SHAPES,
        EFFECTS,
        SETTINGS,
        FINISH
    }

    @Override
    public void init() {
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
        graphics.enableScissor(leftPos + TOOLS_WIDTH + 3, topPos + 3, leftPos + windowWidth - 3, topPos + windowHeight - 3);
        drawModel(graphics, data, leftPos + TOOLS_WIDTH + (windowWidth - TOOLS_WIDTH) / 2, topPos + windowHeight / 2, camZoom, camYaw, camPitch, mouseX, mouseY);
        graphics.disableScissor();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingContext == null) {
            camYaw += (float) (dragX * 0.015f);
            camPitch -= (float) (dragY * 0.015f);
        }

        if (draggingContext != null) {
            float offset = draggingContext.getOffset(mouseX, mouseY);
            float offset2 = holdingShift ? -offset : 0.0f;

            if (draggingContext.direction == Direction.WEST || draggingContext.direction == Direction.NORTH || draggingContext.direction == Direction.UP) {
                float temp = offset2;
                offset = offset2;
                offset2 = temp;
            }

            if (draggingContext.resize) {
                offset = (float) Math.floor(offset + 0.5f);
                switch (draggingContext.direction) {
                    case EAST, WEST -> {
                        draggingContext.element.from.x = draggingContext.originalFrom.z + offset;
                        draggingContext.element.to.x = Math.max(draggingContext.element.from.x, draggingContext.originalTo.x - offset2);
                    }
                    case UP, DOWN -> {
                        draggingContext.element.from.y = draggingContext.originalFrom.z + offset;
                        draggingContext.element.to.y = Math.max(draggingContext.element.from.y, draggingContext.originalTo.y - offset2);
                    }
                    case NORTH, SOUTH -> {
                        draggingContext.element.from.z = draggingContext.originalFrom.z + offset;
                        draggingContext.element.to.z = Math.max(draggingContext.element.from.z, draggingContext.originalTo.z - offset2);
                    }
                }
            } else {
                if (holdingCtrl) {
                    offset = (float) Math.floor(offset * 8.0f + 0.5f) / 8.0f;
                } else {
                    offset = (float) Math.floor(offset + 0.5f);
                }

                switch (draggingContext.direction) {
                    case EAST, WEST -> {
                        draggingContext.element.to.x = draggingContext.originalTo.x + offset;
                        draggingContext.element.from.x = draggingContext.originalFrom.x + offset;
                    }
                    case UP, DOWN -> {
                        draggingContext.element.to.y = draggingContext.originalTo.y + offset;
                        draggingContext.element.from.y = draggingContext.originalFrom.y + offset;
                    }
                    case NORTH, SOUTH -> {
                        draggingContext.element.from.z = draggingContext.originalFrom.z + offset;
                        draggingContext.element.to.z = draggingContext.originalTo.z + offset;
                    }
                }
            }

            modelComponent.update();

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (hoveredElement != null) {
                selectedElement = hoveredElement;
                init();

                draggingContext = new DraggingContext(hoveredElement, hoveredDirection, mouseX, mouseY, button == 1);
            } else if (mouseX > leftPos + TOOLS_WIDTH && mouseX < leftPos + windowWidth && mouseY > topPos && mouseY < topPos + windowHeight) {
                selectedElement = null;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingContext = null;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 340) {
            holdingShift = true;
        } else if (keyCode == 341) {
            holdingCtrl = true;
        } else if (keyCode == 32) {
            holdingSpace = true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 340) {
            holdingShift = false;
        } else if (keyCode == 341) {
            holdingCtrl = false;
        } else if (keyCode == 32) {
            holdingSpace = false;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        camZoom = Math.max(10.0f, Math.min(200.0f, camZoom + (float) delta * 5.0f));

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    final class DraggingContext {
        private final FurnitureData.Element element;
        private final Direction direction;
        private final double x;
        private final double y;
        private final boolean resize;

        private final Vector3f originalFrom;
        private final Vector3f originalTo;

        DraggingContext(FurnitureData.Element element, Direction direction, double x, double y, boolean resize) {
            this.element = element;
            this.direction = direction;
            this.x = x;
            this.y = y;
            this.resize = resize;

            this.originalFrom = new Vector3f(element.from);
            this.originalTo = new Vector3f(element.to);
        }

        public float getOffset(double mouseX, double mouseY) {
            Quaternionf q = new Quaternionf().rotateX(camPitch).rotateY(camYaw);
            Vector3f drag = new Vector3f((float) (mouseX - x), (float) (mouseY - y), 0.0f);
            Vector3f transform = q.transform(direction.step());
            float dot = transform.dot(drag.normalize(new Vector3f()));
            drag.mul(dot);
            return drag.length() / camZoom * 16.0f * (dot < 0 ? -1 : 1);
        }
    }

    protected void drawModel(GuiGraphics graphics, FurnitureData data, int x, int y, float size, float yaw, float pitch, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 100.0);
        graphics.pose().translate(0.5, 1.0, 0.5);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(size, size, size));
        graphics.pose().mulPose(new Quaternionf().rotateX(pitch).rotateY(yaw));
        graphics.pose().translate(-0.5, 0.4, -0.5);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));

        // Render the model
        BakedModel bakedModel = renderModel(graphics, data);

        // Render the checker plane
        checkerPlane(graphics);

        Matrix4f pose = graphics.pose().last().pose();
        Matrix3f normal = graphics.pose().last().normal();
        graphics.pose().popPose();
        graphics.flush();

        // Z-cast and get the hovered element
        List<BakedQuad> quads = bakedModel.getQuads(null, null, RandomSource.create());
        List<Integer> elementIndices = new LinkedList<>();
        List<Direction> elementDirections = new LinkedList<>();
        for (int quadIndex = 0; quadIndex < quads.size(); quadIndex++) {
            BakedQuad quad = quads.get(quadIndex);
            Vector3f n = normal.transform(new Vector3f(quad.getDirection().step().mul(1, -1, 1)));
            if (n.z() < 0) {
                continue;
            }
            int[] vertices = quad.getVertices();
            Vector4f[] vectorVertices = {
                    getVertex(pose, vertices, 0),
                    getVertex(pose, vertices, 1),
                    getVertex(pose, vertices, 2),
                    getVertex(pose, vertices, 3)
            };
            if (Utils.isWithinQuad(mouseX, mouseY, vectorVertices)) {
                elementIndices.add(quadIndex / 6);
                elementDirections.add(quad.getDirection());
                quadIndex = (quadIndex / 6 + 1) * 6;
            }
        }
        if (elementIndices.isEmpty()) {
            hoveredElement = null;
            hoveredDirection = null;
        } else {
            List<FurnitureData.Element> elements = elementIndices.stream().map(data.elements::get).toList();
            int elementIndex = (elements.indexOf(selectedElement) + 1) % elements.size();
            hoveredElement = elements.get(elementIndex);
            hoveredDirection = elementDirections.get(elementIndex);


            // Highlight the hovered element
            drawSelection(graphics, data, hoveredElement, quads, pose, 1.0f);
        }

        // Highlight the selected element
        if (selectedElement != null) {
            drawSelection(graphics, data, selectedElement, quads, pose, 0.5f);
        }
    }
}
