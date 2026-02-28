package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.render.AshwakeBackgroundRenderer;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.render.AshwakeUiSkin;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelSummary;

public final class AshwakeSelectWorldScreen extends SelectWorldScreen {
    private static final int SIDE_MARGIN = 16;
    private static final int HEADER_HEIGHT = 68;
    private static final int FOOTER_HEIGHT = 56;
    private static final int PANEL_GAP = 12;
    private static final int PANEL_PADDING = 10;
    private static final int SIDEBAR_MIN_WIDTH = 220;
    private static final int SIDEBAR_MAX_WIDTH = 340;
    private static final int CONTENT_MIN_LIST_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_GAP = 8;
    private static final int MIN_SIDEBAR_BUTTON_HEIGHT = 18;
    private static final Component NO_SELECTION_TOOLTIP = Component.translatable("menu.ashwake.selectworld.selectFirst");

    private static final AshwakeBackgroundRenderer BACKGROUND_RENDERER = new AshwakeBackgroundRenderer();

    @Nullable
    private static final Field WORLD_LIST_FIELD = resolveField(SelectWorldScreen.class, "list");
    @Nullable
    private static final Field WORLD_ENTRY_SUMMARY_FIELD = resolveWorldEntrySummaryField();

    private WorldSelectionList worldList;

    private Button vanillaPlayButton;
    private Button vanillaCreateButton;
    private Button vanillaEditButton;
    private Button vanillaDeleteButton;
    private Button vanillaRecreateButton;
    private Button vanillaBackButton;

    private AshwakeButton helpButton;
    private AshwakeButton backButton;
    private AshwakeButton playButton;
    private AshwakeButton createButton;
    private AshwakeButton editButton;
    private AshwakeButton deleteButton;
    private AshwakeButton recreateButton;

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

    public AshwakeSelectWorldScreen(Screen lastScreen) {
        super(lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        captureWorldList();
        replaceWorldListIfNeeded();
        removeDuplicateWorldLists();

        captureAndRemoveVanillaButtons();
        createAshwakeButtons();
        configureSearchBox();
        relayout();
        syncProxyButtonStates();
    }

    @Override
    public void tick() {
        super.tick();
        syncProxyButtonStates();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.searchBox != null && this.searchBox.isFocused()) {
            if (!this.searchBox.getValue().isEmpty()) {
                this.searchBox.setValue("");
                if (this.worldList != null) {
                    this.worldList.updateFilter("");
                }
                return true;
            }

            this.searchBox.setFocused(false);
            if (this.worldList != null) {
                this.setFocused(this.worldList);
            }
            return true;
        }

        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (this.worldList != null && this.worldList.getSelectedOpt().isPresent() && CommonInputs.selected(keyCode)) {
            pressVanilla(this.vanillaPlayButton, () -> this.worldList.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::joinWorld));
            return true;
        }

