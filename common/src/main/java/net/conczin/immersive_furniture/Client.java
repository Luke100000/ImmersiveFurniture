package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.client.DelayedFurnitureRenderer;
import net.conczin.immersive_furniture.client.AtlasSprite;
import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.TransparencyManager;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.network.ClientHandlerImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;

public class Client {
    private static final int MAX_ATLAS_SIZE = 8192;

    public static final String ATLAS_REFRESH_COMMAND = "/immersive_furniture_refresh_atlas";
    public static final String ATLAS_INCREASE_COMMAND = "/immersive_furniture_increase_atlas";

    private static boolean atlasRefreshPromptShown;

    public static void postLoad() {
        Common.clientHandler = new ClientHandlerImpl();

        // Load on the right thread
        DynamicAtlas.boostrap();
    }

    public static void onLevelLoad() {
        Common.clientHandler.stopAllFurnitureSounds();

        DynamicAtlas.BAKED.clear();
        DynamicAtlas.SCRATCH.clear();
        DynamicAtlas.ENTITY.clear();
        TransparencyManager.INSTANCE.clear();

        FurnitureDataManager.REQUESTED_DATA.clear();
        FurnitureDataManager.DATA.clear();
        DelayedFurnitureRenderer.INSTANCE.clear();
        InteractionManager.INSTANCE.clearInteraction();

        atlasRefreshPromptShown = false;
    }

    public static void tick() {
        AtlasSprite.syncBakedAtlas();
        DelayedFurnitureRenderer.INSTANCE.tick();
        refreshAtlasIfFull();
    }

    private static void refreshAtlasIfFull() {
        Config config = Config.getInstance();
        if (!DynamicAtlas.BAKED.isFull() || config.disableAtlasRefreshes || atlasRefreshPromptShown) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        atlasRefreshPromptShown = true;
        Component refresh = atlasAction("immersive_furniture.atlas_refresh", ATLAS_REFRESH_COMMAND);
        Component increase = atlasAction("immersive_furniture.atlas_increase", ATLAS_INCREASE_COMMAND);
        player.sendSystemMessage(Component.translatable("immersive_furniture.atlas_full", refresh).append(" ").append(increase));
    }

    private static Component atlasAction(String translationKey, String command) {
        return Component.translatable(translationKey)
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable(translationKey + ".tooltip")
                        ))
                );
    }

    public static void refreshAtlas() {
        Config config = Config.getInstance();
        if (!DynamicAtlas.BAKED.isFull() || config.disableAtlasRefreshes) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        DynamicAtlas.BAKED.clear();
        minecraft.levelRenderer.allChanged();
        atlasRefreshPromptShown = false;
    }

    public static void increaseAtlasSize() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Config config = Config.getInstance();
        int size = config.getBakedAtlasSize();
        if (size >= MAX_ATLAS_SIZE) {
            player.sendSystemMessage(Component.translatable("immersive_furniture.atlas_increase.maximum"));
            return;
        }

        config.bakedAtlasSize = Math.min(size * 2, MAX_ATLAS_SIZE);
        config.save();
        player.sendSystemMessage(Component.translatable("immersive_furniture.atlas_increase.restart", config.bakedAtlasSize, config.bakedAtlasSize));
    }

    public static void dependencyWarn(Player player, String key) {
        player.sendSystemMessage(Component.translatable("immersive_furniture." + key + "_missing"));
    }
}
