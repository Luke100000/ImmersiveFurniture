package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedList;
import java.util.List;

public class ParticlesComponent extends ListComponent {
    private static final int PARTICLE_BUTTON_HEIGHT = 18;
    private static final int PARTICLE_BUTTON_SPACING = 19;
    private static final int PARTICLE_START_Y = 23;
    private static final int PARTICLE_BOTTOM_MARGIN = 25;

    List<ResourceLocation> allLocations = new LinkedList<>();
    List<ResourceLocation> locations = new LinkedList<>();
    List<Button> buttons = new LinkedList<>();

    public ParticlesComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        if (screen.selectedElements.isEmpty()) {
            return;
        }

        // Buttons
        buttons.clear();
        int y = topPos + PARTICLE_START_Y;
        for (int i = 0; i < getParticleRows(height); i++) {
            int finalI = i;
            Button button = Button.builder(Component.literal(""), b -> {
                        if (finalI >= locations.size()) return;
                        screen.selectedElements.forEach(e -> e.particleEmitter.particle = locations.get(finalI));
                    })
                    .bounds(leftPos + 5, y, width - 10, PARTICLE_BUTTON_HEIGHT)
                    .build();
            screen.addRenderableWidget(button);
            buttons.add(button);

            y += PARTICLE_BUTTON_SPACING;
        }

        super.init(leftPos, topPos, width, height);
    }

    @Override
    int getPages() {
        return Math.max(1, (allLocations.size() - 1) / buttons.size() + 1);
    }

    private int getParticleRows(int height) {
        return Math.max(1, (height - PARTICLE_START_Y - PARTICLE_BOTTOM_MARGIN) / PARTICLE_BUTTON_SPACING);
    }

    @Override
    void updateSearch() {
        allLocations = BuiltInRegistries.PARTICLE_TYPE.keySet().stream()
                .filter(p -> Utils.search(searchBox.getValue(), p.toString()))
                .sorted(ResourceLocation::compareTo)
                .toList();

        page = Math.min(page, getPages() - 1);
        locations = allLocations.stream()
                .skip((long) page * buttons.size())
                .limit(buttons.size())
                .toList();

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (i < locations.size()) {
                ResourceLocation location = locations.get(i);
                Component message = Component.literal(Utils.capitalize(location));
                button.setMessage(message);

                Component namespaceTooltip = Component.literal(Utils.capitalize(location.getNamespace())).withStyle(ChatFormatting.GRAY);
                button.setTooltip(Tooltip.create(message.copy().append("\n").append(namespaceTooltip)));

                button.active = true;
            } else {
                button.setMessage(Component.literal(""));
                button.setTooltip(Tooltip.create(Component.literal("")));
                button.active = false;
            }
        }
    }
}