        return false;
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
                false,
                0,
                AshwakeClientConfig.reducedMotion());
        guiGraphics.fill(0, 0, this.width, this.height, 0x30090706);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        drawShell(guiGraphics);
        drawPanels(guiGraphics);

        for (Renderable renderable : this.renderables) {
            if (renderable instanceof WorldSelectionList list && list != this.worldList) {
                continue;
            }
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.searchBox != null) {
            this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        drawHeaderText(guiGraphics);
        drawPanelHeadings(guiGraphics);
        drawListEmptyState(guiGraphics);
        drawSidebarDetails(guiGraphics);
    }

    private void captureWorldList() {
        this.worldList = getWorldListFromField();
        if (this.worldList != null) {
            return;
        }

        for (Renderable renderable : this.renderables) {
            if (renderable instanceof WorldSelectionList list) {
                this.worldList = list;
                setWorldListField(list);
                return;
            }
        }
    }

    private void replaceWorldListIfNeeded() {
        if (this.worldList == null || this.worldList instanceof AshwakeWorldSelectionList) {
            return;
        }

        WorldSelectionList oldList = this.worldList;
        this.removeWidget(oldList);

        String filter = this.searchBox != null ? this.searchBox.getValue() : "";
        this.worldList = new AshwakeWorldSelectionList(
                this,
                this,
                this.minecraft,
                this.width,
                this.height - 112,
                48,
                36,
                filter,
                oldList);
        this.addRenderableWidget(this.worldList);
        setWorldListField(this.worldList);

        if (this.searchBox != null) {
            this.searchBox.setResponder(this.worldList::updateFilter);
        }
    }

    private void removeDuplicateWorldLists() {
        if (this.worldList == null) {
            return;
        }

        List<WorldSelectionList> duplicates = new ArrayList<>();
        for (Renderable renderable : this.renderables) {
            if (renderable instanceof WorldSelectionList list && list != this.worldList) {
                duplicates.add(list);
            }
        }

        for (WorldSelectionList duplicate : duplicates) {
            this.removeWidget(duplicate);
        }
    }

    private void captureAndRemoveVanillaButtons() {
        this.vanillaPlayButton = null;
        this.vanillaCreateButton = null;
        this.vanillaEditButton = null;
        this.vanillaDeleteButton = null;
        this.vanillaRecreateButton = null;
        this.vanillaBackButton = null;

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
                case "selectWorld.select" -> this.vanillaPlayButton = button;
                case "selectWorld.create" -> this.vanillaCreateButton = button;
                case "selectWorld.edit" -> this.vanillaEditButton = button;
                case "selectWorld.delete" -> this.vanillaDeleteButton = button;
                case "selectWorld.recreate" -> this.vanillaRecreateButton = button;
                case "gui.back" -> this.vanillaBackButton = button;
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
        this.helpButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                24,
                BUTTON_HEIGHT,
                Component.literal("?"),
                button -> this.minecraft.setScreen(new AshwakeGuidanceScreen(this))));

        this.backButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                100,
                BUTTON_HEIGHT,
                Component.translatable("gui.back"),
                AshwakeButton.Icon.QUIT,
                button -> pressVanilla(this.vanillaBackButton, this::onClose)));

        this.playButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                126,
                BUTTON_HEIGHT,
                Component.translatable("selectWorld.select"),
                AshwakeButton.Icon.PLAY,
                button -> pressVanilla(
                        this.vanillaPlayButton,
                        () -> this.worldList.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::joinWorld))));

        this.createButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                150,
                BUTTON_HEIGHT,
                Component.translatable("selectWorld.create"),
                AshwakeButton.Icon.OPTIONS,
                button -> pressVanilla(this.vanillaCreateButton, () -> CreateWorldScreen.openFresh(this.minecraft, this))));

        this.editButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                120,
                BUTTON_HEIGHT,
                Component.translatable("selectWorld.edit"),
                AshwakeButton.Icon.OPTIONS,
                button -> pressVanilla(
                        this.vanillaEditButton,
                        () -> this.worldList.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::editWorld))));

        this.deleteButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                120,
                BUTTON_HEIGHT,
                Component.translatable("selectWorld.delete"),
                AshwakeButton.Icon.QUIT,
                button -> pressVanilla(
                        this.vanillaDeleteButton,
                        () -> this.worldList.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::deleteWorld))));

        this.recreateButton = this.addRenderableWidget(new AshwakeButton(
                0,
                0,
                120,
                BUTTON_HEIGHT,
                Component.translatable("selectWorld.recreate"),
                AshwakeButton.Icon.CHANGELOG,
                button -> pressVanilla(
                        this.vanillaRecreateButton,
                        () -> this.worldList.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::recreateWorld))));
    }

    private void configureSearchBox() {
        if (this.searchBox == null) {
            return;
        }

        this.searchBox.setMaxLength(128);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setCanLoseFocus(true);
        if (this.worldList != null) {
            this.searchBox.setResponder(this.worldList::updateFilter);
        }
    }

    private void relayout() {
        this.shellX = Math.min(SIDE_MARGIN, Math.max(6, this.width / 22));
        this.shellY = 8;
        this.shellW = Math.max(160, this.width - (this.shellX * 2));
        this.shellH = Math.max(140, this.height - 16);
        if (this.shellX + this.shellW > this.width - 2) {
            this.shellX = 2;
            this.shellW = Math.max(120, this.width - 4);
        }
        if (this.shellY + this.shellH > this.height - 2) {
            this.shellY = 2;
            this.shellH = Math.max(120, this.height - 4);
        }

        this.contentX = this.shellX + 2;
        this.contentY = this.shellY + HEADER_HEIGHT;
        this.contentW = Math.max(120, this.shellW - 4);
        this.contentH = Math.max(80, this.shellY + this.shellH - FOOTER_HEIGHT - this.contentY);

        int panelTop = this.contentY + 6;
        int panelBottom = this.contentY + this.contentH - 6;
        int panelRegionH = Math.max(72, panelBottom - panelTop);
        boolean stackedLayout = this.contentW < 760 || panelRegionH < 220;

        if (stackedLayout) {
            this.listPanelX = this.contentX + 6;
            this.listPanelY = panelTop;
            this.listPanelW = this.contentW - 12;

            int minListH = 70;
            int minSidebarH = 98;
            int desiredListH = (panelRegionH * 3) / 5;
            if (panelRegionH >= minListH + minSidebarH + PANEL_GAP) {
                this.listPanelH = Mth.clamp(desiredListH, minListH, panelRegionH - minSidebarH - PANEL_GAP);
            } else {
                this.listPanelH = Math.max(56, panelRegionH / 2 - (PANEL_GAP / 2));
            }

            this.sidebarPanelX = this.listPanelX;
            this.sidebarPanelY = this.listPanelY + this.listPanelH + PANEL_GAP;
            this.sidebarPanelW = this.listPanelW;
            this.sidebarPanelH = Math.max(56, panelBottom - this.sidebarPanelY);
        } else {
            int sidebarTarget = Mth.clamp(this.contentW / 3, SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH);
            this.sidebarPanelW = Math.min(sidebarTarget, this.contentW - CONTENT_MIN_LIST_WIDTH);
            this.sidebarPanelX = this.contentX + this.contentW - this.sidebarPanelW - 6;
            this.sidebarPanelY = panelTop;
            this.sidebarPanelH = panelRegionH;

            this.listPanelX = this.contentX + 6;
            this.listPanelY = this.sidebarPanelY;
            this.listPanelW = this.sidebarPanelX - this.listPanelX - PANEL_GAP;
            this.listPanelH = this.sidebarPanelH;
        }

        this.listInnerX = this.listPanelX + PANEL_PADDING;
        this.listInnerY = this.listPanelY + 24;
        this.listInnerW = Math.max(160, this.listPanelW - (PANEL_PADDING * 2));
        this.listInnerH = Math.max(42, this.listPanelH - 30);
        if (this.worldList != null) {
            this.worldList.setRectangle(this.listInnerW, this.listInnerH, this.listInnerX, this.listInnerY);
            if (this.worldList instanceof AshwakeWorldSelectionList ashwakeList) {
                ashwakeList.setRowWidth(Math.max(150, this.listInnerW - 8));
            }
        }

        int headerButtonY = this.shellY + 18;
        setBounds(this.helpButton, this.shellX + this.shellW - this.helpButton.getWidth() - 12, headerButtonY, this.helpButton.getWidth(), BUTTON_HEIGHT);
        layoutSearchBox();

        int footerY = this.shellY + this.shellH - FOOTER_HEIGHT + 16;
        layoutFooterButtons(footerY);
        layoutSidebarButtons();
    }

    private void layoutSearchBox() {
        if (this.searchBox == null) {
            return;
        }

        int searchY = this.shellY + 20;
        int searchRight = this.helpButton.getX() - 8;
        int minSearchX = this.shellX + 180;
        int searchW = Mth.clamp(this.shellW / 4, 96, 220);

        if (searchRight - minSearchX < searchW) {
            searchW = Math.max(72, searchRight - minSearchX);
        }

        if (searchW < 72) {
            searchW = 72;
        }

        int searchX = searchRight - searchW;
        if (searchX < this.shellX + 8) {
            searchX = this.shellX + 8;
            searchW = Math.max(72, searchRight - searchX);
        }

        this.searchBox.setPosition(searchX, searchY);
        this.searchBox.setWidth(searchW);
        this.searchBox.setHeight(20);
    }

    private void layoutFooterButtons(int footerY) {
        int backX = this.shellX + 10;
        int gapBetweenGroups = 12;
        int actionGap = BUTTON_GAP;
        int minActionWidth = 58;

        int preferredBackWidth = 100;
        int minBackWidth = 64;
        int actionsMinimumTotal = (minActionWidth * 2) + actionGap;
        int maxBackWidth = Math.max(minBackWidth, this.shellW - 20 - actionsMinimumTotal - gapBetweenGroups);
        int backWidth = Mth.clamp(preferredBackWidth, minBackWidth, maxBackWidth);
        setBounds(this.backButton, backX, footerY, backWidth, BUTTON_HEIGHT);

        int actionsLeft = backX + backWidth + gapBetweenGroups;
        int actionsRight = this.shellX + this.shellW - 10;
        int available = Math.max(2, actionsRight - actionsLeft);

        int createW = 150;
        int playW = 126;
        int total = createW + playW + actionGap;
        if (total > available) {
            int each = Math.max(minActionWidth, (available - actionGap) / 2);
            playW = each;
            createW = each;
            int spare = available - (playW + createW + actionGap);
            if (spare > 0) {
                createW += spare;
            }
        }

        setBounds(this.playButton, this.playButton.getX(), this.playButton.getY(), playW, BUTTON_HEIGHT);
        setBounds(this.createButton, this.createButton.getX(), this.createButton.getY(), createW, BUTTON_HEIGHT);

        int rightCursor = actionsRight;
        rightCursor = placeFromRight(this.createButton, rightCursor, footerY);
        placeFromRight(this.playButton, rightCursor, footerY);
    }

    private int placeFromRight(AshwakeButton button, int rightCursor, int y) {
        int x = rightCursor - button.getWidth();
        setBounds(button, x, y, button.getWidth(), BUTTON_HEIGHT);
        return x - BUTTON_GAP;
    }

    private void layoutSidebarButtons() {
        int sidebarButtonW = Math.max(96, this.sidebarPanelW - (PANEL_PADDING * 2));
        int sidebarButtonX = this.sidebarPanelX + PANEL_PADDING;
        int buttonCount = 3;
        int availableInnerHeight = Math.max(68, this.sidebarPanelH - 46 - PANEL_PADDING);
        this.sidebarButtonHeight = BUTTON_HEIGHT;
        this.sidebarButtonGap = BUTTON_GAP;
        int stackHeight = (buttonCount * this.sidebarButtonHeight) + ((buttonCount - 1) * this.sidebarButtonGap);

        while (stackHeight > availableInnerHeight && this.sidebarButtonGap > 4) {
            this.sidebarButtonGap--;
            stackHeight = (buttonCount * this.sidebarButtonHeight) + ((buttonCount - 1) * this.sidebarButtonGap);
        }

        while (stackHeight > availableInnerHeight && this.sidebarButtonHeight > MIN_SIDEBAR_BUTTON_HEIGHT) {
            this.sidebarButtonHeight--;
            stackHeight = (buttonCount * this.sidebarButtonHeight) + ((buttonCount - 1) * this.sidebarButtonGap);
        }

        int minTop = this.sidebarPanelY + 52;
        int sidebarBottom = this.sidebarPanelY + this.sidebarPanelH - PANEL_PADDING;
        int sidebarButtonY = sidebarBottom - stackHeight;
        if (sidebarButtonY < minTop) {
            sidebarButtonY = minTop;
        }
        if (sidebarButtonY + stackHeight > sidebarBottom) {
            sidebarButtonY = sidebarBottom - stackHeight;
        }

        this.sidebarDetailsBottomY = sidebarButtonY - 8;

        setBounds(this.editButton, sidebarButtonX, sidebarButtonY, sidebarButtonW, this.sidebarButtonHeight);
        setBounds(
                this.recreateButton,
                sidebarButtonX,
                sidebarButtonY + this.sidebarButtonHeight + this.sidebarButtonGap,
                sidebarButtonW,
                this.sidebarButtonHeight);
        setBounds(
                this.deleteButton,
                sidebarButtonX,
                sidebarButtonY + ((this.sidebarButtonHeight + this.sidebarButtonGap) * 2),
                sidebarButtonW,
                this.sidebarButtonHeight);
    }
    private void drawShell(GuiGraphics guiGraphics) {
        drawPanelFrame(guiGraphics, this.shellX, this.shellY, this.shellW, this.shellH, false);
        guiGraphics.fill(this.shellX + 1, this.shellY + 1, this.shellX + this.shellW - 1, this.shellY + HEADER_HEIGHT - 2, 0x4F151110);
        guiGraphics.fill(
                this.shellX + 1,
                this.shellY + this.shellH - FOOTER_HEIGHT,
                this.shellX + this.shellW - 1,
                this.shellY + this.shellH - 1,
                0x5A151110);
        guiGraphics.fill(
                this.shellX + 1,
                this.shellY + HEADER_HEIGHT - 1,
                this.shellX + this.shellW - 1,
                this.shellY + HEADER_HEIGHT,
                AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(
                this.shellX + 1,
                this.shellY + this.shellH - FOOTER_HEIGHT,
                this.shellX + this.shellW - 1,
                this.shellY + this.shellH - FOOTER_HEIGHT + 1,
                AshwakePalette.BASALT_EDGE);
    }

    private void drawPanels(GuiGraphics guiGraphics) {
        boolean listFocused = this.worldList != null && this.worldList.isFocused();
        boolean sidebarFocused = (this.editButton != null && this.editButton.isFocused())
                || (this.recreateButton != null && this.recreateButton.isFocused())
                || (this.deleteButton != null && this.deleteButton.isFocused());
        drawPanelFrame(guiGraphics, this.listPanelX, this.listPanelY, this.listPanelW, this.listPanelH, listFocused);
        drawPanelFrame(guiGraphics, this.sidebarPanelX, this.sidebarPanelY, this.sidebarPanelW, this.sidebarPanelH, sidebarFocused);
    }

    private void drawHeaderText(GuiGraphics guiGraphics) {
        int titleX = this.shellX + 14;
        int titleY = this.shellY + 14;
        guiGraphics.drawString(this.font, Component.translatable("selectWorld.title"), titleX, titleY, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                this.font,
                Component.translatable("menu.ashwake.selectworld.subtitle"),
                titleX,
                titleY + 14,
                AshwakePalette.BONE_WHITE);
    }

    private void drawPanelHeadings(GuiGraphics guiGraphics) {
        guiGraphics.drawString(
                this.font,
                Component.translatable("menu.ashwake.selectworld.worlds"),
                this.listPanelX + PANEL_PADDING,
                this.listPanelY + 8,
                AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                this.font,
                Component.translatable("menu.ashwake.selectworld.details"),
                this.sidebarPanelX + PANEL_PADDING,
                this.sidebarPanelY + 8,
                AshwakePalette.LAVA_YELLOW);
    }

    private void drawListEmptyState(GuiGraphics guiGraphics) {
        if (this.worldList == null || hasWorldEntries() || isWorldListLoading()) {
            return;
        }

        Component empty = Component.translatable("menu.ashwake.selectworld.empty");
        List<FormattedCharSequence> lines = this.font.split(empty, Math.max(90, this.listInnerW - 24));
        int textBlockHeight = lines.size() * 10;
        int y = this.listInnerY + Math.max(8, (this.listInnerH - textBlockHeight) / 2);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawCenteredString(this.font, line, this.listInnerX + (this.listInnerW / 2), y, AshwakePalette.MUTED_TEXT);
            y += 10;
        }
    }

    private void drawSidebarDetails(GuiGraphics guiGraphics) {
        int x = this.sidebarPanelX + PANEL_PADDING;
        int y = this.sidebarPanelY + 24;
        int width = this.sidebarPanelW - (PANEL_PADDING * 2);

        LevelSummary summary = getSelectedSummary();
        if (summary == null) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("menu.ashwake.selectworld.noSelection"),
                    x,
                    y,
                    AshwakePalette.BONE_WHITE);
            y += 12;
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("menu.ashwake.selectworld.selectPrompt"),
                    x,
                    y,
                    AshwakePalette.MUTED_TEXT);
            return;
        }

        y = drawDetailLine(guiGraphics, x, y, width, Component.literal(summary.getLevelName()), AshwakePalette.BONE_WHITE, true, this.sidebarDetailsBottomY);

        Component lastPlayed = summary.getLastPlayed() > 0
                ? Component.literal(
                        Component.translatable("menu.ashwake.selectworld.lastPlayed").getString()
                                + ": "
                                + WorldSelectionList.DATE_FORMAT.format(Instant.ofEpochMilli(summary.getLastPlayed())))
                : Component.translatable("menu.ashwake.selectworld.lastPlayedUnknown");
        y = drawDetailLine(guiGraphics, x, y, width, lastPlayed, AshwakePalette.MUTED_TEXT, false, this.sidebarDetailsBottomY);

        String modeText = summary.isHardcore()
                ? Component.translatable("gameMode.hardcore").getString()
                : Component.translatable("gameMode." + summary.getGameMode().getName()).getString();
        Component modeLine = Component.literal(Component.translatable("menu.ashwake.selectworld.mode").getString() + ": " + modeText);
        y = drawDetailLine(guiGraphics, x, y, width, modeLine, AshwakePalette.BONE_WHITE, false, this.sidebarDetailsBottomY);

        String commandsState = summary.hasCommands()
                ? Component.translatable("options.on").getString()
                : Component.translatable("options.off").getString();
        Component commandsLine = Component.literal(
                Component.translatable("menu.ashwake.selectworld.commands").getString() + ": " + commandsState);
        y = drawDetailLine(guiGraphics, x, y, width, commandsLine, AshwakePalette.MUTED_TEXT, false, this.sidebarDetailsBottomY);

        Component versionLine = Component.literal(
                Component.translatable("menu.ashwake.selectworld.version").getString()
                        + ": "
                        + summary.getWorldVersionName().getString());
        y = drawDetailLine(guiGraphics, x, y, width, versionLine, AshwakePalette.MUTED_TEXT, false, this.sidebarDetailsBottomY);

        if (summary.isExperimental()) {
            drawDetailLine(
                    guiGraphics,
                    x,
                    y,
                    width,
                    Component.translatable("selectWorld.experimental"),
                    0xFFE2B86A,
                    false,
                    this.sidebarDetailsBottomY);
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
        boolean hasSelection = getSelectedSummary() != null;

        this.playButton.active = this.vanillaPlayButton != null ? this.vanillaPlayButton.active : hasSelection;
        this.createButton.active = this.vanillaCreateButton == null || this.vanillaCreateButton.active;
        this.editButton.active = this.vanillaEditButton != null && this.vanillaEditButton.active;
        this.deleteButton.active = this.vanillaDeleteButton != null && this.vanillaDeleteButton.active;
        this.recreateButton.active = this.vanillaRecreateButton != null && this.vanillaRecreateButton.active;

        this.playButton.setTooltip(this.playButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
        this.editButton.setTooltip(this.editButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
        this.deleteButton.setTooltip(this.deleteButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
        this.recreateButton.setTooltip(this.recreateButton.active ? null : Tooltip.create(NO_SELECTION_TOOLTIP));
    }

    private boolean hasWorldEntries() {
        if (this.worldList == null) {
            return false;
        }

        for (WorldSelectionList.Entry entry : this.worldList.children()) {
            if (entry instanceof WorldSelectionList.WorldListEntry) {
                return true;
            }
        }
        return false;
    }

    private boolean isWorldListLoading() {
        if (this.worldList == null) {
            return false;
        }

        for (WorldSelectionList.Entry entry : this.worldList.children()) {
            if (entry instanceof WorldSelectionList.LoadingHeader) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private LevelSummary getSelectedSummary() {
        if (this.worldList == null) {
            return null;
        }

        Optional<WorldSelectionList.WorldListEntry> selected = this.worldList.getSelectedOpt();
        if (selected.isEmpty()) {
            return null;
        }

        if (WORLD_ENTRY_SUMMARY_FIELD == null) {
            return null;
        }

        try {
            Object summary = WORLD_ENTRY_SUMMARY_FIELD.get(selected.get());
            return summary instanceof LevelSummary levelSummary ? levelSummary : null;
        } catch (IllegalAccessException exception) {
            return null;
        }
    }
    @Nullable
    private WorldSelectionList getWorldListFromField() {
        if (WORLD_LIST_FIELD == null) {
            return null;
        }

        try {
            Object value = WORLD_LIST_FIELD.get(this);
            return value instanceof WorldSelectionList list ? list : null;
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private void setWorldListField(WorldSelectionList list) {
        if (WORLD_LIST_FIELD == null) {
            return;
        }

        try {
            WORLD_LIST_FIELD.set(this, list);
        } catch (IllegalAccessException ignored) {
        }
    }

    @Nullable
    private static Field resolveField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    @Nullable
    private static Field resolveWorldEntrySummaryField() {
        try {
            Class<?> clazz = Class.forName("net.minecraft.client.gui.screens.worldselection.WorldSelectionList$WorldListEntry");
            Field field = clazz.getDeclaredField("summary");
            field.setAccessible(true);
            return field;
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            return null;
        }
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

    private static final class AshwakeWorldSelectionList extends WorldSelectionList {
        private final AshwakeSelectWorldScreen owner;
        private int rowWidth = 340;

        private AshwakeWorldSelectionList(
                AshwakeSelectWorldScreen owner,
                SelectWorldScreen screen,
                net.minecraft.client.Minecraft minecraft,
                int width,
                int height,
                int y,
                int itemHeight,
                String filter,
                @Nullable WorldSelectionList worlds) {
            super(screen, minecraft, width, height, y, itemHeight, filter, worlds);
            this.owner = owner;
        }

        void setRowWidth(int rowWidth) {
            this.rowWidth = rowWidth;
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
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
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
            WorldSelectionList.Entry entry = this.getEntry(index);
            boolean hovered = Objects.equals(this.getHovered(), entry);
            boolean selected = this.isSelectedItem(index);

            int lift = hovered && !selected ? 1 : 0;
            int rowTop = top - lift;
            int rowBottom = rowTop + height;
            int rowLeft = left - 2;
            int rowRight = left + width + 2;

            int clipLeft = Math.max(this.getX(), rowLeft);
            int clipTop = Math.max(this.getY(), rowTop);
            int clipRight = Math.min(this.getRight(), rowRight);
            int clipBottom = Math.min(this.getBottom(), rowBottom);
            if (clipRight <= clipLeft || clipBottom <= clipTop) {
                return;
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            drawRowHighlight(guiGraphics, rowLeft, rowTop, rowRight, rowBottom, selected, hovered);
            entry.renderBack(guiGraphics, index, rowTop, left, width, height, mouseX, mouseY, false, partialTick);
            entry.render(guiGraphics, index, rowTop, left, width, height, mouseX, mouseY, false, partialTick);
            guiGraphics.disableScissor();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
            // Selection is rendered in renderItem with the row-local basalt highlight.
        }

        private static void drawRowHighlight(
                GuiGraphics guiGraphics,
                int rowLeft,
                int rowTop,
                int rowRight,
                int rowBottom,
                boolean selected,
                boolean hovered) {
            int rowWidth = rowRight - rowLeft;
            int rowHeight = rowBottom - rowTop;
            if (rowWidth <= 0 || rowHeight <= 0) {
                return;
            }

            if (selected) {
                guiGraphics.fill(rowLeft, rowTop, rowRight, rowBottom, 0xAA1A1412);
                guiGraphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, 0xFF5B4B3E);
                guiGraphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, 0xFF5B4B3E);
                guiGraphics.fill(rowLeft, rowTop, rowLeft + 1, rowBottom, 0xFF5B4B3E);
                guiGraphics.fill(rowRight - 1, rowTop, rowRight, rowBottom, 0xFF5B4B3E);
                guiGraphics.fill(rowLeft, rowTop, rowLeft + 2, rowBottom, 0xFFF08A2B);
            } else if (hovered) {
                guiGraphics.fill(rowLeft, rowTop, rowRight, rowBottom, 0x661A1412);
            }
        }
    }
}
