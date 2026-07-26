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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;

public class Client {
    public static final String ATLAS_REFRESH_COMMAND = "/immersive_furniture_refresh_atlas";

    private static boolean atlasRefreshInProgress;
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
        if (!DynamicAtlas.BAKED.isFull() || config.disableAtlasRefreshes || atlasRefreshInProgress || atlasRefreshPromptShown) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        atlasRefreshPromptShown = true;
        Component refresh = Component.translatable("immersive_furniture.atlas_refresh")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ATLAS_REFRESH_COMMAND))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("immersive_furniture.atlas_refresh.tooltip")
                        ))
                );
        player.sendSystemMessage(Component.translatable("immersive_furniture.atlas_full", refresh));
    }

    public static void refreshAtlas() {
        Config config = Config.getInstance();
        if (!DynamicAtlas.BAKED.isFull() || config.disableAtlasRefreshes || atlasRefreshInProgress) {
            return;
        }

        config.maxMipLevel = Math.max(0, config.maxMipLevel - 1);
        atlasRefreshInProgress = true;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.reloadResourcePacks().whenComplete((unused, error) ->
                minecraft.execute(() -> {
                    atlasRefreshInProgress = false;
                    if (error != null) atlasRefreshPromptShown = false;
                })
        );
    }

    public static void dependencyWarn(Player player, String key) {
        player.sendSystemMessage(Component.translatable("immersive_furniture." + key + "_missing"));
    }
}
