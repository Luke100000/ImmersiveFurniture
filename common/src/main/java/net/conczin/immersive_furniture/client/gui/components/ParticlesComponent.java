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
    static final int PAGE_SIZE = 7;

    List<ResourceLocation> allLocations = new LinkedList<>();
    List<ResourceLocation> locations = new LinkedList<>();
    List<Button> buttons = new LinkedList<>();

    public ParticlesComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        if (screen.selectedElement == null) {
            return;
        }

        // Buttons
        buttons.clear();
        int y = topPos + 23;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int finalI = i;
            Button button = Button.builder(Component.literal(""), b -> {
                        if (screen.selectedElement == null) return;
                        if (finalI >= locations.size()) return;
                        screen.selectedElement.particleEmitter.particle = locations.get(finalI);
                    })
                    .bounds(leftPos + 5, y, width - 10, 18)
                    .build();
            screen.addRenderableWidget(button);
            buttons.add(button);

            y += 19;
        }

        super.init(leftPos, topPos, width, height);
    }

    @Override
    int getPages() {
        return Math.max(0, (allLocations.size() - 1) / PAGE_SIZE + 1);
    }

    @Override
    void updateSearch() {
        allLocations = BuiltInRegistries.PARTICLE_TYPE.keySet().stream()
                .filter(p -> Utils.search(searchBox.getValue(), p.toString()))
                .sorted(ResourceLocation::compareTo)
                .toList();

        page = Math.min(page, getPages() - 1);
        locations = allLocations.subList(
                page * PAGE_SIZE,
                Math.min((page * PAGE_SIZE + PAGE_SIZE), allLocations.size())
        );

        locations = allLocations.stream()
                .skip((long) page * PAGE_SIZE)
                .limit(PAGE_SIZE)
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
