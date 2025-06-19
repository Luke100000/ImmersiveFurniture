package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.LinkedList;
import java.util.List;

import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public class SoundsComponent extends ListComponent {
    static final int PAGE_SIZE = 7;

    List<ResourceLocation> allLocations = new LinkedList<>();
    List<ResourceLocation> locations = new LinkedList<>();
    List<Button> buttons = new LinkedList<>();

    public SoundsComponent(ArtisansWorkstationEditorScreen screen) {
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
                        screen.selectedElement.soundEmitter.sound = locations.get(finalI);
                    })
                    .bounds(leftPos + 5, y, width - 29, 18)
                    .build();
            screen.addRenderableWidget(button);
            buttons.add(button);

            // Sample sound button
            screen.addRenderableWidget(new ImageButton(
                    leftPos + width - 23, y, 18, 18, 238, 220, 18, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                    b -> {
                        if (finalI >= locations.size()) return;
                        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(locations.get(finalI));
                        if (soundEvent == null) return;
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0f, 1.0f));
                    },
                    Component.translatable("gui.immersive_furniture.play_sound")
            ));

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
        allLocations = BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .filter(p -> Utils.search(searchBox.getValue(), p.toString()))
                .sorted(ResourceLocation::compareTo)
                .toList();

        page = Math.min(page, getPages() - 1);
        locations = allLocations.subList(
                page * PAGE_SIZE,
                Math.min((page * PAGE_SIZE + PAGE_SIZE), allLocations.size())
        );

        for (int i = 0; i < buttons.size(); i++) {
            if (i < locations.size()) {
                ResourceLocation location = locations.get(i);
                Component message = Component.translatableWithFallback(
                        "subtitles." + location.getPath(),
                        Utils.capitalize(location)
                );
                buttons.get(i).setMessage(message);

                Component namespaceTooltip = Component.literal(Utils.capitalize(location.getNamespace())).withStyle(ChatFormatting.GRAY);
                buttons.get(i).setTooltip(Tooltip.create(message.copy().append("\n").append(namespaceTooltip)));

                buttons.get(i).active = true;
            } else {
                buttons.get(i).setMessage(Component.literal(""));
                buttons.get(i).setTooltip(Tooltip.create(Component.literal("")));
                buttons.get(i).active = false;
            }
        }
    }
}
