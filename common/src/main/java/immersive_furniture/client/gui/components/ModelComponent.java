package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import immersive_furniture.client.gui.widgets.StateImageButton;
import immersive_furniture.data.FurnitureData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class ModelComponent extends ScreenComponent {
    static final Component SELECT_TITLE = Component.translatable("gui.immersive_furniture.select");
    static final Component POSITION_TITLE = Component.translatable("gui.immersive_furniture.position");
    static final Component SIZE_TITLE = Component.translatable("gui.immersive_furniture.size");
    static final Component ROTATION_TITLE = Component.translatable("gui.immersive_furniture.rotation");

    static final Component FIELD_TITLE = Component.translatable("gui.immersive_furniture.field");
    static final Component FIELD_HINT = Component.translatable("gui.immersive_furniture.field_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    private EditBox px;
    private EditBox py;
    private EditBox pz;

    private EditBox sx;
    private EditBox sy;
    private EditBox sz;

    private StateImageButton rx;
    private StateImageButton ry;
    private StateImageButton rz;

    public ModelComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        // New
        addButton(leftPos + 6, topPos + height - 22, 16, 64, 96, "gui.immersive_furniture.new_element", () -> {
            screen.selectedElement = new FurnitureData.Element();
            screen.data.elements.add(screen.selectedElement);
            screen.init();
        });

        if (screen.selectedElement == null) return;

        // Delete
        addButton(leftPos + 24, topPos + height - 22, 16, 80, 96, "gui.immersive_furniture.delete_element", () -> {
            screen.data.elements.remove(screen.selectedElement);
            screen.selectedElement = null;
            screen.init();
        });

        // Duplicate
        addButton(leftPos + 42, topPos + height - 22, 16, 160, 96, "gui.immersive_furniture.duplicate_element", () -> {
            screen.selectedElement = new FurnitureData.Element(screen.selectedElement);
            screen.data.elements.add(screen.selectedElement);
            screen.init();
        });

        // Position
        px = addNewFloatBox(leftPos + 6, topPos + 17, 28);
        px.setValue(Float.toString(screen.selectedElement.from.x));
        px.setResponder(b -> {
            float offset = parse(px.getValue(), screen.selectedElement.from.x) - screen.selectedElement.from.x;
            screen.selectedElement.from.x += offset;
            screen.selectedElement.to.x += offset;
        });
        py = addNewFloatBox(leftPos + 6 + 30, topPos + 17, 28);
        py.setValue(Float.toString(screen.selectedElement.from.y));
        py.setResponder(b -> {
            float offset = parse(py.getValue(), screen.selectedElement.from.y) - screen.selectedElement.from.y;
            screen.selectedElement.from.y += offset;
            screen.selectedElement.to.y += offset;
        });
        pz = addNewFloatBox(leftPos + 6 + 30 * 2, topPos + 17, 28);
        pz.setValue(Float.toString(screen.selectedElement.from.z));
        pz.setResponder(b -> {
            float offset = parse(pz.getValue(), screen.selectedElement.from.z) - screen.selectedElement.from.z;
            screen.selectedElement.from.z += offset;
            screen.selectedElement.to.z += offset;
        });

        // Size
        Vector3i size = screen.selectedElement.getSize();
        sx = addNewFloatBox(leftPos + 6, topPos + 47, 24);
        sx.setValue(String.valueOf(size.x));
        sx.setResponder(b -> {
            int oldSize = screen.selectedElement.getSize().x;
            int newSize = Math.max(0, parse(sx.getValue(), oldSize));
            screen.selectedElement.from.x -= (newSize - oldSize) / 2.0f;
            screen.selectedElement.to.x += (newSize - oldSize) / 2.0f;
        });
        sy = addNewFloatBox(leftPos + 6 + 30, topPos + 47, 28);
        sy.setValue(String.valueOf(size.y));
        sy.setResponder(b -> {
            int oldSize = screen.selectedElement.getSize().y;
            int newSize = Math.max(0, parse(sy.getValue(), oldSize));
            screen.selectedElement.from.y -= (newSize - oldSize) / 2.0f;
            screen.selectedElement.to.y += (newSize - oldSize) / 2.0f;
        });
        sz = addNewFloatBox(leftPos + 6 + 30 * 2, topPos + 47, 28);
        sz.setValue(String.valueOf(size.z));
        sz.setResponder(b -> {
            int oldSize = screen.selectedElement.getSize().z;
            int newSize = Math.max(0, parse(sz.getValue(), oldSize));
            screen.selectedElement.from.z -= (newSize - oldSize) / 2.0f;
            screen.selectedElement.to.z += (newSize - oldSize) / 2.0f;
        });

        // Rotation
        rx = addToggleButton(leftPos + 6, topPos + 77, 16, 16, 96, null, () -> {
            screen.selectedElement.axis = Direction.Axis.X;
            rx.setEnabled(true);
            ry.setEnabled(false);
            rz.setEnabled(false);
        });
        ry = addToggleButton(leftPos + 24, topPos + 77, 16, 32, 96, null, () -> {
            screen.selectedElement.axis = Direction.Axis.Y;
            rx.setEnabled(false);
            ry.setEnabled(true);
            rz.setEnabled(false);
        });
        rz = addToggleButton(leftPos + 42, topPos + 77, 16, 48, 96, null, () -> {
            screen.selectedElement.axis = Direction.Axis.Z;
            rx.setEnabled(false);
            ry.setEnabled(false);
            rz.setEnabled(true);
        });

        rx.setEnabled(screen.selectedElement.axis == Direction.Axis.X);
        ry.setEnabled(screen.selectedElement.axis == Direction.Axis.Y);
        rz.setEnabled(screen.selectedElement.axis == Direction.Axis.Z);

        addButton(leftPos + 62, topPos + 78, 14, 26, 228, null, () ->
                screen.selectedElement.rotation = (screen.selectedElement.rotation + 22.5f) % 360);
        addButton(leftPos + 78, topPos + 78, 14, 42, 228, null, () ->
                screen.selectedElement.rotation = (screen.selectedElement.rotation - 22.5f) % 360);
    }

    public void update() {
        if (screen.selectedElement == null) return;

        px.setValue(Float.toString(screen.selectedElement.from.x));
        py.setValue(Float.toString(screen.selectedElement.from.y));
        pz.setValue(Float.toString(screen.selectedElement.from.z));

        Vector3i size = screen.selectedElement.getSize();
        sx.setValue(String.valueOf(size.x));
        sy.setValue(String.valueOf(size.y));
        sz.setValue(String.valueOf(size.z));

        rx.setEnabled(screen.selectedElement.axis == Direction.Axis.X);
        ry.setEnabled(screen.selectedElement.axis == Direction.Axis.Y);
        rz.setEnabled(screen.selectedElement.axis == Direction.Axis.Z);
    }

    private EditBox addNewFloatBox(int x, int y, int width) {
        EditBox searchBox = new EditBox(minecraft.font, x, y, width, minecraft.font.lineHeight + 3, FIELD_TITLE);
        searchBox.setMaxLength(8);
        searchBox.setHint(FIELD_HINT);
        screen.addRenderableWidget(searchBox);
        return searchBox;
    }

    public float parse(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int parse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void render(GuiGraphics context) {
        if (screen.selectedElement == null) {
            context.drawString(minecraft.font, SELECT_TITLE, leftPos + 6, topPos + 6, 0xFFFFFF);
        } else {
            context.drawString(minecraft.font, POSITION_TITLE, leftPos + 6, topPos + 6, 0xFFFFFF);
            context.drawString(minecraft.font, SIZE_TITLE, leftPos + 6, topPos + 36, 0xFFFFFF);
            context.drawString(minecraft.font, ROTATION_TITLE, leftPos + 6, topPos + 66, 0xFFFFFF);
        }
    }
}
