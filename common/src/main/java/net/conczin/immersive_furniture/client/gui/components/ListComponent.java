package net.conczin.immersive_furniture.client.gui.components;

import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationEditorScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;

import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE;
import static net.conczin.immersive_furniture.client.gui.ArtisansWorkstationScreen.TEXTURE_SIZE;

public abstract class ListComponent extends ScreenComponent {
    static final Component SEARCH_TITLE = Component.translatable("itemGroup.search");
    static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    EditBox searchBox;

    int page = 0;

    public ListComponent(ArtisansWorkstationEditorScreen screen) {
        super(screen);

        assert minecraft.level != null;
        assert minecraft.player != null;
    }

    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);

        // Search box
        String oldSearch = searchBox != null ? searchBox.getValue() : "";
        this.searchBox = new EditBox(minecraft.font, leftPos + 6, topPos + 6, width - 12, minecraft.font.lineHeight + 3, SEARCH_TITLE);
        this.searchBox.setMaxLength(50);
        this.searchBox.setVisible(true);
        this.searchBox.setValue(oldSearch);
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setResponder(this::updateSearch);
        screen.addRenderableWidget(searchBox);

        // Page buttons
        screen.addRenderableWidget(
                new ImageButton(leftPos + 6, topPos + height - 21, 12, 15, 13, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                    page = Math.max(0, page - 1);
                    updateSearch(searchBox.getValue());
                })
        );
        screen.addRenderableWidget(
                new ImageButton(leftPos + width - 18, topPos + height - 21, 12, 15, 0, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                    page += 1;
                    updateSearch(searchBox.getValue());
                })
        );

        updateSearch(searchBox.getValue());
    }

    abstract int getPages();

    abstract void updateSearch(String search);

    public void render(GuiGraphics context) {
        context.drawCenteredString(minecraft.font, String.format("%s / %S", page + 1, getPages()), leftPos + width / 2, topPos + height - 17, 0xFFFFFF);
    }
}