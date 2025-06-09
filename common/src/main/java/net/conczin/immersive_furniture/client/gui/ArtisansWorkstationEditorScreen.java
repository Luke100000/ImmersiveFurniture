package net.conczin.immersive_furniture.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.conczin.immersive_furniture.client.Utils;
import net.conczin.immersive_furniture.client.gui.components.*;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.client.model.ClientModelUtils;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ArtisansWorkstationEditorScreen extends ArtisansWorkstationScreen {
    public static final int TOOLS_WIDTH = 100;

    float camYaw = (float) (-Math.PI / 4 * 3);
    float camPitch = (float) (-Math.PI / 4);
    float camZoom = 100.0f;

    public final FurnitureData data;
    public FurnitureData.Element hoveredElement;
    public FurnitureData.Element selectedElement;
    public Direction hoveredDirection;

    DraggingContext draggingContext;
    boolean isRotatingView;
    boolean holdingShift = false;
    boolean holdingCtrl = false;
    boolean holdingSpace = false;

    int lastMouseX;
    int lastMouseY;

    final MaterialsComponent materialsComponent = new MaterialsComponent(this);
    final ParticlesComponent particlesComponent = new ParticlesComponent(this);
    final SoundsComponent poundsComponent = new SoundsComponent(this);
    final ModelComponent modelComponent = new ModelComponent(this);
    final EffectsComponent effectsComponent = new EffectsComponent(this);
    final SettingsComponent settingsComponent = new SettingsComponent(this);

    Page currentPage = Page.MODEL;

    public enum Page {
        MODEL,
        MATERIALS,
        PARTICLES,
        SOUNDS,
        EFFECTS,
        SETTINGS
    }

    public ArtisansWorkstationEditorScreen(FurnitureData data) {
        this.data = data;
    }

    @Override
    public void init() {
        super.init();

        clearWidgets();

        minecraft = Minecraft.getInstance();

        switch (currentPage) {
            case MODEL -> modelComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
            case MATERIALS -> materialsComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
            case PARTICLES -> particlesComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
            case SOUNDS -> poundsComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
            case EFFECTS -> effectsComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
            case SETTINGS -> settingsComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
        }

        // Close
        MutableComponent text = Component.translatable("gui.immersive_furniture.tab.cancel");
        StateImageButton button = new StateImageButton(
                leftPos + 4, topPos - 24, 26, 28,
                130, 160, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> Minecraft.getInstance().setScreen(new ArtisansWorkstationLibraryScreen()), text);
        button.setTooltip(Tooltip.create(text));
        button.setEnabled(false);
        addRenderableWidget(button);

        // Page buttons
        int x = 0;
        addRenderableWidget(pagePageButton(Page.MODEL, x, 0));
        x += 26;
        if (selectedElement != null && selectedElement.type == FurnitureData.ElementType.PARTICLE_EMITTER) {
            addRenderableWidget(pagePageButton(Page.PARTICLES, x, 6 * 26));
            x += 26;
        } else if (selectedElement != null && selectedElement.type == FurnitureData.ElementType.SOUND_EMITTER) {
            addRenderableWidget(pagePageButton(Page.SOUNDS, x, 7 * 26));
            x += 26;
        } else if (selectedElement != null && selectedElement.type == FurnitureData.ElementType.ELEMENT) {
            addRenderableWidget(pagePageButton(Page.MATERIALS, x, 26));
            x += 26;
            addRenderableWidget(pagePageButton(Page.EFFECTS, x, 2 * 26));
            x += 26;
        }
        addRenderableWidget(pagePageButton(Page.SETTINGS, x, 3 * 26));
    }

    private StateImageButton pagePageButton(Page page, int x, int u) {
        MutableComponent text = Component.translatable("gui.immersive_furniture.tab." + page.name().toLowerCase(Locale.ROOT));
        StateImageButton button = new StateImageButton(
                TOOLS_WIDTH + (windowWidth - TOOLS_WIDTH - 26 * Page.values().length) / 2 + leftPos + x, topPos - 24, 26, 28,
                u, 160, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
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
        drawRectangle(context, leftPos, topPos, TOOLS_WIDTH, windowHeight);
        drawRectangle(context, leftPos + TOOLS_WIDTH, topPos, windowWidth - TOOLS_WIDTH, windowHeight);

        switch (currentPage) {
            case MODEL -> modelComponent.render(context);
            case MATERIALS -> materialsComponent.render(context);
            case PARTICLES -> particlesComponent.render(context);
            case SOUNDS -> poundsComponent.render(context);
            case SETTINGS -> settingsComponent.render(context);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        // Recompute hash
        data.dirty();

        // Model
        graphics.enableScissor(leftPos + TOOLS_WIDTH + 3, topPos + 3, leftPos + windowWidth - 3, topPos + windowHeight - 3);
        drawModel(graphics, data, leftPos + TOOLS_WIDTH + (windowWidth - TOOLS_WIDTH) / 2, topPos + windowHeight / 2, camZoom, camYaw, camPitch, mouseX, mouseY);
        graphics.disableScissor();
    }

    public Vector3f quantVector(Vector3f normal, float offset, boolean quantize) {
        normal.mul(offset);

        float stepSize = quantize ? 4.0f : 1.0f;
        normal.x = (float) Math.floor(normal.x * stepSize + 0.5f) / stepSize;
        normal.y = (float) Math.floor(normal.y * stepSize + 0.5f) / stepSize;
        normal.z = (float) Math.floor(normal.z * stepSize + 0.5f) / stepSize;

        return normal;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isRotatingView) {
            camYaw += (float) (dragX * 0.015f);
            camPitch -= (float) (dragY * 0.015f);
        }

        if (draggingContext != null) {
            float offset = draggingContext.getOffset(mouseX, mouseY);
            Vector3f local = quantVector(draggingContext.direction.step().mul(1, -1, 1), offset, false);
            Vector3f global = quantVector(draggingContext.getNormal(), offset, holdingCtrl && !draggingContext.resize);

            Vector3f normal;
            Vector3f normal2;
            if (draggingContext.resize) {
                if (holdingShift) {
                    // Offset into both directions
                    normal = new Vector3f(local);
                    normal2 = local.negate();
                } else {
                    // Offset one face, but move the object to negate the origin offset
                    Vector3f o = new Vector3f(local).sub(global).mul(0.5f);
                    normal = new Vector3f(local).sub(o);
                    normal2 = new Vector3f(0.0f).sub(o);
                }
            } else {
                // Offset object by global axis
                normal = global;
                normal2 = global;
            }

            if (draggingContext.direction == Direction.DOWN || draggingContext.direction == Direction.WEST || draggingContext.direction == Direction.NORTH) {
                draggingContext.element.from.x = Math.min(draggingContext.element.to.x, draggingContext.originalFrom.x + normal.x);
                draggingContext.element.from.y = Math.min(draggingContext.element.to.y, draggingContext.originalFrom.y - normal.y);
                draggingContext.element.from.z = Math.min(draggingContext.element.to.z, draggingContext.originalFrom.z + normal.z);

                draggingContext.element.to.x = Math.max(draggingContext.element.from.x, draggingContext.originalTo.x + normal2.x);
                draggingContext.element.to.y = Math.max(draggingContext.element.from.y, draggingContext.originalTo.y - normal2.y);
                draggingContext.element.to.z = Math.max(draggingContext.element.from.z, draggingContext.originalTo.z + normal2.z);
            } else {
                draggingContext.element.to.x = Math.max(draggingContext.element.from.x, draggingContext.originalTo.x + normal.x);
                draggingContext.element.to.y = Math.max(draggingContext.element.from.y, draggingContext.originalTo.y - normal.y);
                draggingContext.element.to.z = Math.max(draggingContext.element.from.z, draggingContext.originalTo.z + normal.z);

                draggingContext.element.from.x = Math.min(draggingContext.element.to.x, draggingContext.originalFrom.x + normal2.x);
                draggingContext.element.from.y = Math.min(draggingContext.element.to.y, draggingContext.originalFrom.y - normal2.y);
                draggingContext.element.from.z = Math.min(draggingContext.element.to.z, draggingContext.originalFrom.z + normal2.z);
            }

            draggingContext.element.sanityCheck();

            if (currentPage == Page.MODEL) {
                modelComponent.update();
            }

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && hoveredElement != null) {
            selectedElement = hoveredElement;
            draggingContext = new DraggingContext(hoveredElement, hoveredDirection, mouseX, mouseY, button == 1);
            isRotatingView = false;
            init();
        } else {
            isRotatingView = isOverRightWindow(mouseX, mouseY);
        }

        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingContext != null) {
            draggingContext = null;
        }

        // Deselect element
        if (selectedElement != null && hoveredElement == null && lastMouseX == (int) mouseX && lastMouseY == (int) mouseY && isOverRightWindow(mouseX, mouseY)) {
            selectedElement = null;
            init();
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverRightWindow(double mouseX, double mouseY) {
        return mouseX > leftPos + TOOLS_WIDTH && mouseX < leftPos + windowWidth && mouseY > topPos && mouseY < topPos + windowHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 340) {
            holdingShift = true;
        } else if (keyCode == 341) {
            holdingCtrl = true;
        } else if (keyCode == 32) {
            holdingSpace = true;
        } else if (keyCode == 261) {
            if (selectedElement != null) {
                data.elements.remove(selectedElement);
                selectedElement = null;
                init();
            }
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
        camZoom = Math.max(20.0f, Math.min(120.0f, camZoom + (float) delta * 0.1f * camZoom));

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
            // View space normal
            Vector3f normal = getNormal();
            Quaternionf q = new Quaternionf().rotateX(camPitch).rotateY(camYaw);
            q.transform(normal).normalize();

            Vector3f screenNormal = new Vector3f(normal.x, normal.y, 0.0f).normalize();
            Vector3f drag = new Vector3f((float) (mouseX - x), (float) (mouseY - y), 0.0f);
            float proj = drag.dot(screenNormal);

            float viewDot = (float) Math.sqrt(1.0f - normal.z * normal.z);
            return proj / camZoom * 16.0f / viewDot;
        }

        private Vector3f getNormal() {
            Vector3f normal = direction.step();
            switch (this.element.axis) {
                case X -> Axis.XP.rotationDegrees(this.element.rotation).transform(normal);
                case Y -> Axis.YP.rotationDegrees(this.element.rotation).transform(normal);
                case Z -> Axis.ZP.rotationDegrees(this.element.rotation).transform(normal);
            }
            normal.mul(1, -1, 1);
            return normal;
        }
    }

    record HoverResult(FurnitureData.Element element, Direction direction, float depth) {
    }

    protected void drawModel(GuiGraphics graphics, FurnitureData data, int x, int y, float size, float yaw, float pitch, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 100.0);
        graphics.pose().translate(0.5, 1.0, 0.5);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(size));
        graphics.pose().mulPose(new Quaternionf().rotateX(pitch).rotateY(yaw));
        graphics.pose().translate(-0.5, 0.4, -0.5);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));

        RenderSystem.assertOnRenderThread();
        Lighting.setupLevel(new Matrix4f().rotateX(pitch).rotateY(yaw));

        // Render the model
        renderModel(graphics, data, yaw, pitch, true);

        Lighting.setupFor3DItems();

        // Render the checker plane
        graphics.pose().pushPose();
        checkerPlane(graphics);
        graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.PI / 2));
        graphics.pose().translate(1, 1, -1);
        graphics.pose().scale(-1, 1, 1);
        checkerPlane(graphics);
        graphics.pose().popPose();

        Matrix4f pose = graphics.pose().last().pose();
        Matrix3f normal = graphics.pose().last().normal();

        graphics.pose().popPose();

        // Z-cast and get the hovered element
        List<HoverResult> results = new LinkedList<>();
        for (FurnitureData.Element element : data.elements) {
            float[] fs = ClientModelUtils.getShapeData(element);
            for (Direction facing : Direction.values()) {
                Vector3f n = new Vector3f(facing.step());
                n = ModelUtils.getElementRotation(element.getRotation()).transform(n);
                n.mul(1, -1, 1);
                normal.transform(n);
                if (n.z() < 0) continue;

                Vector3f[] vertices = ClientModelUtils.getVertices(element, facing, fs, pose);
                if (Utils.isWithinQuad(mouseX, mouseY, vertices)) {
                    float depth = vertices[0].z() + vertices[1].z() + vertices[2].z() + vertices[3].z();
                    results.add(new HoverResult(element, facing, depth));
                }
            }
        }

        if (results.isEmpty() || !isMouseOver(mouseX, mouseY)) {
            hoveredElement = null;
            hoveredDirection = null;
        } else {
            results.sort((a, b) -> Float.compare(a.depth, b.depth));

            int index = 0;
            if (lastMouseX == mouseX && lastMouseY == mouseY && selectedElement != null) {
                for (HoverResult result : results) {
                    if (result.element == selectedElement) {
                        break;
                    }
                    index++;
                }
            }
            HoverResult hoverResult = results.get((index + 1) % results.size());
            hoveredElement = hoverResult.element();
            hoveredDirection = hoverResult.direction();

            // Highlight the hovered element
            drawSelection(graphics, hoveredElement, pose, 1.0f, false);
        }

        // Highlight the selected element
        if (selectedElement != null) {
            drawSelection(graphics, selectedElement, pose, 0.5f, true);
        }

        // Highlight all non-solid elements
        for (FurnitureData.Element element : data.elements) {
            if (element.type != FurnitureData.ElementType.ELEMENT) {
                drawSelection(graphics, element, pose, 0.4f, true);
            }
        }
    }

    void drawSelection(GuiGraphics graphics, FurnitureData.Element element, Matrix4f pose, float width, boolean overlay) {
        float[] fs = ClientModelUtils.getShapeData(element);
        for (Direction facing : Direction.values()) {
            Vector3f[] vertices = ClientModelUtils.getVertices(element, facing, fs, pose);
            for (int i = 0; i < 4; i++) {
                Vector3f vertex = vertices[i];
                Vector3f nextVertex = vertices[(i + 1) % 4];
                line(graphics, vertex.x(), vertex.y(), vertex.z(), nextVertex.x(), nextVertex.y(), nextVertex.z(), width, overlay, 0.0f, 0.0f, 0.0f, 1.0f);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
