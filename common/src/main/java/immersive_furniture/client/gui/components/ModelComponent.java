package immersive_furniture.client.gui.components;

import immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
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

    public ModelComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        // New
        addButton(leftPos + 6, topPos + height - 22, 16, 64, 96, () -> {

        });

        if (screen.selectedElement == null) return;

        // Delete
        addButton(leftPos + 24, topPos + height - 22, 16, 80, 96, () -> {

        });

        // Position
        EditBox px = addNewFloatBox(leftPos + 6, topPos + 17, 24);
        px.setValue(Float.toString(screen.selectedElement.from.x));
        px.setResponder(b -> {
            float offset = parse(px.getValue(), screen.selectedElement.from.x) - screen.selectedElement.from.x;
            screen.selectedElement.from.x += offset;
            screen.selectedElement.to.x += offset;
        });
        EditBox py = addNewFloatBox(leftPos + 6 + 28, topPos + 17, 24);
        py.setValue(Float.toString(screen.selectedElement.from.y));
        py.setResponder(b -> {
            float offset = parse(py.getValue(), screen.selectedElement.from.y) - screen.selectedElement.from.y;
            screen.selectedElement.from.y += offset;
            screen.selectedElement.to.y += offset;
        });
        EditBox pz = addNewFloatBox(leftPos + 6 + 28 * 2, topPos + 17, 24);
        pz.setValue(Float.toString(screen.selectedElement.from.z));
        pz.setResponder(b -> {
            float offset = parse(pz.getValue(), screen.selectedElement.from.z) - screen.selectedElement.from.z;
            screen.selectedElement.from.z += offset;
            screen.selectedElement.to.z += offset;
        });

        // Size
        Vector3i size = screen.selectedElement.getSize();
        EditBox sx = addNewFloatBox(leftPos + 6, topPos + 47, 24);
        sx.setValue(String.valueOf(size.x));
        sx.setResponder(b -> {
            int oldSize = screen.selectedElement.getSize().x;
            int newSize = Integer.getInteger(sx.getValue(), oldSize);
            screen.selectedElement.from.x -= (newSize - oldSize) / 2.0f;
            screen.selectedElement.to.x += (newSize - oldSize) / 2.0f;
        });
        EditBox sy = addNewFloatBox(leftPos + 6 + 26, topPos + 47, 24);
        sy.setValue(String.valueOf(size.y));
        sy.setResponder(b -> {
            int oldSize = screen.selectedElement.getSize().y;
            int newSize = Integer.getInteger(sy.getValue(), oldSize);
            screen.selectedElement.from.y -= (newSize - oldSize) / 2.0f;
            screen.selectedElement.to.y += (newSize - oldSize) / 2.0f;
        });
        EditBox sz = addNewFloatBox(leftPos + 6 + 26 * 2, topPos + 47, 24);
        sz.setValue(String.valueOf(size.z));
        sz.setResponder(b -> {
            int oldSize = screen.selectedElement.getSize().z;
            int newSize = Integer.getInteger(sz.getValue(), oldSize);
            screen.selectedElement.from.z -= (newSize - oldSize) / 2.0f;
            screen.selectedElement.to.z += (newSize - oldSize) / 2.0f;
        });

        // Rotation
        addButton(leftPos + 6, topPos + 77, 16, 16, 96, () -> {
            screen.selectedElement.axis = Direction.Axis.X;
        });
        addButton(leftPos + 24, topPos + 77, 16, 32, 96, () -> {
            screen.selectedElement.axis = Direction.Axis.Y;
        });
        addButton(leftPos + 42, topPos + 77, 16, 48, 96, () -> {
            screen.selectedElement.axis = Direction.Axis.Z;
        });
        addButton(leftPos + 64, topPos + 78, 14, 26, 228, () -> {
            screen.selectedElement.rotation = (screen.selectedElement.rotation + 22.5f) % 360;
        });
        addButton(leftPos + 80, topPos + 78, 14, 41, 228, () -> {
            screen.selectedElement.rotation = (screen.selectedElement.rotation - 22.5f) % 360;
        });
    }

    private ImageButton addButton(int x, int y, int size, int u, int v, Runnable clicked) {
        return screen.addRenderableWidget(
                new ImageButton(x, y, size, size, u, v, size, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> clicked.run())
        );
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
