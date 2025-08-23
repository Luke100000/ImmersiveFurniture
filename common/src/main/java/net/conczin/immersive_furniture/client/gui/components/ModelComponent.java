package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.client.gui.widgets.BoundedDoubleSlider;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.ModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
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
        addButton(leftPos + 6, topPos + height - 22, 16, 64, 192, "gui.immersive_furniture.new_element", () -> {
            FurnitureData.Element element = new FurnitureData.Element();
            screen.data.elements.add(element);
            screen.selectElement(element, false);
            screen.init();
        });

        // Furniture movement buttons
        if (screen.selectedElements.isEmpty()) {
            int x = leftPos + 6 + 12;
            int y = topPos + 108;
            int spacing = 24;

            // X offset
            addButton(x, y, 16, 96, 160, "gui.immersive_furniture.move_furniture_east", () -> moveFurniture(1.0f, 0, 0));
            addButton(x, y + 29, 16, 128, 160, "gui.immersive_furniture.move_furniture_west", () -> moveFurniture(-1.0f, 0, 0));

            // Y offset
            addButton(x + spacing, y, 16, 96, 160, "gui.immersive_furniture.move_furniture_up", () -> moveFurniture(0, 1.0f, 0));
            addButton(x + spacing, y + 29, 16, 128, 160, "gui.immersive_furniture.move_furniture_down", () -> moveFurniture(0, -1.0f, 0));

            // Z offset
            addButton(x + spacing * 2, y, 16, 96, 160, "gui.immersive_furniture.move_furniture_south", () -> moveFurniture(0, 0, 1.0f));
            addButton(x + spacing * 2, y + 29, 16, 128, 160, "gui.immersive_furniture.move_furniture_north", () -> moveFurniture(0, 0, -1.0f));

            int dimY = topPos + 45;
            int maxDimension = 4;

            // X dimension
            addButton(x, dimY, 16, 96, 160, "", () -> screen.data.size.x = Math.min(maxDimension, screen.data.size.x + 1));
            addButton(x, dimY + 29, 16, 128, 160, "", () -> screen.data.size.x = Math.max(1, screen.data.size.x - 1));

            // Y dimension
            addButton(x + spacing, dimY, 16, 96, 160, "", () -> screen.data.size.y = Math.min(maxDimension, screen.data.size.y + 1));
            addButton(x + spacing, dimY + 29, 16, 128, 160, "", () -> screen.data.size.y = Math.max(1, screen.data.size.y - 1));

            // Z dimension
            addButton(x + spacing * 2, dimY, 16, 96, 160, "", () -> screen.data.size.z = Math.min(maxDimension, screen.data.size.z + 1));
            addButton(x + spacing * 2, dimY + 29, 16, 128, 160, "", () -> screen.data.size.z = Math.max(1, screen.data.size.z - 1));
        }

        FurnitureData.Element firstElement = screen.getFirstElement().orElse(null);
        if (firstElement == null) return;

        // Delete
        addButton(leftPos + 24, topPos + height - 22, 16, 80, 192, "gui.immersive_furniture.delete_element", () -> {
            screen.data.elements.removeAll(screen.selectedElements);
            screen.selectedElements.clear();
            screen.init();
        });

        // Duplicate
        addButton(leftPos + 42, topPos + height - 22, 16, 160, 192, "gui.immersive_furniture.duplicate_element", this::duplicateElements);

        // Mask toggle
        int u = 208 + (firstElement.mask - 1) * 16;
        addToggleButton(leftPos + 78, topPos + height - 22, 16, u, 224, "gui.immersive_furniture.mask." + firstElement.mask, () -> {
            screen.selectedElements.forEach(e -> e.mask = e.mask % 3 + 1);
            screen.init();
        }).setEnabled(false);

        // Position
        int y = topPos + 17;
        px = addNewFloatBox(leftPos + 6, y, 28);
        px.setValue(Float.toString(firstElement.from.x));
        px.setResponder(b -> screen.getFirstElement().ifPresent(currentFirstElement -> {
            float offset = parse(px.getValue(), currentFirstElement.from.x) - currentFirstElement.from.x;
            if (Math.abs(offset) < 0.001) return;
            currentFirstElement.from.x += offset;
            currentFirstElement.to.x += offset;
            for (FurnitureData.Element element : screen.selectedElements) {
                element.from.x = currentFirstElement.from.x;
                element.to.x = currentFirstElement.to.x;
                element.sanityCheck();
            }
        }));
        py = addNewFloatBox(leftPos + 6 + 30, y, 28);
        py.setValue(Float.toString(firstElement.from.y));
        py.setResponder(b -> screen.getFirstElement().ifPresent(currentFirstElement -> {
            float offset = parse(py.getValue(), currentFirstElement.from.y) - currentFirstElement.from.y;
            if (Math.abs(offset) < 0.001) return;
            currentFirstElement.from.y += offset;
            currentFirstElement.to.y += offset;
            for (FurnitureData.Element element : screen.selectedElements) {
                element.from.y = currentFirstElement.from.y;
                element.to.y = currentFirstElement.to.y;
                element.sanityCheck();
            }
        }));
        pz = addNewFloatBox(leftPos + 6 + 30 * 2, y, 28);
        pz.setValue(Float.toString(firstElement.from.z));
        pz.setResponder(b -> screen.getFirstElement().ifPresent(currentFirstElement -> {
            float offset = parse(pz.getValue(), currentFirstElement.from.z) - currentFirstElement.from.z;
            if (Math.abs(offset) < 0.001) return;
            currentFirstElement.from.z += offset;
            currentFirstElement.to.z += offset;
            for (FurnitureData.Element element : screen.selectedElements) {
                element.from.z = currentFirstElement.from.z;
                element.to.z = currentFirstElement.to.z;
                element.sanityCheck();
            }
        }));

        // Size
        if (isResizable(firstElement)) {
            y = topPos + 45;
            Vector3i size = firstElement.getSize();
            sx = addNewFloatBox(leftPos + 6, y, 28);
            sx.setValue(String.valueOf(size.x));
            sx.setResponder(b -> screen.getFirstElement().ifPresent(currentFirstElement -> {
                int oldSize = currentFirstElement.getSize().x;
                int newSize = Math.max(0, parse(sx.getValue(), oldSize));
                if (Math.abs(newSize - oldSize) < 0.001) return;
                currentFirstElement.from.x -= (newSize - oldSize) / 2.0f;
                currentFirstElement.to.x += (newSize - oldSize) / 2.0f;
                for (FurnitureData.Element element : screen.selectedElements) {
                    element.from.x = currentFirstElement.from.x;
                    element.to.x = currentFirstElement.to.x;
                    element.sanityCheck();
                }
            }));
            sy = addNewFloatBox(leftPos + 6 + 30, y, 28);
            sy.setValue(String.valueOf(size.y));
            sy.setResponder(b -> screen.getFirstElement().ifPresent(currentFirstElement -> {
                int oldSize = currentFirstElement.getSize().y;
                int newSize = Math.max(0, parse(sy.getValue(), oldSize));
                if (Math.abs(newSize - oldSize) < 0.001) return;
                currentFirstElement.from.y -= (newSize - oldSize) / 2.0f;
                currentFirstElement.to.y += (newSize - oldSize) / 2.0f;
                for (FurnitureData.Element element : screen.selectedElements) {
                    element.from.y = currentFirstElement.from.y;
                    element.to.y = currentFirstElement.to.y;
                    element.sanityCheck();
                }
            }));
            sz = addNewFloatBox(leftPos + 6 + 30 * 2, y, 28);
            sz.setValue(String.valueOf(size.z));
            sz.setResponder(b -> screen.getFirstElement().ifPresent(currentFirstElement -> {
                int oldSize = currentFirstElement.getSize().z;
                int newSize = Math.max(0, parse(sz.getValue(), oldSize));
                if (Math.abs(newSize - oldSize) < 0.001) return;
                currentFirstElement.from.z -= (newSize - oldSize) / 2.0f;
                currentFirstElement.to.z += (newSize - oldSize) / 2.0f;
                for (FurnitureData.Element element : screen.selectedElements) {
                    element.from.z = currentFirstElement.from.z;
                    element.to.z = currentFirstElement.to.z;
                    element.sanityCheck();
                }
            }));
        }

        // Rotation
        y = topPos + 73;
        rx = addToggleButton(leftPos + 6, y, 16, 16, 192, null, () -> {
            screen.selectedElements.forEach(e -> e.axis = Direction.Axis.X);
            rx.setEnabled(false);
            ry.setEnabled(true);
            rz.setEnabled(true);
        });
        rx.setEnabled(firstElement.axis != Direction.Axis.X);
        ry = addToggleButton(leftPos + 24, y, 16, 32, 192, null, () -> {
            screen.selectedElements.forEach(e -> e.axis = Direction.Axis.Y);
            rx.setEnabled(true);
            ry.setEnabled(false);
            rz.setEnabled(true);
        });
        ry.setEnabled(firstElement.axis != Direction.Axis.Y);
        rz = addToggleButton(leftPos + 42, y, 16, 48, 192, null, () -> {
            screen.selectedElements.forEach(e -> e.axis = Direction.Axis.Z);
            rx.setEnabled(true);
            ry.setEnabled(true);
            rz.setEnabled(false);
        });
        rz.setEnabled(firstElement.axis != Direction.Axis.Z);

        addButton(leftPos + 62, y + 1, 14, 222, 2, null, () -> rotate(22.5f));
        addButton(leftPos + 78, y + 1, 14, 206, 2, null, () -> rotate(-22.5f));

        // Element type
        for (FurnitureData.ElementType type : FurnitureData.ElementType.values()) {
            addToggleButton(leftPos + 6 + type.ordinal() * 18, topPos + 94, 16, 176 + type.ordinal() * 16, 192, "gui.immersive_furniture.element_type." + type.name().toLowerCase(), () -> {
                screen.selectedElements.forEach(e -> {
                    e.type = type;

                    // Update mask based on type: 1 for emitters, 3 otherwise
                    if (type == FurnitureData.ElementType.PARTICLE_EMITTER || type == FurnitureData.ElementType.SOUND_EMITTER) {
                        e.mask = 1;
                    } else {
                        e.mask = 3;
                    }
                    e.sanityCheck();
                });
                screen.init();
            }).setEnabled(firstElement.type != type);
        }

        if (firstElement.type == FurnitureData.ElementType.PARTICLE_EMITTER) {
            // Direction velocity
            BoundedDoubleSlider directionalVelocitySlider = new BoundedDoubleSlider(leftPos + 6, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.directional_velocity",
                    firstElement.particleEmitter.velocityDirectional, 0, 5.0);
            directionalVelocitySlider.setCallback(v -> screen.selectedElements.forEach(e -> e.particleEmitter.velocityDirectional = v.floatValue()));
            screen.addRenderableWidget(directionalVelocitySlider);

            // Random velocity
            BoundedDoubleSlider velocityRandomSlider = new BoundedDoubleSlider(leftPos + 8 + (width - 14) / 2, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.random_velocity",
                    firstElement.particleEmitter.velocityRandom, 0, 5.0);
            velocityRandomSlider.setCallback(v -> screen.selectedElements.forEach(e -> e.particleEmitter.velocityRandom = v.floatValue()));
            screen.addRenderableWidget(velocityRandomSlider);

            // Particle amount
            BoundedDoubleSlider amountSlider = new BoundedDoubleSlider(leftPos + 6, topPos + 134, width - 32, 20,
                    "gui.immersive_furniture.particle_amount",
                    firstElement.particleEmitter.amount, 0, 4.0);
            amountSlider.setCallback(v -> screen.selectedElements.forEach(e -> e.particleEmitter.amount = v.floatValue()));
            screen.addRenderableWidget(amountSlider);

            // Particle settings
            addToggleButton(leftPos + width - 23, topPos + 136, 16, 192, 160, "gui.immersive_furniture.on_interact", () -> {
                screen.selectedElements.forEach(e -> e.particleEmitter.onInteract = !e.particleEmitter.onInteract);
                screen.init();

                // Show particles on interacting
                ClientLevel level = Minecraft.getInstance().level;
                LocalPlayer player = Minecraft.getInstance().player;
                if (level != null && player != null && firstElement.particleEmitter.onInteract) {
                    screen.data.emitInteractParticles(player.getOnPos(), null, screen.currentState, player, getParticleEngine(screen.data)::addParticle, true);
                }
            }).setEnabled(!firstElement.particleEmitter.onInteract);
        } else if (firstElement.type == FurnitureData.ElementType.SOUND_EMITTER) {
            // Volume
            BoundedDoubleSlider volumeSlider = new BoundedDoubleSlider(leftPos + 6, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.volume",
                    firstElement.soundEmitter.volume, 0, 2.0);
            volumeSlider.setCallback(v -> screen.selectedElements.forEach(e -> e.soundEmitter.volume = v.floatValue()));
            screen.addRenderableWidget(volumeSlider);

            // Pitch
            BoundedDoubleSlider velocityRandomSlider = new BoundedDoubleSlider(leftPos + 8 + (width - 14) / 2, topPos + 112, (width - 14) / 2, 20,
                    "gui.immersive_furniture.pitch",
                    firstElement.soundEmitter.pitch, 0.5, 2.0);
            velocityRandomSlider.setCallback(v -> screen.selectedElements.forEach(e -> e.soundEmitter.pitch = v.floatValue()));
            screen.addRenderableWidget(velocityRandomSlider);

            // Frequency
            if (!firstElement.soundEmitter.onInteract) {
                BoundedDoubleSlider frequencySlider = new BoundedDoubleSlider(leftPos + 6, topPos + 134, width - 32, 20,
                        "gui.immersive_furniture.frequency",
                        firstElement.soundEmitter.frequency, 0.0, 1.0);
                frequencySlider.setCallback(v -> screen.selectedElements.forEach(e -> e.soundEmitter.frequency = v.floatValue()));
                screen.addRenderableWidget(frequencySlider);
            }

            // Sound settings
            addToggleButton(leftPos + width - 23, topPos + 136, 16, 192, 160, "gui.immersive_furniture.on_interact", () -> {
                screen.selectedElements.forEach(e -> {
                    e.soundEmitter.onInteract = !e.soundEmitter.onInteract;
                    e.soundEmitter.frequency = e.soundEmitter.onInteract ? 0.0f : 0.1f;
                });
                screen.init();

                // Play sound on interacting
                ClientLevel level = Minecraft.getInstance().level;
                LocalPlayer player = Minecraft.getInstance().player;
                if (level != null && player != null && firstElement.soundEmitter.onInteract) {
                    screen.data.playInteractSound(level, player.getOnPos(), screen.currentState, player);
                }
            }).setEnabled(!firstElement.soundEmitter.onInteract);
        } else if (firstElement.type == FurnitureData.ElementType.PLAYER_POSE) {
            // Pose settings
            List<Pose> poses = List.of(Pose.SITTING, Pose.SLEEPING);
            for (int i = 0; i < poses.size(); i++) {
                Pose pose = poses.get(i);
                addToggleButton(leftPos + 6 + i * 18, topPos + 114, 16, 160 + i * 16, 160, "gui.immersive_furniture.player_pose." + pose.name().toLowerCase(), () -> {
                    screen.selectedElements.forEach(e -> {
                        e.playerPose.pose = pose;
                        e.sanityCheck();
                    });
                    screen.init();
                }).setEnabled(firstElement.playerPose.pose != pose);
            }
        } else if (firstElement.type == FurnitureData.ElementType.SPRITE) {
            // Rotation
            for (int i = 0; i < 360; i += 90) {
                final int rotation = i;
                addToggleButton(leftPos + 6 + i / 90 * 18, topPos + 114, 16, 96 + (i / 90) * 16, 160, "gui.immersive_furniture.rotation." + i, () -> {
                    // TODO: Rotate around origin
                    screen.selectedElements.forEach(e -> e.sprite.rotation = rotation);
                    screen.init();
                }).setEnabled(firstElement.sprite.rotation != rotation);
            }

            // Size
            addButton(leftPos + 6, topPos + 132, 16, 112, 192, "gui.immersive_furniture.decrease_size", () -> {
                screen.selectedElements.forEach(e -> {
                    e.sprite.size = Math.max(0.25f, e.sprite.size / 2.0f);
                    e.sanityCheck();
                });
                screen.init();
            });
            addButton(leftPos + 24, topPos + 132, 16, 96, 192, "gui.immersive_furniture.increase_size", () -> {
                screen.selectedElements.forEach(e -> {
                    e.sprite.size = Math.min(2.0f, e.sprite.size * 2.0f);
                    e.sanityCheck();
                });
                screen.init();
            });

            // Tiled toggle
            addToggleButton(leftPos + 78, topPos + 132, 16, 144, 192, "gui.immersive_furniture.tiled", () -> {
                screen.selectedElements.forEach(e -> {
                    e.sprite.tiled = !e.sprite.tiled;
                    e.sanityCheck();
                });
                screen.init();
            }).setEnabled(!firstElement.sprite.tiled);
        }
    }

    private void rotate(float rotation) {
        // Rotate around the center
        if (screen.selectedElements.size() > 1) {
            Vector3f center = new Vector3f();
            screen.selectedElements.stream().map(FurnitureData.Element::getCenter).forEach(center::add);
            center.div(screen.selectedElements.size());

            for (FurnitureData.Element element : screen.selectedElements) {
                Vector3f offset = element.getCenter().sub(center);
                Vector3f newOffset = ModelUtils.rotate(new Vector3f(offset), element.axis, rotation);
                newOffset.sub(offset);
                element.move(
                        Math.round(newOffset.x * 4) / 4f,
                        Math.round(newOffset.y * 4) / 4f,
                        Math.round(newOffset.z * 4) / 4f
                );
            }
        }

        // Rotate each selected element
        for (FurnitureData.Element element : screen.selectedElements) {
            element.rotation = (element.rotation + rotation) % 360;
        }
    }

    public void duplicateElements() {
        ArrayList<FurnitureData.Element> duplicatedElements = new ArrayList<>(screen.selectedElements);
        if (duplicatedElements.isEmpty()) return;
        screen.selectedElements.clear();
        for (FurnitureData.Element element : duplicatedElements) {
            FurnitureData.Element newElement = new FurnitureData.Element(element);
            screen.data.elements.add(newElement);
            screen.selectedElements.add(newElement);
        }
        screen.init();
    }

    private boolean isResizable(FurnitureData.Element element) {
        return (element.type != FurnitureData.ElementType.SPRITE || element.sprite.tiled) && element.type != FurnitureData.ElementType.PLAYER_POSE;
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
        FurnitureData.Element firstElement = screen.getFirstElement().orElse(null);
        if (firstElement == null) return;

        px.setValue(Float.toString(firstElement.from.x));
        py.setValue(Float.toString(firstElement.from.y));
        pz.setValue(Float.toString(firstElement.from.z));

        if (sx != null) {
            Vector3i size = firstElement.getSize();
            sx.setValue(String.valueOf(size.x));
            sy.setValue(String.valueOf(size.y));
            sz.setValue(String.valueOf(size.z));
        }

        if (rx != null) {
            rx.setEnabled(firstElement.axis == Direction.Axis.X);
            ry.setEnabled(firstElement.axis == Direction.Axis.Y);
            rz.setEnabled(firstElement.axis == Direction.Axis.Z);
        }
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
        FurnitureData.Element firstElement = screen.getFirstElement().orElse(null);
        if (firstElement == null) {
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
            if (isResizable(firstElement)) {
                graphics.drawString(minecraft.font, SIZE_TITLE, leftPos + 6, topPos + 34, 0xFFFFFF);
            }
            if (firstElement.type != FurnitureData.ElementType.PLAYER_POSE) {
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
