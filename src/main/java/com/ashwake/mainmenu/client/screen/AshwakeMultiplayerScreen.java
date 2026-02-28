package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.render.AshwakeBackgroundRenderer;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.render.AshwakeUiSkin;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class AshwakeMultiplayerScreen extends JoinMultiplayerScreen {
    private static final int SIDE_MARGIN = 16;
    private static final int HEADER_HEIGHT = 68;
    private static final int FOOTER_HEIGHT = 56;
    private static final int PANEL_GAP = 12;
    private static final int PANEL_PADDING = 10;
    private static final int SIDEBAR_MIN_WIDTH = 210;
    private static final int SIDEBAR_MAX_WIDTH = 320;
    private static final int CONTENT_MIN_LIST_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_GAP = 8;
    private static final int HEADER_LINE_HEIGHT = 10;
    private static final int MIN_SIDEBAR_BUTTON_HEIGHT = 18;
    private static final long COPY_FEEDBACK_DURATION_MS = 1800L;
    private static final Component NO_SELECTION_TOOLTIP = Component.literal("Select a server first");
    private static final Component COPY_TOOLTIP = Component.translatable("menu.ashwake.multiplayer.copy");
    @Nullable
    private static final Field LAN_DETECTOR_FIELD = resolveLanDetectorField();

    private static final AshwakeBackgroundRenderer BACKGROUND_RENDERER = new AshwakeBackgroundRenderer();

    private Button vanillaSelectButton;
    private Button vanillaDirectButton;
    private Button vanillaAddButton;
    private Button vanillaEditButton;
    private Button vanillaDeleteButton;
    private Button vanillaRefreshButton;
    private Button vanillaBackButton;

    private AshwakeButton helpButton;
    private AshwakeButton backButton;
    private AshwakeButton joinButton;
    private AshwakeButton directButton;
    private AshwakeButton addButton;
    private AshwakeButton editButton;
    private AshwakeButton deleteButton;
    private AshwakeButton refreshButton;
    private AshwakeButton copyIpButton;

    private int shellX;
    private int shellY;
    private int shellW;
    private int shellH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int listPanelX;
    private int listPanelY;
    private int listPanelW;
    private int listPanelH;
    private int sidebarPanelX;
    private int sidebarPanelY;
    private int sidebarPanelW;
    private int sidebarPanelH;
    private int listInnerX;
    private int listInnerY;
    private int listInnerW;
    private int listInnerH;
    private int sidebarDetailsBottomY;
    private int sidebarButtonHeight = BUTTON_HEIGHT;
    private int sidebarButtonGap = BUTTON_GAP;
    private boolean stackedLayout;
    private boolean lanScanRunning = true;
    private long copiedFeedbackUntilMs;

    public AshwakeMultiplayerScreen(Screen lastScreen) {
        super(lastScreen);
    }

    @Override
    protected void init() {
        super.init();

        if (!(this.serverSelectionList instanceof AshwakeServerSelectionList)) {
            ServerSelectionList oldList = this.serverSelectionList;
            this.removeWidget(oldList);
            this.serverSelectionList = new AshwakeServerSelectionList(this, this, this.minecraft, this.width, this.height - 96, 32, 36);
            this.serverSelectionList.updateOnlineServers(this.getServers());
            this.addRenderableWidget(this.serverSelectionList);
            this.onSelectedChange();
        }

        captureAndRemoveVanillaButtons();
        createAshwakeButtons();
        relayout();
        updateLanScanState();
        syncProxyButtonStates();
    }

    @Override
    public void tick() {
        super.tick();
        updateLanScanState();
        syncProxyButtonStates();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        BACKGROUND_RENDERER.render(
                guiGraphics,
                this.width,
                this.height,
                partialTick,
                AshwakeClientConfig.animationsEnabled(),
                AshwakeClientConfig.animationIntensity(),
                AshwakeClientConfig.particlesEnabled(),
                AshwakeClientConfig.particleDensity(),
                AshwakeClientConfig.reducedMotion());
        guiGraphics.fill(0, 0, this.width, this.height, 0x30090706);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        drawShell(guiGraphics);
        drawPanels(guiGraphics);

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        drawHeaderText(guiGraphics);
        drawPanelHeadings(guiGraphics);
        drawListEmptyState(guiGraphics);
        drawSidebarDetails(guiGraphics);
    }

    private void captureAndRemoveVanillaButtons() {
        vanillaSelectButton = null;
        vanillaDirectButton = null;
        vanillaAddButton = null;
        vanillaEditButton = null;
        vanillaDeleteButton = null;
        vanillaRefreshButton = null;
        vanillaBackButton = null;

        List<Button> discovered = new ArrayList<>();
        for (Renderable renderable : this.renderables) {
            if (!(renderable instanceof Button button) || button instanceof AshwakeButton) {
                continue;
            }

            String key = translatableKey(button.getMessage());
            if (key == null) {
                continue;
            }

            switch (key) {
                case "selectServer.select" -> vanillaSelectButton = button;
                case "selectServer.direct" -> vanillaDirectButton = button;
                case "selectServer.add" -> vanillaAddButton = button;
                case "selectServer.edit" -> vanillaEditButton = button;
                case "selectServer.delete" -> vanillaDeleteButton = button;
                case "selectServer.refresh" -> vanillaRefreshButton = button;
                case "gui.back" -> vanillaBackButton = button;
                default -> {
                    continue;
                }
            }
            discovered.add(button);
        }

        for (Button button : discovered) {
            this.removeWidget(button);
        }
    }

    private void createAshwakeButtons() {
        helpButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                24,
                BUTTON_HEIGHT,
                Component.literal("?"),
                button -> this.minecraft.setScreen(new AshwakeGuidanceScreen(this))));

        backButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                100,
                BUTTON_HEIGHT,
                Component.translatable("gui.back"),
                AshwakeButton.Icon.QUIT,
                button -> pressVanilla(vanillaBackButton, this::onClose)));

        joinButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                110,
                BUTTON_HEIGHT,
                Component.translatable("selectServer.select"),
                AshwakeButton.Icon.PLAY,
                button -> pressVanilla(vanillaSelectButton, this::joinSelectedServer)));

        directButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                132,
                BUTTON_HEIGHT,
                Component.translatable("selectServer.direct"),
                AshwakeButton.Icon.CHANGELOG,
                button -> pressVanilla(vanillaDirectButton, null)));

        addButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                110,
                BUTTON_HEIGHT,
                Component.translatable("selectServer.add"),
                AshwakeButton.Icon.OPTIONS,
                button -> pressVanilla(vanillaAddButton, null)));

        editButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                100,
                BUTTON_HEIGHT,
                Component.translatable("selectServer.edit"),
                AshwakeButton.Icon.OPTIONS,
                button -> pressVanilla(vanillaEditButton, null)));

        deleteButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                100,
                BUTTON_HEIGHT,
                Component.translatable("selectServer.delete"),
                AshwakeButton.Icon.QUIT,
                button -> pressVanilla(vanillaDeleteButton, null)));

        refreshButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                100,
                BUTTON_HEIGHT,
                Component.translatable("selectServer.refresh"),
                AshwakeButton.Icon.CHANGELOG,
                button -> pressVanilla(vanillaRefreshButton, null)));

        copyIpButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                100,
                BUTTON_HEIGHT,
                Component.translatable("menu.ashwake.multiplayer.copy"),
                AshwakeButton.Icon.CHANGELOG,
                button -> copySelectedServerAddress()));
    }

    private void relayout() {
        shellX = Math.min(SIDE_MARGIN, Math.max(6, this.width / 22));
        shellY = 8;
        shellW = Math.max(160, this.width - (shellX * 2));
        shellH = Math.max(140, this.height - 16);
        if (shellX + shellW > this.width - 2) {
            shellX = 2;
            shellW = Math.max(120, this.width - 4);
        }
        if (shellY + shellH > this.height - 2) {
            shellY = 2;
            shellH = Math.max(120, this.height - 4);
        }

        contentX = shellX + 2;
        contentY = shellY + HEADER_HEIGHT;
        contentW = Math.max(120, shellW - 4);
        contentH = Math.max(80, shellY + shellH - FOOTER_HEIGHT - contentY);

        int panelTop = contentY + 6;
        int panelBottom = contentY + contentH - 6;
        int panelRegionH = Math.max(72, panelBottom - panelTop);
        stackedLayout = contentW < 760 || panelRegionH < 220;
        if (stackedLayout) {
            listPanelX = contentX + 6;
            listPanelY = panelTop;
            listPanelW = contentW - 12;
            int minListH = 70;
            int minSidebarH = 98;
            int desiredListH = (panelRegionH * 3) / 5;
            if (panelRegionH >= minListH + minSidebarH + PANEL_GAP) {
                listPanelH = Mth.clamp(desiredListH, minListH, panelRegionH - minSidebarH - PANEL_GAP);
            } else {
                listPanelH = Math.max(56, panelRegionH / 2 - (PANEL_GAP / 2));
            }

            sidebarPanelX = listPanelX;
            sidebarPanelY = listPanelY + listPanelH + PANEL_GAP;
            sidebarPanelW = listPanelW;
            sidebarPanelH = Math.max(56, panelBottom - sidebarPanelY);
        } else {
            int sidebarTarget = Mth.clamp(contentW / 3, SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH);
            sidebarPanelW = Math.min(sidebarTarget, contentW - CONTENT_MIN_LIST_WIDTH);
            sidebarPanelX = contentX + contentW - sidebarPanelW - 6;
            sidebarPanelY = panelTop;
            sidebarPanelH = panelRegionH;

            listPanelX = contentX + 6;
            listPanelY = sidebarPanelY;
            listPanelW = sidebarPanelX - listPanelX - PANEL_GAP;
            listPanelH = sidebarPanelH;
        }

        listInnerX = listPanelX + PANEL_PADDING;
        listInnerY = listPanelY + 24;
        listInnerW = Math.max(160, listPanelW - (PANEL_PADDING * 2));
        listInnerH = Math.max(42, listPanelH - 30);
        this.serverSelectionList.setRectangle(listInnerW, listInnerH, listInnerX, listInnerY);
        if (this.serverSelectionList instanceof AshwakeServerSelectionList ashwakeList) {
            ashwakeList.setRowWidth(Math.max(150, listInnerW - 8));
        }

        int headerButtonY = shellY + 18;
        setBounds(helpButton, shellX + shellW - helpButton.getWidth() - 12, headerButtonY, helpButton.getWidth(), BUTTON_HEIGHT);

        int footerY = shellY + shellH - FOOTER_HEIGHT + 16;
        layoutFooterButtons(footerY);
        layoutSidebarButtons();
    }

    private int placeFromRight(AshwakeButton button, int rightCursor, int y) {
        int x = rightCursor - button.getWidth();
        setBounds(button, x, y, button.getWidth(), BUTTON_HEIGHT);
        return x - BUTTON_GAP;
    }

    private void layoutFooterButtons(int footerY) {
        int backX = shellX + 10;
        int gapBetweenGroups = 12;
        int actionGap = BUTTON_GAP;
        int minActionWidth = 44;

        int preferredBackWidth = 100;
        int minBackWidth = 64;
        int actionsMinimumTotal = (minActionWidth * 3) + (actionGap * 2);
        int maxBackWidth = Math.max(minBackWidth, shellW - 20 - actionsMinimumTotal - gapBetweenGroups);
        int backWidth = Mth.clamp(preferredBackWidth, minBackWidth, maxBackWidth);
        setBounds(backButton, backX, footerY, backWidth, BUTTON_HEIGHT);

        int actionsLeft = backX + backWidth + gapBetweenGroups;
        int actionsRight = shellX + shellW - 10;
        int available = Math.max(3, actionsRight - actionsLeft);

        int joinW = 110;
        int directW = 132;
        int addW = 110;
        int total = joinW + directW + addW + (actionGap * 2);
        if (total > available) {
            int each = Math.max(minActionWidth, (available - actionGap * 2) / 3);
            joinW = each;
            directW = each;
            addW = each;
            int spare = available - (joinW + directW + addW + actionGap * 2);
            while (spare > 0) {
                directW++;
                spare--;
                if (spare > 0) {
                    joinW++;
                    spare--;
                }
                if (spare > 0) {
                    addW++;
                    spare--;
                }
            }
        }

        setBounds(joinButton, joinButton.getX(), joinButton.getY(), joinW, BUTTON_HEIGHT);
        setBounds(directButton, directButton.getX(), directButton.getY(), directW, BUTTON_HEIGHT);
        setBounds(addButton, addButton.getX(), addButton.getY(), addW, BUTTON_HEIGHT);

        int rightCursor = actionsRight;
        rightCursor = placeFromRight(addButton, rightCursor, footerY);
        rightCursor = placeFromRight(directButton, rightCursor, footerY);
        placeFromRight(joinButton, rightCursor, footerY);
    }

    private void layoutSidebarButtons() {
        int sidebarButtonW = Math.max(96, sidebarPanelW - (PANEL_PADDING * 2));
        int sidebarButtonX = sidebarPanelX + PANEL_PADDING;
        int buttonCount = 4;
        int availableInnerHeight = Math.max(72, sidebarPanelH - 46 - PANEL_PADDING);
        sidebarButtonHeight = BUTTON_HEIGHT;
        sidebarButtonGap = BUTTON_GAP;
        int stackHeight = (buttonCount * sidebarButtonHeight) + ((buttonCount - 1) * sidebarButtonGap);
        while (stackHeight > availableInnerHeight && sidebarButtonGap > 4) {
            sidebarButtonGap--;
            stackHeight = (buttonCount * sidebarButtonHeight) + ((buttonCount - 1) * sidebarButtonGap);
        }
        while (stackHeight > availableInnerHeight && sidebarButtonHeight > MIN_SIDEBAR_BUTTON_HEIGHT) {
            sidebarButtonHeight--;
            stackHeight = (buttonCount * sidebarButtonHeight) + ((buttonCount - 1) * sidebarButtonGap);
        }

        int minTop = sidebarPanelY + 50;
        int sidebarBottom = sidebarPanelY + sidebarPanelH - PANEL_PADDING;
        int sidebarButtonY = sidebarBottom - stackHeight;
        if (sidebarButtonY < minTop) {
            sidebarButtonY = minTop;
        }
        if (sidebarButtonY + stackHeight > sidebarBottom) {
            sidebarButtonY = sidebarBottom - stackHeight;
        }
        sidebarDetailsBottomY = sidebarButtonY - 8;

        setBounds(copyIpButton, sidebarButtonX, sidebarButtonY, sidebarButtonW, sidebarButtonHeight);
        setBounds(editButton, sidebarButtonX, sidebarButtonY + sidebarButtonHeight + sidebarButtonGap, sidebarButtonW, sidebarButtonHeight);
        setBounds(
                deleteButton,
                sidebarButtonX,
                sidebarButtonY + ((sidebarButtonHeight + sidebarButtonGap) * 2),
                sidebarButtonW,
                sidebarButtonHeight);
        setBounds(
                refreshButton,
                sidebarButtonX,
                sidebarButtonY + ((sidebarButtonHeight + sidebarButtonGap) * 3),
                sidebarButtonW,
                sidebarButtonHeight);
    }

    private void drawShell(GuiGraphics guiGraphics) {
        drawPanelFrame(guiGraphics, shellX, shellY, shellW, shellH, false);
        guiGraphics.fill(shellX + 1, shellY + 1, shellX + shellW - 1, shellY + HEADER_HEIGHT - 2, 0x4F151110);
        guiGraphics.fill(
                shellX + 1,
                shellY + shellH - FOOTER_HEIGHT,
                shellX + shellW - 1,
                shellY + shellH - 1,
                0x5A151110);
        guiGraphics.fill(shellX + 1, shellY + HEADER_HEIGHT - 1, shellX + shellW - 1, shellY + HEADER_HEIGHT, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(
                shellX + 1,
                shellY + shellH - FOOTER_HEIGHT,
                shellX + shellW - 1,
                shellY + shellH - FOOTER_HEIGHT + 1,
                AshwakePalette.BASALT_EDGE);
    }

    private void drawPanels(GuiGraphics guiGraphics) {
        boolean listFocused = this.serverSelectionList != null && this.serverSelectionList.isFocused();
        boolean sidebarFocused = (copyIpButton != null && copyIpButton.isFocused())
                || (editButton != null && editButton.isFocused())
                || (deleteButton != null && deleteButton.isFocused())
                || (refreshButton != null && refreshButton.isFocused());
        drawPanelFrame(guiGraphics, listPanelX, listPanelY, listPanelW, listPanelH, listFocused);
        drawPanelFrame(guiGraphics, sidebarPanelX, sidebarPanelY, sidebarPanelW, sidebarPanelH, sidebarFocused);
    }

    private void drawHeaderText(GuiGraphics guiGraphics) {
        int titleX = shellX + 14;
        int titleY = shellY + 14;
        guiGraphics.drawString(this.font, Component.translatable("multiplayer.title"), titleX, titleY, AshwakePalette.LAVA_YELLOW);
        Component status = lanScanRunning
                ? Component.literal(Component.translatable("lanServer.scanning").getString() + " " + LoadingDotsText.get(Util.getMillis()))
                : Component.translatable("menu.ashwake.multiplayer.scanComplete");
        guiGraphics.drawString(this.font, status, titleX, titleY + HEADER_LINE_HEIGHT + 4, AshwakePalette.BONE_WHITE);
    }

    private void drawPanelHeadings(GuiGraphics guiGraphics) {
        guiGraphics.drawString(
                this.font,
                Component.translatable("menu.ashwake.multiplayer.servers"),
                listPanelX + PANEL_PADDING,
                listPanelY + 8,
                AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                this.font,
                Component.translatable("menu.ashwake.multiplayer.details"),
                sidebarPanelX + PANEL_PADDING,
                sidebarPanelY + 8,
                AshwakePalette.LAVA_YELLOW);
    }

    private void drawListEmptyState(GuiGraphics guiGraphics) {
        if (countSavedServers() > 0 || countLanServers() > 0) {
            return;
        }

        Component empty = Component.translatable("menu.ashwake.multiplayer.empty");
        List<FormattedCharSequence> lines = this.font.split(empty, Math.max(90, listInnerW - 24));
        int textBlockHeight = lines.size() * 10;
        int y = listInnerY + Math.max(8, (listInnerH - textBlockHeight) / 2);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawCenteredString(this.font, line, listInnerX + (listInnerW / 2), y, AshwakePalette.MUTED_TEXT);
            y += 10;
        }
    }

    private void drawSidebarDetails(GuiGraphics guiGraphics) {
        int x = sidebarPanelX + PANEL_PADDING;
        int y = sidebarPanelY + 24;
        int width = sidebarPanelW - (PANEL_PADDING * 2);

        ServerSelectionList.Entry selected = this.serverSelectionList.getSelected();
        if (selected == null || selected instanceof ServerSelectionList.LANHeader) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("menu.ashwake.multiplayer.noSelection"),
                    x,
                    y,
                    AshwakePalette.BONE_WHITE);
            y += 12;
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("menu.ashwake.multiplayer.selectPrompt"),
                    x,
                    y,
                    AshwakePalette.MUTED_TEXT);
            return;
        }

        if (selected instanceof ServerSelectionList.OnlineServerEntry online) {
            ServerData data = online.getServerData();
            y = drawDetailLine(guiGraphics, x, y, width, Component.literal(data.name), AshwakePalette.BONE_WHITE, true, sidebarDetailsBottomY);
            y = drawDetailLine(
                    guiGraphics,
                    x,
                    y,
                    width,
                    Component.literal(maskAddress(data.ip)),
                    AshwakePalette.MUTED_TEXT,
                    false,
                    sidebarDetailsBottomY);
            Component pingLine = data.state() == ServerData.State.PINGING
                    ? Component.translatable("multiplayer.status.pinging")
                    : data.ping >= 0L
                            ? Component.literal(Component.translatable("menu.ashwake.multiplayer.ping").getString() + ": " + data.ping + " ms")
                            : Component.translatable("multiplayer.status.no_connection");
            y = drawDetailLine(guiGraphics, x, y, width, pingLine, AshwakePalette.BONE_WHITE, false, sidebarDetailsBottomY);
            y = drawDetailLine(
                    guiGraphics,
                    x,
                    y,
                    width,
                    Component.literal(Component.translatable("menu.ashwake.multiplayer.version").getString() + ": " + data.version.getString()),
                    AshwakePalette.MUTED_TEXT,
                    false,
                    sidebarDetailsBottomY);
            if (data.state() == ServerData.State.INCOMPATIBLE) {
                y = drawDetailLine(
                        guiGraphics,
                        x,
                        y,
                        width,
                        Component.translatable("multiplayer.status.incompatible"),
                        0xFFDF7462,
                        false,
                        sidebarDetailsBottomY);
            }
            if (y + 10 < sidebarDetailsBottomY) {
                y += 4;
            }

            List<FormattedCharSequence> lines = this.font.split(data.motd, Math.max(80, width));
            int maxMotdLines = Math.min(4, lines.size());
            for (int i = 0; i < maxMotdLines && y + 10 <= sidebarDetailsBottomY; i++) {
                guiGraphics.drawString(this.font, lines.get(i), x, y, AshwakePalette.BONE_WHITE);
                y += 10;
            }

            if (Util.getMillis() < copiedFeedbackUntilMs) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("menu.ashwake.multiplayer.copied"),
                        x,
                        Math.max(y + 2, copyIpButton.getY() - 11),
                        AshwakePalette.LAVA_YELLOW);
            }
            return;
        }

        if (selected instanceof ServerSelectionList.NetworkServerEntry network) {
            LanServer data = network.getServerData();
            y = drawDetailLine(
                    guiGraphics,
                    x,
                    y,
                    width,
                    Component.translatable("lanServer.title"),
                    AshwakePalette.BONE_WHITE,
                    true,
                    sidebarDetailsBottomY);
            y = drawDetailLine(
                    guiGraphics,
                    x,
                    y,
                    width,
                    Component.literal(data.getMotd()),
                    AshwakePalette.BONE_WHITE,
                    false,
                    sidebarDetailsBottomY);
            drawDetailLine(
                    guiGraphics,
                    x,
                    y,
                    width,
                    Component.literal(maskAddress(data.getAddress())),
                    AshwakePalette.MUTED_TEXT,
                    false,
                    sidebarDetailsBottomY);
            if (Util.getMillis() < copiedFeedbackUntilMs) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("menu.ashwake.multiplayer.copied"),
                        x,
                        Math.max(y + 2, copyIpButton.getY() - 11),
                        AshwakePalette.LAVA_YELLOW);
            }
        }
    }

    private int drawDetailLine(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            Component component,
            int color,
            boolean highlight,
            int maxY) {
        if (y + 10 > maxY) {
            return y;
        }
        List<FormattedCharSequence> lines = this.font.split(component, Math.max(80, width));
        int maxLines = highlight ? 2 : 1;
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            if (y + 10 > maxY) {
                break;
            }
            guiGraphics.drawString(this.font, lines.get(i), x, y, color);
            y += 10;
        }
        return y + 2;
    }

    private void syncProxyButtonStates() {
        boolean hasSelection = hasSelectableServerSelected();
        boolean canCopy = selectedServerAddress() != null;

        joinButton.active = vanillaSelectButton != null ? vanillaSelectButton.active : hasSelection;
        directButton.active = vanillaDirectButton == null || vanillaDirectButton.active;
        addButton.active = vanillaAddButton == null || vanillaAddButton.active;
        editButton.active = vanillaEditButton != null && vanillaEditButton.active;
        deleteButton.active = vanillaDeleteButton != null && vanillaDeleteButton.active;
        refreshButton.active = vanillaRefreshButton == null || vanillaRefreshButton.active;
        copyIpButton.active = canCopy;

        joinButton.setTooltip(joinButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
        editButton.setTooltip(editButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
        deleteButton.setTooltip(deleteButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
        copyIpButton.setTooltip(copyIpButton.active ? Tooltip.create(COPY_TOOLTIP) : Tooltip.create(NO_SELECTION_TOOLTIP));
    }

    private void copySelectedServerAddress() {
        String address = selectedServerAddress();
        if (address == null || this.minecraft == null) {
            return;
        }

        this.minecraft.keyboardHandler.setClipboard(address);
        copiedFeedbackUntilMs = Util.getMillis() + COPY_FEEDBACK_DURATION_MS;
    }

    private boolean hasSelectableServerSelected() {
        ServerSelectionList.Entry selected = this.serverSelectionList.getSelected();
        return selected != null && !(selected instanceof ServerSelectionList.LANHeader);
    }

    @Nullable
    private String selectedServerAddress() {
        ServerSelectionList.Entry selected = this.serverSelectionList.getSelected();
        if (selected instanceof ServerSelectionList.OnlineServerEntry online) {
            return online.getServerData().ip;
        }
        if (selected instanceof ServerSelectionList.NetworkServerEntry network) {
            return network.getServerData().getAddress();
        }
        return null;
    }

    private int countSavedServers() {
        int count = 0;
        for (ServerSelectionList.Entry entry : this.serverSelectionList.children()) {
            if (entry instanceof ServerSelectionList.OnlineServerEntry) {
                count++;
            }
        }
        return count;
    }

    private int countLanServers() {
        int count = 0;
        for (ServerSelectionList.Entry entry : this.serverSelectionList.children()) {
            if (entry instanceof ServerSelectionList.NetworkServerEntry) {
                count++;
            }
        }
        return count;
    }

    private void updateLanScanState() {
        if (LAN_DETECTOR_FIELD == null) {
            lanScanRunning = true;
            return;
        }
        try {
            Object detector = LAN_DETECTOR_FIELD.get(this);
            lanScanRunning = detector instanceof Thread thread && thread.isAlive();
        } catch (IllegalAccessException exception) {
            lanScanRunning = true;
        }
    }

    private static String maskAddress(@Nullable String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        int colonIndex = address.lastIndexOf(':');
        String host = colonIndex > 0 ? address.substring(0, colonIndex) : address;
        String port = colonIndex > 0 ? address.substring(colonIndex) : "";
        if (host.length() <= 4) {
            return "***" + port;
        }

        int left = Math.min(3, host.length() / 2);
        int right = Math.min(2, host.length() - left);
        int hidden = Math.max(1, host.length() - left - right);
        return host.substring(0, left) + "*".repeat(hidden) + host.substring(host.length() - right) + port;
    }

    private static void setBounds(Button button, int x, int y, int width, int height) {
        button.setX(x);
        button.setY(y);
        button.setWidth(width);
        button.setHeight(height);
    }

    private static void pressVanilla(@Nullable Button vanillaButton, @Nullable Runnable fallback) {
        if (vanillaButton != null) {
            vanillaButton.onPress();
            return;
        }
        if (fallback != null) {
            fallback.run();
        }
    }

    @Nullable
    private static String translatableKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }

    @Nullable
    private static Field resolveLanDetectorField() {
        try {
            Field field = JoinMultiplayerScreen.class.getDeclaredField("lanServerDetector");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static void drawPanelFrame(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean focused) {
        if (w <= 0 || h <= 0) {
            return;
        }

        if (AshwakeUiSkin.hasTexture(AshwakeUiSkin.PANEL_FRAME)) {
            AshwakeUiSkin.drawNineSlice(guiGraphics, AshwakeUiSkin.PANEL_FRAME, x, y, w, h);
            int fillAlpha = focused ? 0xB8 : 0xA8;
            guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, (fillAlpha << 24) | 0x0E0B0A);
            int outerBorder = focused ? 0xA8171210 : 0x99151110;
            int innerHighlight = focused ? 0x38F2D5AE : 0x2EF2D5AE;
            guiGraphics.fill(x, y, x + w, y + 1, outerBorder);
            guiGraphics.fill(x, y + h - 1, x + w, y + h, outerBorder);
            guiGraphics.fill(x, y, x + 1, y + h, outerBorder);
            guiGraphics.fill(x + w - 1, y, x + w, y + h, outerBorder);
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, innerHighlight);
            guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 1, innerHighlight);
            if (focused) {
                AshwakeUiSkin.drawFocusGlow(guiGraphics, x, y, w, h);
            }
            return;
        }

        int top = focused ? 0xBF1A1513 : 0xAD14110F;
        int bottom = focused ? 0xB6120F0D : 0xA80E0B0A;
        guiGraphics.fillGradient(x, y, x + w, y + h, top, bottom);

        int borderOuter = focused ? 0xAA2B231E : 0x99171210;
        int borderInner = focused ? 0x38F2D5AE : 0x2EF2D5AE;

        guiGraphics.fill(x, y, x + w, y + 1, borderOuter);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, borderOuter);
        guiGraphics.fill(x, y, x + 1, y + h, borderOuter);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, borderOuter);

        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, borderInner);
        guiGraphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, borderInner);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 1, borderInner);
        guiGraphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, borderInner);

        if (focused) {
            AshwakeUiSkin.drawFocusGlow(guiGraphics, x, y, w, h);
        }
    }

    private static final class AshwakeServerSelectionList extends ServerSelectionList {
        private final AshwakeMultiplayerScreen owner;
        private int rowWidth = 340;

        private AshwakeServerSelectionList(
                AshwakeMultiplayerScreen owner,
                JoinMultiplayerScreen screen,
                net.minecraft.client.Minecraft minecraft,
                int width,
                int height,
                int y,
                int itemHeight) {
            super(screen, minecraft, width, height, y, itemHeight);
            this.owner = owner;
        }

        void setRowWidth(int rowWidth) {
            this.rowWidth = rowWidth;
        }

        @Override
        public int getRowWidth() {
            return rowWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {
        }

        @Override
        protected void renderItem(
                GuiGraphics guiGraphics,
                int mouseX,
                int mouseY,
                float partialTick,
                int index,
                int left,
                int top,
                int width,
                int height) {
            ServerSelectionList.Entry entry = this.getEntry(index);
            boolean hovered = Objects.equals(this.getHovered(), entry);
            boolean selected = this.isSelectedItem(index);
            if (entry instanceof ServerSelectionList.LANHeader) {
                renderLanSection(guiGraphics, left, top, width, height);
                return;
            }

            int lift = hovered && !selected ? 1 : 0;
            int rowTop = top - lift;
            int rowBottom = rowTop + height;
            int rowLeft = left - 2;
            int rowRight = left + width + 2;

            int topColor = selected ? 0xB4191412 : hovered ? 0xA0171311 : 0x8813100E;
            int bottomColor = selected ? 0xAB140F0D : hovered ? 0x95140F0D : 0x80100D0B;
            guiGraphics.fillGradient(rowLeft, rowTop, rowRight, rowBottom, topColor, bottomColor);

            int border = selected ? 0xB6F2D5AE : hovered ? 0x8AF2D5AE : 0x544C433A;
            guiGraphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, border);
            guiGraphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, border);
            guiGraphics.fill(rowLeft, rowTop, rowLeft + 1, rowBottom, border);
            guiGraphics.fill(rowRight - 1, rowTop, rowRight, rowBottom, border);
            if (selected) {
                guiGraphics.fill(rowLeft + 1, rowTop + 1, rowRight - 1, rowBottom - 1, 0x241D1815);
            }

            entry.renderBack(guiGraphics, index, rowTop, left, width, height, mouseX, mouseY, hovered, partialTick);
            entry.render(guiGraphics, index, rowTop, left, width, height, mouseX, mouseY, hovered, partialTick);
        }

        private void renderLanSection(GuiGraphics guiGraphics, int left, int top, int width, int height) {
            int rowLeft = left - 2;
            int rowRight = left + width + 2;
            int centerY = top + (height / 2);
            guiGraphics.fill(rowLeft, centerY, rowRight, centerY + 1, 0x57F2D5AE);

            Component label = Component.translatable("menu.ashwake.multiplayer.lan");
            Component status = owner.lanScanRunning
                    ? Component.literal(LoadingDotsText.get(Util.getMillis()))
                    : Component.translatable("menu.ashwake.multiplayer.scanCompleteShort");
            guiGraphics.drawString(this.minecraft.font, label, rowLeft + 5, top + 5, AshwakePalette.LAVA_YELLOW, false);
            int statusX = rowRight - this.minecraft.font.width(status) - 6;
            guiGraphics.drawString(this.minecraft.font, status, statusX, top + 5, AshwakePalette.MUTED_TEXT, false);
        }

        @Override
        protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
            int left = this.getX() + (this.width - width) / 2;
            int right = left + width;
            guiGraphics.fill(left, top - 2, right, top + height + 2, 0xB0F2D5AE);
            guiGraphics.fill(left + 1, top - 1, right - 1, top + height + 1, 0x7D1D1814);
        }
    }
}
