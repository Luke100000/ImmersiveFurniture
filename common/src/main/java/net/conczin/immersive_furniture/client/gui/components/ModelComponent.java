package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.BoundedDoubleSlider;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import org.joml.Vector3i;

import java.util.List;

import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen.TOOLS_WIDTH;
import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.getParticleEngine;

public class ModelComponent extends ScreenComponent {
    static final Component SELECT_TITLE = Component.translatable("gui.immersive_furniture.select");
    static final Component POSITION_TITLE = Component.translatable("gui.immersive_furniture.position");
    static final Component SIZE_TITLE = Component.translatable("gui.immersive_furniture.size");
    static final Component ROTATION_TITLE = Component.translatable("gui.immersive_furniture.rotation");
    static final Component MOVE_FURNITURE_TITLE = Component.translatable("gui.immersive_furniture.move_furniture");
    static final Component FURNITURE_DIMENSION_TITLE = Component.translatable("gui.immersive_furniture.furniture_dimension");

    static final Component FIELD_TITLE = Component.literal("");

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

        // Furniture movement buttons - only shown when no element is selected
        if (screen.selectedElement == null) {
            int x = leftPos + 6 + 12;
            int y = topPos + 108;
            int spacing = 24;

            // X offset
            addButton(x, y, 16, 160, 224, "gui.immersive_furniture.move_furniture_west", () -> moveFurniture(-1.0f, 0, 0));
            addButton(x, y + 29, 16, 192, 224, "gui.immersive_furniture.move_furniture_east", () -> moveFurniture(1.0f, 0, 0));

            // Y offset
            addButton(x + spacing, y, 16, 160, 224, "gui.immersive_furniture.move_furniture_down", () -> moveFurniture(0, -1.0f, 0));
            addButton(x + spacing, y + 29, 16, 192, 224, "gui.immersive_furniture.move_furniture_up", () -> moveFurniture(0, 1.0f, 0));

            // Z offset
            addButton(x + spacing * 2, y, 16, 160, 224, "gui.immersive_furniture.move_furniture_north", () -> moveFurniture(0, 0, -1.0f));
            addButton(x + spacing * 2, y + 29, 16, 192, 224, "gui.immersive_furniture.move_furniture_south", () -> moveFurniture(0, 0, 1.0f));

            int dimY = topPos + 45;
            int maxDimension = 4;

            // X dimension
            addButton(x, dimY, 16, 160, 224, "", () -> screen.data.size.x = Math.min(maxDimension, screen.data.size.x + 1));
            addButton(x, dimY + 29, 16, 192, 224, "", () -> screen.data.size.x = Math.max(1, screen.data.size.x - 1));

            // Y dimension
            addButton(x + spacing, dimY, 16, 160, 224, "", () -> screen.data.size.y = Math.min(maxDimension, screen.data.size.y + 1));
            addButton(x + spacing, dimY + 29, 16, 192, 224, "", () -> screen.data.size.y = Math.max(1, screen.data.size.y - 1));

            // Z dimension
            addButton(x + spacing * 2, dimY, 16, 160, 224, "", () -> screen.data.size.z = Math.min(maxDimension, screen.data.size.z + 1));
            addButton(x + spacing * 2, dimY + 29, 16, 192, 224, "", () -> screen.data.size.z = Math.max(1, screen.data.size.z - 1));
        }

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
        int y = topPos + 17;
        px = addNewFloatBox(leftPos + 6, y, 28);
        px.setValue(Float.toString(screen.selectedElement.from.x));
        px.setResponder(b -> {
            if (screen.selectedElement == null) return;
            float offset = parse(px.getValue(), screen.selectedElement.from.x) - screen.selectedElement.from.x;
            screen.selectedElement.from.x += offset;
            screen.selectedElement.to.x += offset;
            screen.selectedElement.sanityCheck();
        });
        py = addNewFloatBox(leftPos + 6 + 30, y, 28);
        py.setValue(Float.toString(screen.selectedElement.from.y));
        py.setResponder(b -> {
            if (screen.selectedElement == null) return;
            float offset = parse(py.getValue(), screen.selectedElement.from.y) - screen.selectedElement.from.y;
            screen.selectedElement.from.y += offset;
            screen.selectedElement.to.y += offset;
            screen.selectedElement.sanityCheck();
        });
        pz = addNewFloatBox(leftPos + 6 + 30 * 2, y, 28);
        pz.setValue(Float.toString(screen.selectedElement.from.z));
        pz.setResponder(b -> {
            if (screen.selectedElement == null) return;
            float offset = parse(pz.getValue(), screen.selectedElement.from.z) - screen.selectedElement.from.z;
            screen.selectedElement.from.z += offset;
            screen.selectedElement.to.z += offset;
            screen.selectedElement.sanityCheck();
        });

