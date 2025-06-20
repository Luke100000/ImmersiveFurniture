package net.conczin.immersive_furniture.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.client.gui.widgets.StateImageButton;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.api.API;
import net.conczin.immersive_furniture.data.api.Auth;
import net.conczin.immersive_furniture.data.api.responses.*;
import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.s2c.CraftRequest;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.conczin.immersive_furniture.client.gui.components.SettingsComponent.TAGS;
import static net.conczin.immersive_furniture.data.FurnitureDataManager.REQUESTED_DATA;
import static net.conczin.immersive_furniture.data.FurnitureDataManager.getData;
import static net.conczin.immersive_furniture.data.api.API.request;

public class ArtisansWorkstationLibraryScreen extends ArtisansWorkstationScreen {
    static final Component SEARCH_TITLE = Component.translatable("itemGroup.search");
    static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);
    static final int ENTRIES_PER_PAGE = 8;

    public enum Tab {
        LOCAL,
        GLOBAL,
        FAVORITES,
        SUBMISSIONS,
    }

    private boolean uploading = false;
    private boolean awaitingAuthentication = false;
    private boolean awaitingSearch = false;
    private boolean shouldSearch = true;
    private String lastSearch = "";

    private boolean authenticating;
    private boolean isBrowserOpen = false;
    private boolean authenticated = false;

    private EditBox searchBox;
    private boolean sortByDate = false;
    private String tagFilter = "miscellaneous";

    Tab tab = Tab.GLOBAL;
    int page = 0;

    List<ResourceLocation> furniture = new LinkedList<>();
    List<ResourceLocation> localFiles;

    ResourceLocation selected = null;

    int lastMouseX;
    int lastMouseY;

    float previewYaw = 0.0f;
    float previewPitch = 0.0f;

    public ArtisansWorkstationLibraryScreen() {
        super();

        localFiles = FurnitureDataManager.getLocalFiles();

        // Check if the player is still authenticated
        authenticating = Auth.loadToken() != null;

        // Just in case a few jobs failed
        REQUESTED_DATA.clear();
    }

    @Override
    protected void init() {
        super.init();

        if (selected == null) {
            // Tabs
            int w = (windowWidth - 4) / 4;
            int x = leftPos;
            for (Tab tab : Tab.values()) {
                String text = "gui.immersive_furniture.tab." + tab.name().toLowerCase(Locale.ROOT);
                addRenderableWidget(
                        Button.builder(Component.translatable(text), b -> {
                                    this.tab = tab;
                                    this.page = 0;
                                    this.shouldSearch = true;
                                    init();
                                })
                                .bounds(x + 2, topPos - 19, w, 20)
                                .tooltip(Tooltip.create(Component.translatable(text + ".hint")))
                                .build()
                ).active = tab != this.tab;
                x += w;
            }

            // Search box
            this.searchBox = new EditBox(font, leftPos + 5, topPos + 5, windowWidth - 64 - 12, font.lineHeight + 3, SEARCH_TITLE);
            this.searchBox.setMaxLength(50);
            this.searchBox.setVisible(true);
            this.searchBox.setValue(lastSearch);
            this.searchBox.setHint(SEARCH_HINT);
            this.searchBox.setResponder(s -> {
                if (!s.equals(lastSearch)) {
                    shouldSearch = true;
                    lastSearch = s;
                }
            });
            addRenderableWidget(this.searchBox);
            setInitialFocus(searchBox);

            // Tags
            int i = 0;
            int tx = leftPos + 4;
            for (String tag : TAGS) {
                addToggleButton(tx, topPos + 18, 16, 48 + i * 16, 128, "gui.immersive_furniture.tag." + tag.toLowerCase(Locale.ROOT), b -> {
                    tagFilter = tag;
                    shouldSearch = true;
                    b.setEnabled(true);
                }).setEnabled(tag.equals(tagFilter));
                tx += 17;
                i++;
            }

            int y = topPos + windowHeight - 25;

            // Sort by date
            addRenderableWidget(
                    new ImageButton(leftPos + 3, y, 22, 22, sortByDate ? 88 : 66, 48, 22, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                        sortByDate = !sortByDate;
                        shouldSearch = true;
                        init();
                    })
            ).setTooltip(Tooltip.create(Component.translatable(sortByDate ? "gui.immersive_furniture.sort.favorites" : "gui.immersive_furniture.sort.date")));

            // Page buttons
            addRenderableWidget(
                    new ImageButton(leftPos + windowWidth / 2 - 24 - 6, y + 4, 12, 15, 13, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                        page = Math.max(0, page - 1);
                        shouldSearch = true;
                        init();
                    })
            );
            addRenderableWidget(
                    new ImageButton(leftPos + windowWidth / 2 + 24 - 6, y + 4, 12, 15, 0, 226, 15, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, b -> {
                        page += 1;
                        shouldSearch = true;
                        init();
                    })
            );

            // Login
            MutableComponent text = Component.translatable(authenticated || authenticating ? "gui.immersive_furniture.logout" : "gui.immersive_furniture.login");
            addRenderableWidget(
                    Button.builder(text, b -> {
                                if (authenticated || authenticating) {
                                    // Delete the token and log out
                                    Auth.clearToken();
                                    authenticated = false;
                                    authenticating = false;
                                } else {
                                    // Open a browser and start explicit authentication
                                    Auth.authenticate(getPlayerName());
                                    authenticating = true;
                                    isBrowserOpen = true;
                                }
                                init();
                            })
                            .bounds(leftPos + windowWidth - 64 - 4, topPos + 4, 64, 20)
                            .tooltip(Tooltip.create(Component.translatable("gui.immersive_furniture.login.tooltip")))
                            .build()
            );

            // Create button
            addRenderableWidget(
                    Button.builder(Component.translatable("gui.immersive_furniture.create"), b -> {
                                if (minecraft != null) {
                                    minecraft.setScreen(new ArtisansWorkstationEditorScreen(new FurnitureData()));
                                }
                            })
                            .bounds(leftPos + windowWidth - 64 - 4, topPos + windowHeight - 24, 64, 20)
                            .tooltip(Tooltip.create(Component.translatable("gui.immersive_furniture.create.hint")))
                            .build()
            );
        } else {
            // Favorite
            int x = leftPos + windowWidth / 2;
            int y = topPos + windowHeight - 25;

            if (!selected.getNamespace().equals("local")) {
                addToggleButton(x - 50 - 11, y, 22, 0, 48, "gui.immersive_furniture.favorite", b -> {
                    if (authenticated) {
                        FurnitureData model = FurnitureDataManager.getData(selected);
                        if (model != null) {
                            if (!b.isEnabled()) {
                                request(API.HttpMethod.DELETE, "like/furniture/" + model.contentid);
                            } else {
                                request(API.HttpMethod.POST, "like/furniture/" + model.contentid);
                            }
                            b.setEnabled(!b.isEnabled());
                        }
                    } else {
                        setError("gui.immersive_furniture.login_required");
                    }
                }).setEnabled(tab != Tab.FAVORITES);
            }

            // Publish
            if (selected.getNamespace().equals("local")) {
                addButton(x - 25 - 11, y, 22, 22 * 6, 48, "gui.immersive_furniture.publish", () -> {
                    if (authenticated) {
                        FurnitureData model = FurnitureDataManager.getData(selected);
                        if (model != null) {
                            publish(model);
                        }
                    } else {
                        setError("gui.immersive_furniture.login_required");
                    }
                });
            }

            // Delete
            if (tab == Tab.SUBMISSIONS || tab == Tab.LOCAL) {
                addButton(x - 11, y, 22, 22 * 2, 48, "gui.immersive_furniture.delete", this::delete);
            }

            // Modify
            addButton(x + 25 - 11, y, 22, 22, 48, "gui.immersive_furniture.modify", () -> {
                FurnitureData data = FurnitureDataManager.getData(selected);
                if (minecraft != null && data != null) {
                    minecraft.setScreen(new ArtisansWorkstationEditorScreen(new FurnitureData(data)));
                }
            });

            // Report
            if (tab == Tab.GLOBAL) {
                addToggleButton(x + 50 - 11, y, 22, 22 * 5, 48, "gui.immersive_furniture.report", b -> {
                    if (authenticated) {
                        FurnitureData model = FurnitureDataManager.getData(selected);
                        if (model != null) {
                            if (b.isEnabled()) {
                                request(API.HttpMethod.DELETE, "report/furniture/" + model.contentid + "/DEFAULT");
                            } else {
                                request(API.HttpMethod.POST, "report/furniture/" + model.contentid + "/DEFAULT");
                            }
                            b.setEnabled(!b.isEnabled());
                        }
                    } else {
                        setError("gui.immersive_furniture.login_required");
                    }
                }).setEnabled(false);
            }

            // Close
            addRenderableWidget(
                    Button.builder(Component.translatable("gui.immersive_furniture.back"), b -> {
                                selected = null;
                                init();
                            })
                            .bounds(leftPos + 4, topPos + windowHeight - 24, 64, 20)
                            .build()
            );

            // Craft
            addRenderableWidget(
                    Button.builder(Component.translatable("gui.immersive_furniture.craft"), b -> {
                                Network.sendToServer(new CraftRequest(FurnitureDataManager.getData(selected), holdingShift()));
                                Minecraft.getInstance().setScreen(null);
                            })
                            .bounds(leftPos + windowWidth - 68, topPos + windowHeight - 24, 64, 20)
                            .build()
            );
        }
    }

    private void delete() {
        if (lastCriticalActionAttempt + 5000 > System.currentTimeMillis()) {
            if (tab == Tab.SUBMISSIONS) {
                FurnitureData model = FurnitureDataManager.getData(selected);
                if (model != null) {
                    request(API.HttpMethod.DELETE, "content/furniture/" + model.contentid);
                } else {
                    setError("gui.immersive_furniture.delete_failed");
                }
            } else {
                FurnitureDataManager.deleteLocalFile(selected);
                localFiles = FurnitureDataManager.getLocalFiles();
                shouldSearch = true;
            }
            selected = null;
            init();
        } else {
            lastCriticalActionAttempt = System.currentTimeMillis();
            setError("gui.immersive_furniture.delete_confirm");
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);

        if (selected == null) {
            drawRectangle(graphics, leftPos, topPos, windowWidth, 38);
            drawRectangle(graphics, leftPos, topPos + windowHeight - 28, windowWidth, 28);
        } else {
            // Background
            drawRectangle(graphics, leftPos, topPos, windowWidth, windowHeight - 28, 0, 48);
            drawRectangle(graphics, leftPos, topPos + windowHeight - 28, windowWidth, 28);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        super.render(graphics, mouseX, mouseY, delta);

        List<Component> tooltip = null;

        if (selected == null) {
            search();

            int w = windowWidth / 4;
            int h = (windowHeight - 38 - 28) / 2;

            // Background
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 2; y++) {
                    int i = x + y * 4;
                    boolean hovered = isTileHovered(x, y) && i < furniture.size();
                    drawRectangle(graphics, leftPos + x * w, topPos + 38 + y * h, w, h, 0, hovered ? 96 : 48);

                    if (i < furniture.size()) {
                        FurnitureData data = FurnitureDataManager.getData(furniture.get(i));
                        if (data != null) {
                            float rot = (float) (hovered ? (System.currentTimeMillis() % 10000) / 10000.0f * Math.PI * 2.0f : -Math.PI / 4 * 3);
                            renderModel(graphics, data, leftPos + (x + 0.5) * w, topPos + 38 + (y + 0.5) * h, h, rot, (float) (-Math.PI / 4));

                            if (hovered) {
                                tooltip = data.getTooltip(Screen.hasShiftDown());
                                tooltip.add(0, Component.literal(data.name).withStyle(ChatFormatting.BOLD));
                            }
                        }
                    }
                }
            }

            graphics.drawCenteredString(font, String.valueOf(page + 1), leftPos + windowWidth / 2, topPos + windowHeight - 17, 0xFFFFFF);
        } else {
            FurnitureData data = FurnitureDataManager.getData(selected);

            if (data != null) {
                graphics.pose().translate(0, 0, 1024);
                graphics.enableScissor(leftPos + 6, topPos + 6, leftPos + windowWidth - 6, topPos + windowHeight - 28 - 6);
                renderModel(graphics, data, leftPos + windowWidth / 2.0, topPos + windowHeight / 2.0, windowHeight - 28, previewYaw, previewPitch);
                graphics.flush();
                graphics.disableScissor();

                // Title and author
                graphics.drawString(font, data.name, leftPos + 8, topPos + 8, 0xFFFFFF);
                graphics.drawString(font, Component.translatable("gui.immersive_furniture.author", data.author).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY), leftPos + 8, topPos + 18, 0xFFFFFF);

                // Icon and cost
                graphics.pose().pushPose();
                graphics.pose().translate(leftPos + windowWidth, topPos + windowHeight - 28, 0);
                graphics.pose().scale(1.5f, 1.5f, 1.5f);
                MutableComponent cost = Component.literal(String.valueOf(data.getCost()));
                graphics.drawString(font, cost, -font.width(cost) - 26, -17, 0xFFFFFF);
                graphics.renderFakeItem(Items.CRAFTING_MATERIAL.getDefaultInstance(), -22, -22);
                graphics.pose().popPose();

                // Material tooltip
                if (lastMouseX >= leftPos + windowWidth - 32 - 6 && lastMouseX < leftPos + windowWidth - 6 && lastMouseY >= topPos + windowHeight - 32 - 32 && lastMouseY < topPos + windowHeight - 32) {
                    tooltip = Items.CRAFTING_MATERIAL.getDefaultInstance().getTooltipLines(null, TooltipFlag.Default.NORMAL);
                }
            }
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);

        if (authenticating) {
            tickAuthentication();

            // Auth hint
            if (isBrowserOpen) {
                drawTextBox(graphics, Component.translatable("gui.immersive_furniture.authenticating_browser"));
            } else {
                drawTextBox(graphics, Component.translatable("gui.immersive_furniture.authenticating").append(Component.literal(" " + ".".repeat((int) (System.currentTimeMillis() / 500 % 4)))));
            }
        }

        if (tooltip != null) {
            graphics.renderTooltip(font, tooltip, Optional.empty(), lastMouseX, lastMouseY);
        }

        graphics.pose().popPose();

        renderError(graphics, selected == null ? height / 2 : topPos + 9);
    }

    private boolean isTileHovered(int x, int y) {
        int w = windowWidth / 4;
        int h = (windowHeight - 32 - 28) / 2;

        return lastMouseX >= leftPos + x * w && lastMouseX < leftPos + (x + 1) * w &&
               lastMouseY >= topPos + 38 + y * h && lastMouseY < topPos + 38 + (y + 1) * h;
    }

    private boolean holdingShift() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
    }

    protected ImageButton addButton(int x, int y, int size, int u, int v, String tooltip, Runnable clicked) {
        ImageButton button = addRenderableWidget(
                new ImageButton(x, y, size, size, u, v, size, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                        b -> clicked.run(),
                        tooltip == null ? Component.literal("") : Component.translatable(tooltip))
        );

        if (tooltip != null) {
            button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }

        return button;
    }

    protected StateImageButton addToggleButton(int x, int y, int size, int u, int v, String tooltip, Consumer<StateImageButton> clicked) {
        StateImageButton button = addRenderableWidget(
                new StateImageButton(x, y, size, size, u, v, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE,
                        b -> clicked.accept((StateImageButton) b),
                        tooltip == null ? Component.literal("") : Component.translatable(tooltip))
        );

        if (tooltip != null) {
            button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }

        return button;
    }

    private void tickAuthentication() {
        if (awaitingAuthentication) return;
        awaitingAuthentication = true;
        CompletableFuture.runAsync(() -> {
            try {
                Response response = Auth.hasToken() ? request(API.HttpMethod.GET, IsAuthResponse::new, "auth") : null;
                if (response instanceof IsAuthResponse authResponse) {
                    if (authResponse.authenticated()) {
                        authenticated = true;
                        authenticating = false;

                        clearError();
                        Minecraft.getInstance().execute(this::init);

                        // Token accepted, save
                        Auth.saveToken();
                    } else {
                        // Token rejected, delete file
                        Auth.clearToken();

                        // If the browser is open, the user is still authenticating
                        if (!isBrowserOpen) {
                            setError("gui.immersive_furniture.is_auth_failed");
                            authenticating = false;
                        }
                    }
                } else {
                    // Connection or server error
                    setError("gui.immersive_furniture.is_auth_failed");
                    authenticating = false;
                }
                Thread.sleep(2000);
            } catch (Exception e) {
                Common.logger.error("Failed to authenticate!", e);
            }
            awaitingAuthentication = false;
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selected == null) {
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 2; y++) {
                    int index = x + y * 4;
                    if (index < furniture.size() && isTileHovered(x, y)) {
                        setSelected(furniture.get(index));
                        init();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setSelected(ResourceLocation location) {
        this.selected = location;
        previewYaw = (float) (-Math.PI / 4 * 3);
        previewPitch = (float) (-Math.PI / 4);
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    private void drawTextBox(GuiGraphics graphics, Component text) {
        int y = height / 2 - 32;
        graphics.fill(width / 2 - 115, y - 5, width / 2 + 115, y + 12, 0x50000000);
        graphics.drawCenteredString(font, text, width / 2, y, 0xFFFFFFFF);
    }

    private String getPlayerName() {
        return Minecraft.getInstance().player == null ? "Unknown" : Minecraft.getInstance().player.getGameProfile().getName();
    }

    private void search() {
        if (awaitingSearch) return;
        if (!shouldSearch) return;
        shouldSearch = false;

        if (tab == Tab.LOCAL) {
            // Fetch from local files
            furniture = localFiles.stream()
                    .filter(l -> Utils.search(searchBox.getValue(), l.toString()))
                    .filter(l -> tagFilter.equals("miscellaneous") || getData(l) == null || getData(l).tag.equals(tagFilter))
                    .skip((long) page * ENTRIES_PER_PAGE)
                    .limit(ENTRIES_PER_PAGE)
                    .toList();
        } else {
            // Fetch from the library
            awaitingSearch = true;
            CompletableFuture.runAsync(() -> {
                Response response = request(API.HttpMethod.GET, ContentListResponse::new, "v2/content/furniture", Map.of(
                        "whitelist", searchBox.getValue() + (tagFilter.equals("miscellaneous") ? "" : "," + tagFilter),
                        "blacklist", "",
                        "order", sortByDate ? "date" : "likes",
                        "track", tab == Tab.FAVORITES ? "likes" : tab == Tab.SUBMISSIONS ? "submissions" : "all",
                        "descending", "true",
                        "offset", String.valueOf(page * ENTRIES_PER_PAGE),
                        "limit", String.valueOf(ENTRIES_PER_PAGE)
                ));

                if (response instanceof ContentListResponse contentListResponse) {
                    furniture = Arrays.stream(contentListResponse.contents())
                            .map(c -> new ResourceLocation("library", c.contentid() + "." + c.version()))
                            .collect(Collectors.toList());
                    Minecraft.getInstance().execute(this::init);
                } else {
                    setError("gui.immersive_furniture.list_fetch_failed");
                }
                awaitingSearch = false;
            });
        }
    }

    private void publish(FurnitureData data) {
        if (uploading) return;
        uploading = true;

        CompletableFuture.runAsync(() -> {
            if (!Auth.hasToken()) return;

            // Gather tags
            List<String> tags = new LinkedList<>();
            tags.add(data.tag);
            if (data.hasParticles()) tags.add("has_particles");
            if (data.hasSounds()) tags.add("has_sounds");
            if (data.canSit()) tags.add("can_sit");
            if (data.canSleep()) tags.add("can_sleep");
            if (!data.dependencies.isEmpty()) tags.add("has_dependencies");
            if (!data.sources.isEmpty()) tags.add("has_modded_textures");
            if (data.inventorySize > 0) tags.add("has_inventory");
            if (data.lightLevel > 0) tags.add("is_light_source");
            tags.addAll(data.elements.stream()
                    .filter(e -> e.type == FurnitureData.ElementType.ELEMENT)
                    .map(e -> e.material.source.getPath())
                    .distinct().toList());

            // Upload
            Response request = request(
                    data.contentid == -1 ? API.HttpMethod.POST : API.HttpMethod.PUT,
                    data.contentid == -1 ? ContentIdResponse::new : SuccessResponse::new,
                    data.contentid == -1 ? "content/furniture" : "content/furniture/" + data.contentid,
                    Map.of(

                    ), Map.of(
                            "title", data.name,
                            "meta", "{}",
                            "data", new String(Base64.getEncoder().encode(Utils.toBytes(data.toTag()))),
                            "tags", tags
                    ));

            if (request instanceof ContentIdResponse response) {
                data.contentid = response.contentid();
                FurnitureDataManager.saveLocalFile(data);
                selected = null;
                Minecraft.getInstance().execute(() -> {
                    setTab(Tab.SUBMISSIONS);
                    init();
                });
            } else if (request instanceof ErrorResponse response) {
                if (response.code() == 428) {
                    setError("gui.immersive_furniture.upload_duplicate");
                } else {
                    setError("gui.immersive_furniture.upload_failed");
                }
            }
            uploading = false;
        });
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (selected != null) {
            if (button == 0) {
                previewYaw += (float) (dragX * 0.015f);
                previewPitch -= (float) (dragY * 0.015f);
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
