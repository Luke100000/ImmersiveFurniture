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
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
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
    public List<FurnitureData.Element> selectedElements = new LinkedList<>();
    public HoverResult hoverResult;
    public HoverResult nextHoverResult;

    final static int MAX_HISTORY_SIZE = 20;
    private String lastHistoryHash = "";
    private final Deque<CompoundTag> history = new ArrayDeque<>(MAX_HISTORY_SIZE);
    private List<CompoundTag> copiedElements = new LinkedList<>();

    private long lastAutosaveTime = 0;

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

    boolean backwardsCheckerPlane = true;

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
                130, 56, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> cancel(), text);
        button.setTooltip(Tooltip.create(text));
        button.setEnabled(false);
        addRenderableWidget(button);

        // Page buttons
        int x = 16;
        addRenderableWidget(pagePageButton(Page.MODEL, x, 0));
        x += 26;
        if (isFirstElement(FurnitureData.ElementType.PARTICLE_EMITTER)) {
            addRenderableWidget(pagePageButton(Page.PARTICLES, x, 6 * 26));
            x += 26;
        } else if (isFirstElement(FurnitureData.ElementType.SOUND_EMITTER)) {
            addRenderableWidget(pagePageButton(Page.SOUNDS, x, 7 * 26));
            x += 26;
        } else if (isFirstElement(FurnitureData.ElementType.ELEMENT)) {
            addRenderableWidget(pagePageButton(Page.MATERIALS, x, 26));
            x += 26;
            addRenderableWidget(pagePageButton(Page.EFFECTS, x, 2 * 26));
            x += 26;
        } else if (isFirstElement(FurnitureData.ElementType.SPRITE)) {
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
                8 * 26, 56, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> openHelp(),
                helpText
        );
        helpButton.setEnabled(false);
        helpButton.setTooltip(Tooltip.create(helpText));
        addRenderableWidget(helpButton);

        // Night-mode button
        MutableComponent nightModeText = Component.translatable("gui.immersive_furniture.nightmode");
        StateImageButton nightModeButton = new StateImageButton(
                leftPos + windowWidth + 1, topPos + windowHeight - 19, 16, 16,
                256 - 48, 160, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> {
                    nightMode = !nightMode;
                    init();
                },
                nightModeText
        );
        nightModeButton.setTooltip(Tooltip.create(nightModeText));
        nightModeButton.setEnabled(nightMode);
        addRenderableWidget(nightModeButton);

        // Backwards checker plane button
        MutableComponent backwardsCheckerPlaneText = Component.translatable("gui.immersive_furniture.backwards_checkerplane");
        StateImageButton backwardsCheckerButton = new StateImageButton(
                leftPos + windowWidth + 1, topPos + windowHeight - 36, 16, 16,
                256 - 32, 160, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                b -> {
                    backwardsCheckerPlane = !backwardsCheckerPlane;
                    init();
                },
                backwardsCheckerPlaneText
        );
        backwardsCheckerButton.setTooltip(Tooltip.create(backwardsCheckerPlaneText));
        backwardsCheckerButton.setEnabled(backwardsCheckerPlane);
        addRenderableWidget(backwardsCheckerButton);

        addHistory();
    }

    private void cancel() {
        if (lastCriticalActionAttempt + 5000 > System.currentTimeMillis()) {
            Minecraft.getInstance().setScreen(new ArtisansWorkstationLibraryScreen());
        } else {
            // Ask for confirmation
            lastCriticalActionAttempt = System.currentTimeMillis();
            setError("gui.immersive_furniture.cancel_confirm");
        }
    }

    private StateImageButton pagePageButton(Page page, int x, int u) {
        MutableComponent text = Component.translatable("gui.immersive_furniture.tab." + page.name().toLowerCase(Locale.ROOT));
        StateImageButton button = new StateImageButton(
                TOOLS_WIDTH + (windowWidth - TOOLS_WIDTH - 26 * Page.values().length) / 2 + leftPos + x, topPos - 24, 26, 28,
                u, 56, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
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

        renderError(graphics, height / 2);
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
            Vector3f local = quantVector(draggingContext.direction.step(), offset, false);
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

            for (FurnitureData.Element element : selectedElements) {
                if (!draggingContext.originalFrom.containsKey(element)) continue;
                Vector3f originalFrom = draggingContext.originalFrom.get(element);
                Vector3f originalTo = draggingContext.originalTo.get(element);

                if (draggingContext.direction == Direction.DOWN || draggingContext.direction == Direction.WEST || draggingContext.direction == Direction.NORTH) {
                    element.from.x = Math.min(element.to.x, originalFrom.x + normal.x);
                    element.from.y = Math.min(element.to.y, originalFrom.y + normal.y);
                    element.from.z = Math.min(element.to.z, originalFrom.z + normal.z);

                    element.to.x = Math.max(element.from.x, originalTo.x + normal2.x);
                    element.to.y = Math.max(element.from.y, originalTo.y + normal2.y);
                    element.to.z = Math.max(element.from.z, originalTo.z + normal2.z);
                } else {
                    element.to.x = Math.max(element.from.x, originalTo.x + normal.x);
                    element.to.y = Math.max(element.from.y, originalTo.y + normal.y);
                    element.to.z = Math.max(element.from.z, originalTo.z + normal.z);

                    element.from.x = Math.min(element.to.x, originalFrom.x + normal2.x);
                    element.from.y = Math.min(element.to.y, originalFrom.y + normal2.y);
                    element.from.z = Math.min(element.to.z, originalFrom.z + normal2.z);
                }

                element.sanityCheck();
            }

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
            boolean doubleClick = lastMouseX == (int) mouseX && lastMouseY == (int) mouseY;
            HoverResult result = doubleClick ? nextHoverResult : hoverResult;

            if (hasShiftDown() && selectedElements.contains(result.element)) {
                selectedElements.remove(result.element);
            } else {
                selectElement(result.element(), hasShiftDown() || (selectedElements.size() > 1 && !doubleClick));
            }

            if (currentPage == Page.MATERIALS || currentPage == Page.SOUNDS || currentPage == Page.PARTICLES || currentPage == Page.SPRITES) {
                if (isFirstElement(FurnitureData.ElementType.ELEMENT)) {
                    currentPage = Page.MATERIALS;
                } else if (isFirstElement(FurnitureData.ElementType.SOUND_EMITTER)) {
                    currentPage = Page.SOUNDS;
                } else if (isFirstElement(FurnitureData.ElementType.PARTICLE_EMITTER)) {
                    currentPage = Page.PARTICLES;
                } else if (isFirstElement(FurnitureData.ElementType.SPRITE)) {
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
        if (!selectedElements.isEmpty() && hoverResult == null && lastMouseX == (int) mouseX && lastMouseY == (int) mouseY && isOverRightWindow(mouseX, mouseY)) {
            selectedElements.clear();
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
            // Copy
            copiedElements = selectedElements.stream().map(FurnitureData.Element::toTag).toList();
        } else if (isCut(keyCode)) {
            // Cut
            if (!selectedElements.isEmpty()) {
                copiedElements = selectedElements.stream().map(FurnitureData.Element::toTag).toList();
                data.elements.removeAll(selectedElements);
                selectedElements.clear();
                init();
            }
        } else if (isPaste(keyCode)) {
            // Paste
            if (!copiedElements.isEmpty()) {
                selectedElements.clear();
                for (CompoundTag copiedElement : copiedElements) {
                    FurnitureData.Element element = new FurnitureData.Element(copiedElement);
                    data.elements.add(element);
                    selectedElements.add(element);
                }
                init();
            }
        } else if (keyCode == 261) {
            // Delete
            if (!selectedElements.isEmpty() && !(getFocused() instanceof EditBox)) {
                data.elements.removeAll(selectedElements);
                selectedElements.clear();
                init();
            }
        } else if (keyCode == 68 && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            // Duplicate
            modelComponent.duplicateElements();
        } else if (keyCode == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            // Select all
            selectedElements.clear();
            selectedElements.addAll(data.elements);
        } else if (keyCode == 77 && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            // Paste material
            if (!selectedElements.isEmpty() && !copiedElements.isEmpty()) {
                FurnitureData.Element element = new FurnitureData.Element(copiedElements.get(0));
                for (FurnitureData.Element selectedElement : selectedElements) {
                    selectedElement.material = new FurnitureData.Material(element.material);
                    selectedElement.color = element.color;
                    selectedElement.emission = element.emission;
                }
                init();
            }
        } else if (isUndo(keyCode)) {
            // Undo
            if (!history.isEmpty() && lastHistoryHash.equals(data.getHash())) {
                history.removeFirst();
            }
            if (!history.isEmpty()) {
                CompoundTag oldData = history.removeFirst();
                if (oldData != null) {
                    data = new FurnitureData(oldData);
                    selectedElements.clear();
                    init();
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
        private boolean autoDirectionLock;
        private final double x;
        private final double y;
        private final boolean resize;

        private final Map<FurnitureData.Element, Vector3f> originalFrom = new HashMap<>();
        private final Map<FurnitureData.Element, Vector3f> originalTo = new HashMap<>();

        private final boolean isFlat;

        DraggingContext(FurnitureData.Element element, Direction direction, double x, double y, boolean resize) {
            this.element = element;
            this.direction = direction;
            this.x = x;
            this.y = y;
            this.resize = resize;

            for (FurnitureData.Element e : selectedElements) {
                this.originalFrom.put(e, new Vector3f(e.from));
                this.originalTo.put(e, new Vector3f(e.to));
            }

            this.isFlat = element.isFlat();
        }

        public float getOffset(double mouseX, double mouseY) {
            // View space normal
            Vector3f normal = getNormal();
            Quaternionf q = new Quaternionf().rotateX(camPitch).rotateY(camYaw);
            q.transform(normal.mul(1, -1, 1)).normalize();

            Vector3f screenNormal = new Vector3f(normal.x, normal.y, 0.0f).normalize();
            Vector3f drag = new Vector3f((float) (mouseX - x), (float) (mouseY - y), 0.0f);
            float proj = drag.dot(screenNormal);

            // Use the move axis rather than face for flat elements
            if ((isFlat || hasAltDown()) && drag.lengthSquared() > 2.0f) {
                Direction bestDirection = direction;
                float bestDot = 0.0f;
                for (Direction value : Direction.values()) {
                    if (autoDirectionLock && value != direction && value != direction.getOpposite()) continue;
                    Vector3f directionNormal = element.getGlobalDirectionNormal(value);
                    q.transform(directionNormal.mul(1, -1, 1)).normalize();
                    Vector3f directionScreenNormal = directionNormal.normalize();
                    float dot = directionScreenNormal.dot(drag);
                    if (dot > bestDot) {
                        bestDirection = value;
                        bestDot = dot;
                    }
                }

                // TODO: Check on what side of the face the mouse initially grabbed
                direction = bestDirection;
                autoDirectionLock = true;
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
        graphics.pose().translate(-data.size.x / 2.0f, data.size.y / 2.0f, -data.size.z / 2.0f);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));

        Lighting.setupLevel(new Matrix4f().rotateX(pitch).rotateY(yaw));

        // Render the model
        renderModel(graphics, data, yaw, pitch, true);

        Lighting.setupFor3DItems();

        // Render the checker plane
        graphics.pose().pushPose();
        checkerPlane(graphics, data.size.x, data.size.z);
        graphics.pose().mulPose(new Quaternionf().rotateX((float) Math.PI / 2));
        graphics.pose().translate(0, data.size.z, 0);
        graphics.pose().scale(1, 1, -1);
        if (backwardsCheckerPlane) {
            checkerPlane(graphics, data.size.x, data.size.y);
        }
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

            // TODO: SHift + mouse-scroll for next/previous element?
            int index = -1;
            if (!selectedElements.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    if (selectedElements.contains(results.get(i).element())) {
                        index = i;
                        break;
                    }
                }
            }

            hoverResult = results.get(Math.max(0, index));
            nextHoverResult = results.get((index + 1) % results.size());

            // Highlight the hovered element
            float selectionWidth = selectedElements.contains(hoverResult.element()) ? 1.25f : 1.0f;
            drawSelection(graphics, hoverResult.element(), pose, selectionWidth, false);
        }

        graphics.flush();

        // Highlight the selected element
        for (FurnitureData.Element selectedElement : selectedElements) {
            drawSelection(graphics, selectedElement, pose, 0.6f, true);
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
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(matrix4f, 0.0f, 0.001f, 0.0f).uv(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, 0.0f, 0.001f, h).uv(0.0f, h / 8.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, w, 0.001f, h).uv(w / 8.0f, h / 8.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        builder.vertex(matrix4f, w, 0.001f, 0.0f).uv(w / 8.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 0.5f).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    void drawSelection(GuiGraphics graphics, FurnitureData.Element element, Matrix4f pose, float width, boolean overlay) {
        float[] fs = ClientModelUtils.getShapeData(element);
        for (Direction facing : Direction.values()) {
            Vector3f[] vertices = ClientModelUtils.getVertices(element, facing, fs, pose);
            for (int i = 0; i < 4; i++) {
                Vector3f vertex = vertices[i];
                Vector3f nextVertex = vertices[(i + 1) % 4];
                float adjustedWidth = element.type == FurnitureData.ElementType.PLAYER_POSE && facing == Direction.NORTH ? (1.5f + width * 0.5f) : width;
                line(graphics, vertex.x(), vertex.y(), vertex.z(), nextVertex.x(), nextVertex.y(), nextVertex.z(), adjustedWidth, overlay, 0.0f, 0.0f, 0.0f, 1.0f);
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

            // Autosave functionality
            int autosaveInterval = Config.getInstance().autosaveInterval;
            if (autosaveInterval >= 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAutosaveTime > autosaveInterval * 1000L) {
                    FurnitureDataManager.save(data, new ResourceLocation("local", "autosave"));
                    lastAutosaveTime = currentTime;
                }
            }
        }
    }

    public Optional<FurnitureData.Element> getFirstElement() {
        return selectedElements.isEmpty()
                ? Optional.empty()
                : Optional.of(selectedElements.get(0));
    }

    public boolean isFirstElement(FurnitureData.ElementType type) {
        return getFirstElement().filter(e -> e.type == type).isPresent();
    }

    public void selectElement(FurnitureData.Element element, boolean add) {
        if (!add) {
            selectedElements.clear();
        }
        if (!selectedElements.contains(element)) {
            selectedElements.add(element);
        }
    }
}