        // Size
        if (screen.selectedElement.type != FurnitureData.ElementType.SPRITE && screen.selectedElement.type != FurnitureData.ElementType.PLAYER_POSE) {
            y = topPos + 45;
            Vector3i size = screen.selectedElement.getSize();
            sx = addNewFloatBox(leftPos + 6, y, 28);
            sx.setValue(String.valueOf(size.x));
            sx.setResponder(b -> {
                if (screen.selectedElement == null) return;
                int oldSize = screen.selectedElement.getSize().x;
                int newSize = Math.max(0, parse(sx.getValue(), oldSize));
                screen.selectedElement.from.x -= (newSize - oldSize) / 2.0f;
                screen.selectedElement.to.x += (newSize - oldSize) / 2.0f;
                screen.selectedElement.sanityCheck();
            });
            sy = addNewFloatBox(leftPos + 6 + 30, y, 28);
            sy.setValue(String.valueOf(size.y));
            sy.setResponder(b -> {
                if (screen.selectedElement == null) return;
                int oldSize = screen.selectedElement.getSize().y;
                int newSize = Math.max(0, parse(sy.getValue(), oldSize));
                screen.selectedElement.from.y -= (newSize - oldSize) / 2.0f;
                screen.selectedElement.to.y += (newSize - oldSize) / 2.0f;
                screen.selectedElement.sanityCheck();
            });
            sz = addNewFloatBox(leftPos + 6 + 30 * 2, y, 28);
            sz.setValue(String.valueOf(size.z));
            sz.setResponder(b -> {
                if (screen.selectedElement == null) return;
                int oldSize = screen.selectedElement.getSize().z;
                int newSize = Math.max(0, parse(sz.getValue(), oldSize));
                screen.selectedElement.from.z -= (newSize - oldSize) / 2.0f;
                screen.selectedElement.to.z += (newSize - oldSize) / 2.0f;
                screen.selectedElement.sanityCheck();
            });
        }

