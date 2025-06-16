package net.conczin.immersive_furniture.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.Utils;
import net.conczin.immersive_furniture.client.gui.components.*;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.client.model.ClientModelUtils;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class ArtisansWorkstationEditorScreen extends ArtisansWorkstationScreen {
    public static final ResourceLocation TEXTURE_CHECKERPLANE = Common.locate("textures/gui/checkerplane.png");
    public static final int TOOLS_WIDTH = 100;

    float camYaw = (float) (-Math.PI / 4 * 3);
    float camPitch = (float) (-Math.PI / 4);
    float camZoom = 100.0f;

    public FurnitureData data;
    public FurnitureData.Element selectedElement;
    public HoverResult hoverResult;
    public HoverResult nextHoverResult;

    final static int MAX_HISTORY_SIZE = 20;
    private String lastHistoryHash = "";
    private final Deque<CompoundTag> history = new ArrayDeque<>(MAX_HISTORY_SIZE);
    private CompoundTag copiedElement;

    DraggingContext draggingContext;
    boolean isRotatingView;

    int lastMouseX;
    int lastMouseY;

    final MaterialsComponent materialsComponent = new MaterialsComponent(this);
    final ParticlesComponent particlesComponent = new ParticlesComponent(this);
    final SoundsComponent poundsComponent = new SoundsComponent(this);
    final ModelComponent modelComponent = new ModelComponent(this);
    final EffectsComponent effectsComponent = new EffectsComponent(this);
    final SettingsComponent settingsComponent = new SettingsComponent(this);
    final SpritesComponent spritesComponent = new SpritesComponent(this);

    Page currentPage = Page.MODEL;

    public enum Page {
        MODEL,
        MATERIALS,
        PARTICLES,
        SOUNDS,
        EFFECTS,
        SETTINGS,
        SPRITES
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
            case SPRITES -> spritesComponent.init(leftPos, topPos, TOOLS_WIDTH, windowHeight);
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
        int x = 16;
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
        } else if (selectedElement != null && selectedElement.type == FurnitureData.ElementType.SPRITE) {
            addRenderableWidget(pagePageButton(Page.SPRITES, x, 4 * 26));
            x += 26;
            addRenderableWidget(pagePageButton(Page.EFFECTS, x, 2 * 26));
            x += 26;
        }
        addRenderableWidget(pagePageButton(Page.SETTINGS, x, 3 * 26));

        // Help button
        MutableComponent helpText = Component.translatable("gui.immersive_furniture.tab.help");
        StateImageButton helpButton = new StateImageButton(
                leftPos + 240, topPos - 24, 26, 28,
                8 * 26, 160, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> openHelp(),
                helpText
        );
        helpButton.setEnabled(false);
        helpButton.setTooltip(Tooltip.create(helpText));
        addRenderableWidget(helpButton);

        addHistory();
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

    private void openHelp() {
        try {
            Util.getPlatform().openUri("https://github.com/Luke100000/immersiveFurniture/wiki/Help");
        } catch (Exception e) {
            Common.logger.error(e);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);

        // Background
        drawRectangle(graphics, leftPos, topPos, TOOLS_WIDTH, windowHeight);
        drawRectangle(graphics, leftPos + TOOLS_WIDTH, topPos, windowWidth - TOOLS_WIDTH, windowHeight);
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

        graphics.pose().translate(0, 0, 2048.0f);

        switch (currentPage) {
            case MODEL -> modelComponent.render(graphics);
            case MATERIALS -> materialsComponent.render(graphics);
            case PARTICLES -> particlesComponent.render(graphics);
            case SOUNDS -> poundsComponent.render(graphics);
            case SETTINGS -> settingsComponent.render(graphics);
            case SPRITES -> spritesComponent.render(graphics);
        }
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
            Vector3f global = quantVector(draggingContext.getNormal(), offset, hasControlDown() && !draggingContext.resize);

            Vector3f normal;
            Vector3f normal2;
            if (draggingContext.resize) {
                if (hasShiftDown()) {
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
        if ((button == 0 || button == 1) && hoverResult != null && nextHoverResult != null) {
            HoverResult result = lastMouseX == (int) mouseX && lastMouseY == (int) mouseY ? nextHoverResult : hoverResult;
            selectedElement = result.element();

            if (currentPage == Page.MATERIALS || currentPage == Page.SOUNDS || currentPage == Page.PARTICLES || currentPage == Page.SPRITES) {
                if (selectedElement.type == FurnitureData.ElementType.ELEMENT) {
                    currentPage = Page.MATERIALS;
                } else if (selectedElement.type == FurnitureData.ElementType.SOUND_EMITTER) {
                    currentPage = Page.SOUNDS;
                } else if (selectedElement.type == FurnitureData.ElementType.PARTICLE_EMITTER) {
                    currentPage = Page.PARTICLES;
                } else if (selectedElement.type == FurnitureData.ElementType.SPRITE) {
                    currentPage = Page.SPRITES;
                }
            }

            draggingContext = new DraggingContext(result.element(), result.direction(), mouseX, mouseY, button == 1);
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
        if (selectedElement != null && hoverResult == null && lastMouseX == (int) mouseX && lastMouseY == (int) mouseY && isOverRightWindow(mouseX, mouseY)) {
            selectedElement = null;
            init();
        }

        addHistory();

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverRightWindow(double mouseX, double mouseY) {
        return mouseX > leftPos + TOOLS_WIDTH && mouseX < leftPos + windowWidth && mouseY > topPos && mouseY < topPos + windowHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isCopy(keyCode)) {
            if (selectedElement != null) {
                copiedElement = selectedElement.toTag();
            }
        } else if (isCut(keyCode)) {
            if (selectedElement != null) {
                copiedElement = selectedElement.toTag();
                data.elements.remove(selectedElement);
                selectedElement = null;
                init();
            }
        } else if (isPaste(keyCode)) {
            if (copiedElement != null) {
                selectedElement = new FurnitureData.Element(selectedElement);
                data.elements.add(new FurnitureData.Element(copiedElement));
                init();
            }
        } else if (isUndo(keyCode)) {
            if (!history.isEmpty() && lastHistoryHash.equals(data.getHash())) {
                history.removeFirst();
            }
            if (!history.isEmpty()) {
                CompoundTag oldData = history.removeFirst();
                if (oldData != null) {
                    data = new FurnitureData(oldData);
                    selectedElement = null;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean isUndo(int keyCode) {
        return (keyCode == 89 || keyCode == 90) && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        camZoom = Math.max(20.0f, Math.min(120.0f, camZoom + (float) delta * 0.1f * camZoom));

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    final class DraggingContext {
        private final FurnitureData.Element element;
        private Direction direction;
        private final double x;
        private final double y;
        private final boolean resize;

        private final Vector3f originalFrom;
        private final Vector3f originalTo;

        private final boolean isFlat;

        DraggingContext(FurnitureData.Element element, Direction direction, double x, double y, boolean resize) {
            this.element = element;
            this.direction = direction;
            this.x = x;
            this.y = y;
            this.resize = resize;

            this.originalFrom = new Vector3f(element.from);
            this.originalTo = new Vector3f(element.to);

            this.isFlat = element.isFlat();
        }

        public float getOffset(double mouseX, double mouseY) {
            // View space normal
            Vector3f normal = getNormal();
            Quaternionf q = new Quaternionf().rotateX(camPitch).rotateY(camYaw);
            q.transform(normal).normalize();

            Vector3f screenNormal = new Vector3f(normal.x, normal.y, 0.0f).normalize();
            Vector3f drag = new Vector3f((float) (mouseX - x), (float) (mouseY - y), 0.0f);
            float proj = drag.dot(screenNormal);

            // Use the move axis rather than face for flat elements
            if ((isFlat || hasAltDown()) && drag.lengthSquared() > 1.0f) {
                Direction bestDirection = Direction.UP;
                float bestDot = Float.MIN_VALUE;
                for (Direction value : Direction.values()) {
                    Vector3f globalDirectionNormal = element.getGlobalDirectionNormal(value);
                    q.transform(globalDirectionNormal).normalize();
                    float dot = globalDirectionNormal.dot(drag);
                    if (dot > bestDot) {
                        bestDirection = value;
                        bestDot = dot;
                    }
                }
                direction = bestDirection;
            }

            float viewDot = (float) Math.sqrt(1.0f - normal.z * normal.z);
            return proj / camZoom * 16.0f / viewDot;
        }

        private Vector3f getNormal() {
            return element.getGlobalDirectionNormal(direction);
        }
    }

    public record HoverResult(FurnitureData.Element element, Direction direction, float depth) {
    }

    protected void drawModel(GuiGraphics graphics, FurnitureData data, int x, int y, float size, float yaw, float pitch, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 1024.0);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(size));
        graphics.pose().mulPose(new Quaternionf().rotateX(pitch).rotateY(yaw));
        graphics.pose().translate(data.size.x / 2.0f - 1.0f, data.size.y / 2.0f - 1.0f + 1.0f, data.size.z / 2.0f - 1.0f);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));

        RenderSystem.assertOnRenderThread();
        Lighting.setupLevel(new Matrix4f().rotateX(pitch).rotateY(yaw));

        // Render the model
        renderModel(graphics, data, yaw, pitch, true);

        Lighting.setupFor3DItems();

        // Render the checker plane
        graphics.pose().pushPose();
        checkerPlane(graphics, data.size.x, data.size.z);
        graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.PI / 2));
        graphics.pose().translate(0, 1, 1 - data.size.y);
        graphics.pose().scale(1, 1, -1);
        checkerPlane(graphics, data.size.x, data.size.y);
        graphics.pose().popPose();

        Matrix4f pose = graphics.pose().last().pose();

        graphics.pose().popPose();

        // Perform a proper raycast to get the hovered element
        List<HoverResult> results = new LinkedList<>();

        // Raycast against each element
        for (FurnitureData.Element element : data.elements) {
            Utils.Ray ray = Utils.inverseTransformRay(mouseX, mouseY, pose, element);

            Utils.RaycastResult raycastResult = Utils.raycast(ray, element);
            if (raycastResult != null) {
                results.add(new HoverResult(element, raycastResult.face(), raycastResult.distance()));
            }
        }

        if (results.isEmpty() || !isOverRightWindow(mouseX, mouseY)) {
            hoverResult = null;
            nextHoverResult = null;
        } else {
            results.sort((a, b) -> Float.compare(b.depth, a.depth));

            int index = -1;
            if (selectedElement != null) {
                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).element() == selectedElement) {
                        index = i;
                        break;
                    }
                }
            }

            hoverResult = results.get(Math.max(0, index));
            nextHoverResult = results.get((index + 1) % results.size());

            // Highlight the hovered element
            drawSelection(graphics, hoverResult.element(), pose, 1.0f, false);
        }

        // Highlight the selected element
        if (selectedElement != null) {
            drawSelection(graphics, selectedElement, pose, 0.5f, true);
        }

        // Highlight all non-solid elements
        for (FurnitureData.Element element : data.elements) {
            if (element.type != FurnitureData.ElementType.ELEMENT && element.type != FurnitureData.ElementType.SPRITE) {
                drawSelection(graphics, element, pose, 0.4f, true);
            }
        }
    }

    void checkerPlane(GuiGraphics graphics, float w, float h) {
        RenderSystem.setShaderTexture(0, TEXTURE_CHECKERPLANE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(matrix4f, 1.0f - w, 0.001f, 1.0f - h).uv(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, 1.0f - w, 0.001f, 1.0f).uv(0.0f, h / 8.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, 1.0f, 0.001f, 1.0f).uv(w / 8.0f, h / 8.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, 1.0f, 0.001f, 1.0f - h).uv(w / 8.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
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

    public void addHistory() {
        String hash = data.getHash();
        if (!lastHistoryHash.equals(hash)) {
            lastHistoryHash = hash;
            if (history.size() >= MAX_HISTORY_SIZE) {
                history.removeLast();
            }
            history.addFirst(data.toTag());
        }
    }
}