        if (screen.selectedElement.type != FurnitureData.ElementType.PLAYER_POSE) {
            // Rotation
            y = topPos + 73;
            rx = addToggleButton(leftPos + 6, y, 16, 16, 96, null, () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.axis = Direction.Axis.X;
                rx.setEnabled(false);
                ry.setEnabled(true);
                rz.setEnabled(true);
            });
            rx.setEnabled(screen.selectedElement.axis != Direction.Axis.X);
            ry = addToggleButton(leftPos + 24, y, 16, 32, 96, null, () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.axis = Direction.Axis.Y;
                rx.setEnabled(true);
                ry.setEnabled(false);
                rz.setEnabled(true);
            });
            ry.setEnabled(screen.selectedElement.axis != Direction.Axis.Y);
            rz = addToggleButton(leftPos + 42, y, 16, 48, 96, null, () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.axis = Direction.Axis.Z;
                rx.setEnabled(true);
                ry.setEnabled(true);
                rz.setEnabled(false);
            });
            rz.setEnabled(screen.selectedElement.axis != Direction.Axis.Z);

            addButton(leftPos + 62, y + 1, 14, 26, 228, null, () ->
                    screen.selectedElement.rotation = (screen.selectedElement.rotation + 22.5f) % 360);
            addButton(leftPos + 78, y + 1, 14, 42, 228, null, () ->
                    screen.selectedElement.rotation = (screen.selectedElement.rotation - 22.5f) % 360);
        }

        // Element type
        for (FurnitureData.ElementType type : FurnitureData.ElementType.values()) {
            addToggleButton(leftPos + 6 + type.ordinal() * 18, topPos + 94, 16, 176 + type.ordinal() * 16, 96, "gui.immersive_furniture.element_type." + type.name().toLowerCase(), () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.type = type;
                screen.selectedElement.sanityCheck();
                screen.init();
            }).setEnabled(screen.selectedElement.type != type);
        }

        if (screen.selectedElement.type == FurnitureData.ElementType.PARTICLE_EMITTER) {
            // Direction velocity
            BoundedDoubleSlider directionalVelocitySlider = new BoundedDoubleSlider(leftPos + 6, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.directional_velocity",
                    screen.selectedElement.particleEmitter.velocityDirectional, 0, 5.0);
            directionalVelocitySlider.setCallback(v -> screen.selectedElement.particleEmitter.velocityDirectional = v.floatValue());
            screen.addRenderableWidget(directionalVelocitySlider);

            // Random velocity
            BoundedDoubleSlider velocityRandomSlider = new BoundedDoubleSlider(leftPos + 8 + (width - 14) / 2, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.random_velocity",
                    screen.selectedElement.particleEmitter.velocityRandom, 0, 5.0);
            velocityRandomSlider.setCallback(v -> screen.selectedElement.particleEmitter.velocityRandom = v.floatValue());
            screen.addRenderableWidget(velocityRandomSlider);

            // Particle amount
            BoundedDoubleSlider amountSlider = new BoundedDoubleSlider(leftPos + 6, topPos + 134, width - 32, 20,
                    "gui.immersive_furniture.particle_amount",
                    screen.selectedElement.particleEmitter.amount, 0, 4.0);
            amountSlider.setCallback(v -> screen.selectedElement.particleEmitter.amount = v.floatValue());
            screen.addRenderableWidget(amountSlider);

            // Particle settings
            addToggleButton(leftPos + width - 23, topPos + 136, 16, 32, 128, "gui.immersive_furniture.on_interact", () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.particleEmitter.onInteract = !screen.selectedElement.particleEmitter.onInteract;
                screen.init();

                // Show particles on interacting
                ClientLevel level = Minecraft.getInstance().level;
                LocalPlayer player = Minecraft.getInstance().player;
                if (level != null && player != null && screen.selectedElement.particleEmitter.onInteract) {
                    screen.data.emitInteractParticles(player.getOnPos(), player, getParticleEngine(screen.data)::addParticle, true);
                }
            }).setEnabled(!screen.selectedElement.particleEmitter.onInteract);
        } else if (screen.selectedElement.type == FurnitureData.ElementType.SOUND_EMITTER) {
            // Volume
            BoundedDoubleSlider volumeSlider = new BoundedDoubleSlider(leftPos + 6, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.volume",
                    screen.selectedElement.soundEmitter.volume, 0, 2.0);
            volumeSlider.setCallback(v -> screen.selectedElement.soundEmitter.volume = v.floatValue());
            screen.addRenderableWidget(volumeSlider);

            // Pitch
            BoundedDoubleSlider velocityRandomSlider = new BoundedDoubleSlider(leftPos + 8 + (width - 14) / 2, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.pitch",
                    screen.selectedElement.soundEmitter.pitch, 0.5, 2.0);
            velocityRandomSlider.setCallback(v -> screen.selectedElement.soundEmitter.pitch = v.floatValue());
            screen.addRenderableWidget(velocityRandomSlider);

            // Frequency
            BoundedDoubleSlider amountSlider = new BoundedDoubleSlider(leftPos + 6, topPos + 134, width - 32, 20,
                    "gui.immersive_furniture.frequency",
                    screen.selectedElement.soundEmitter.frequency, 0.0, 1.0);
            amountSlider.setCallback(v -> screen.selectedElement.soundEmitter.frequency = v.floatValue());
            screen.addRenderableWidget(amountSlider);

            // Sound settings
            addToggleButton(leftPos + width - 23, topPos + 136, 16, 32, 128, "gui.immersive_furniture.on_interact", () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.soundEmitter.onInteract = !screen.selectedElement.soundEmitter.onInteract;
                if (screen.selectedElement.soundEmitter.onInteract) {
                    screen.selectedElement.soundEmitter.frequency = 0.0f;
                } else {
                    screen.selectedElement.soundEmitter.frequency = 0.1f;
                }
                screen.init();

                // Play sound on interacting
                ClientLevel level = Minecraft.getInstance().level;
                LocalPlayer player = Minecraft.getInstance().player;
                if (level != null && player != null && screen.selectedElement.soundEmitter.onInteract) {
                    screen.data.playInteractSound(level, player.getOnPos(), player);
                }
            }).setEnabled(!screen.selectedElement.soundEmitter.onInteract);
        } else if (screen.selectedElement.type == FurnitureData.ElementType.PLAYER_POSE) {
            // Pose settings
            List<Pose> poses = List.of(Pose.SITTING, Pose.SLEEPING);
            for (int i = 0; i < poses.size(); i++) {
                Pose pose = poses.get(i);
                addToggleButton(leftPos + 6 + i * 18, topPos + 114, 16, i * 16, 128, "gui.immersive_furniture.player_pose." + pose.name().toLowerCase(), () -> {
                    if (screen.selectedElement == null) return;
                    screen.selectedElement.playerPose.pose = pose;
                    screen.selectedElement.sanityCheck();
                    screen.init();
                }).setEnabled(screen.selectedElement.playerPose.pose != pose);
            }
        } else if (screen.selectedElement.type == FurnitureData.ElementType.SPRITE) {
            // Rotation
            for (int i = 0; i < 360; i += 90) {
                final int rotation = i;
                addToggleButton(leftPos + 6 + i / 90 * 18, topPos + 114, 16, 160 + (i / 90) * 16, 224, "gui.immersive_furniture.rotation." + i, () -> {
                    if (screen.selectedElement == null) return;
                    screen.selectedElement.sprite.rotation = rotation;
                    screen.init();
                }).setEnabled(screen.selectedElement.sprite.rotation != rotation);
            }

            // Size
            addButton(leftPos + 6, topPos + 132, 16, 112, 96, "gui.immersive_furniture.decrease_size", () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.sprite.size = Math.max(0.25f, screen.selectedElement.sprite.size / 2.0f);
                screen.selectedElement.sanityCheck();
                screen.init();
            });
            addButton(leftPos + 24, topPos + 132, 16, 96, 96, "gui.immersive_furniture.increase_size", () -> {
                if (screen.selectedElement == null) return;
                screen.selectedElement.sprite.size = Math.min(1.0f, screen.selectedElement.sprite.size * 2.0f);
                screen.selectedElement.sanityCheck();
                screen.init();
            });
        }
    }

    private void moveFurniture(float xOffset, float yOffset, float zOffset) {
        for (FurnitureData.Element element : screen.data.elements) {
            element.from.x += xOffset;
            element.to.x += xOffset;

            element.from.y += yOffset;
            element.to.y += yOffset;

            element.from.z += zOffset;
            element.to.z += zOffset;

            element.sanityCheck();
        }
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

    public void render(GuiGraphics graphics) {
        if (screen.selectedElement == null) {
            // Titles
            graphics.drawString(minecraft.font, SELECT_TITLE, leftPos + 6, topPos + 6, 0xFFFFFF);

            int x = leftPos + 6 + 12;
            int y = topPos + 108;
            int spacing = 24;

            // furniture dimension
            int dimY = topPos + 45;
            graphics.drawCenteredString(minecraft.font, FURNITURE_DIMENSION_TITLE, leftPos + TOOLS_WIDTH / 2, dimY - 12, 0xFFFFFF);
            graphics.drawCenteredString(minecraft.font, "X: " + screen.data.size.x, x + 8, dimY + 19, 0xFFFFFF);
            graphics.drawCenteredString(minecraft.font, "Y: " + screen.data.size.y, x + 8 + spacing, dimY + 19, 0xFFFFFF);
            graphics.drawCenteredString(minecraft.font, "Z: " + screen.data.size.z, x + 8 + spacing * 2, dimY + 19, 0xFFFFFF);

            // Offset labels
            graphics.drawCenteredString(minecraft.font, MOVE_FURNITURE_TITLE, leftPos + TOOLS_WIDTH / 2, y - 12, 0xFFFFFF);
            graphics.drawString(minecraft.font, "X", x + 5, y + 19, 0xFFFFFF);
            graphics.drawString(minecraft.font, "Y", x + 5 + spacing, y + 19, 0xFFFFFF);
            graphics.drawString(minecraft.font, "Z", x + 5 + spacing * 2, y + 19, 0xFFFFFF);

            // Outlines
            renderSmoothOutline(graphics, leftPos + 4, dimY - 15, width - 8, 62, 0x44000000);
            renderSmoothOutline(graphics, leftPos + 4, y - 15, width - 8, 62, 0x44000000);
        } else {
            // Titles
            graphics.drawString(minecraft.font, POSITION_TITLE, leftPos + 6, topPos + 6, 0xFFFFFF);
            if (screen.selectedElement.type != FurnitureData.ElementType.SPRITE && screen.selectedElement.type != FurnitureData.ElementType.PLAYER_POSE) {
                graphics.drawString(minecraft.font, SIZE_TITLE, leftPos + 6, topPos + 34, 0xFFFFFF);
            }
            if (screen.selectedElement.type != FurnitureData.ElementType.PLAYER_POSE) {
                graphics.drawString(minecraft.font, ROTATION_TITLE, leftPos + 6, topPos + 62, 0xFFFFFF);
            }

            // Outlines
            renderSmoothOutline(graphics, leftPos + 4, topPos + 4, width - 8, 87, 0x44000000);
            renderSmoothOutline(graphics, leftPos + 4, topPos + 92, width - 8, height - 117, 0x44000000);
        }

        renderSmoothOutline(graphics, leftPos + 4, topPos + 156, width - 8, 20, 0x44000000);
    }

    public void renderSmoothOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 1, y, x + width - 1, y + 1, color);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
