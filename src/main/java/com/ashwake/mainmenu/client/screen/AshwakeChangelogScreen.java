package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.data.AshwakeChangelogLoader;
import com.ashwake.mainmenu.client.data.AshwakeChangelogLoader.LoadedChangelog;
import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class AshwakeChangelogScreen extends AshwakeScreenBase {
    private static final String CATEGORY_LABEL_KEY = "menu.ashwake.changelog.categoryLabel";
    private static final int HEADER_CONTROLS_Y = 40;
    private static final int HEADER_CONTROL_HEIGHT = 24;
    private static final int HEADER_CONTROL_GAP = 10;
    private static final int CATEGORY_LABEL_MIN_WIDTH = 90;
    private static final int CATEGORY_LABEL_MAX_WIDTH = 160;
    private static final int CATEGORY_DROPDOWN_MIN_WIDTH = 160;

    private final Screen parent;
    private final AshwakeChangelogLoader loader = new AshwakeChangelogLoader();

    private LoadedChangelog loaded;
    private List<String> categories = List.of();
    private final List<String> visibleLines = new ArrayList<>();

    private CategoryDropdownButton categoryButton;
    private AshwakeButton versionButton;
    private AshwakeButton discordButton;
    private AshwakeButton githubButton;
    private EditBox searchBox;

    private int selectedCategoryIndex;
    private int panelX;
    private int panelWidth;
    private int scrollOffset;
    private int maxScroll;
    private boolean needsRefresh;
    private boolean stackedCategoryLayout;
    private int categoryLabelX;
    private int categoryLabelY;

    public AshwakeChangelogScreen(Screen parent) {
        super(Component.translatable("menu.ashwake.changelog"));
        this.parent = parent;
    }

    @Override
    protected int headerBottom() {
        return 112;
    }

    @Override
    protected int contentTop() {
        return headerBottom() + 8;
    }

    @Override
    protected void init() {
        loaded = loader.load();
        categories = new ArrayList<>(loaded.sections().keySet());
        if (categories.isEmpty()) {
            categories = List.of("Highlights");
        }

        panelWidth = safePanelWidth(780);
        panelX = safePanelX(780);

        categoryButton = addRenderableWidget(new CategoryDropdownButton(
                0,
                0,
                CATEGORY_DROPDOWN_MIN_WIDTH,
                HEADER_CONTROL_HEIGHT,
                button -> {
                    selectedCategoryIndex = (selectedCategoryIndex + 1) % categories.size();
                    needsRefresh = true;
                    refreshHeaderLabels();
                }));

        versionButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                210,
                HEADER_CONTROL_HEIGHT,
                Component.empty(),
                button -> refreshHeaderLabels()));

        searchBox = new EditBox(font, 0, 0, panelWidth - 24, 20, Component.translatable("menu.search"));
        searchBox.setMaxLength(120);
        searchBox.setResponder(value -> needsRefresh = true);
        addRenderableWidget(searchBox);

        relayoutHeaderControls();

        discordButton = addRenderableWidget(new AshwakeButton(
                panelX + 12,
                footerButtonY(),
                140,
                24,
                Component.translatable("menu.ashwake.discord"),
                AshwakeButton.Icon.DISCORD,
                button -> openDiscord()));
        if (AshwakeClientConfig.discordUrl().isBlank()) {
            discordButton.active = false;
            discordButton.setTooltip(Tooltip.create(Component.translatable("menu.ashwake.discord.comingSoon")));
        }

        githubButton = addRenderableWidget(new AshwakeButton(
                panelX + 162,
                footerButtonY(),
                166,
                24,
                Component.translatable("menu.ashwake.changelog.github"),
                AshwakeButton.Icon.CHANGELOG,
                button -> openGithub()));
        String githubUrl = AshwakeClientConfig.changelogGithubUrl();
        if (!isValidExternalUrl(githubUrl)) {
            githubButton.active = false;
            githubButton.setTooltip(Tooltip.create(Component.translatable("menu.ashwake.changelog.github.comingSoon")));
        }

        addRenderableWidget(new AshwakeButton(
                panelX + panelWidth - 114,
                footerButtonY(),
                102,
                24,
                Component.translatable("gui.back"),
                button -> minecraft.setScreen(parent)));

        scrollOffset = 0;
        maxScroll = 0;
        refreshHeaderLabels();
        needsRefresh = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (needsRefresh) {
            refreshVisibleLines();
            needsRefresh = false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInContentArea(mouseX, mouseY) || maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 13), 0, maxScroll);
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);

        int logoSize = AshwakeBranding.drawLeftLogo(guiGraphics, font, panelX + 10, 11, 30, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(font, title, panelX + logoSize + 18, 18, AshwakePalette.LAVA_YELLOW);
        drawCategoryLabel(guiGraphics);

        int contentX = panelX + 10;
        int contentY = contentTop();
        int contentW = panelWidth - 20;
        int contentH = contentHeight();
        drawPanelFrame(guiGraphics, contentX, contentY, contentW, contentH, false);

        drawChangelogLines(guiGraphics, contentX + 8, contentY + 8, contentW - 20, contentH - 16);
        if (maxScroll > 0) {
            drawScrollbar(
                    guiGraphics,
                    contentX + contentW - 7,
                    contentY + 2,
                    contentY + contentH - 2,
                    contentH - 4,
                    maxScroll + contentH - 4,
                    scrollOffset);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void relayoutHeaderControls() {
        int controlsLeft = panelX + 12;
        int controlsRight = panelX + panelWidth - 12;
        int rowY = HEADER_CONTROLS_Y;
        int versionWidth = Mth.clamp(panelWidth / 4, 120, 210);
        int versionX = controlsRight - versionWidth;

        int categoryAreaX = controlsLeft;
        int categoryAreaW = Math.max(100, versionX - categoryAreaX - HEADER_CONTROL_GAP);
        int labelWidth = Mth.clamp(
                font.width(Component.translatable(CATEGORY_LABEL_KEY)) + 10,
                CATEGORY_LABEL_MIN_WIDTH,
                CATEGORY_LABEL_MAX_WIDTH);
        int dropdownX = categoryAreaX + labelWidth + HEADER_CONTROL_GAP;
        int dropdownW = categoryAreaW - labelWidth - HEADER_CONTROL_GAP;

        stackedCategoryLayout = dropdownW < CATEGORY_DROPDOWN_MIN_WIDTH;

        versionButton.setX(versionX);
        versionButton.setWidth(versionWidth);
        versionButton.setHeight(HEADER_CONTROL_HEIGHT);

        if (stackedCategoryLayout) {
            categoryLabelX = categoryAreaX;
            categoryLabelY = rowY + 2;

            categoryButton.setX(categoryAreaX);
            categoryButton.setY(rowY + 12);
            categoryButton.setWidth(categoryAreaW);
            categoryButton.setHeight(HEADER_CONTROL_HEIGHT);

            versionButton.setY(rowY + 12);
            searchBox.setY(rowY + 44);
        } else {
            categoryLabelX = categoryAreaX;
            categoryLabelY = rowY + ((HEADER_CONTROL_HEIGHT - 8) / 2);

            categoryButton.setX(dropdownX);
            categoryButton.setY(rowY);
            categoryButton.setWidth(dropdownW);
            categoryButton.setHeight(HEADER_CONTROL_HEIGHT);

            versionButton.setY(rowY);
            searchBox.setY(rowY + HEADER_CONTROL_HEIGHT + 8);
        }

        searchBox.setX(panelX + 12);
        searchBox.setWidth(panelWidth - 24);
        searchBox.setHeight(20);
    }

    private void drawCategoryLabel(GuiGraphics guiGraphics) {
        guiGraphics.drawString(
                font,
                Component.translatable(CATEGORY_LABEL_KEY),
                categoryLabelX,
                categoryLabelY,
                AshwakePalette.BONE_WHITE);
    }

    private void drawChangelogLines(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (visibleLines.isEmpty()) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("menu.ashwake.changelog.notConfigured"),
                    x,
                    y + 8,
                    AshwakePalette.BONE_WHITE);
            return;
        }

        int lineHeight = 11;
        int firstLine = scrollOffset / lineHeight;
        int lineOffset = -(scrollOffset % lineHeight);
        int maxLinesOnScreen = (height / lineHeight) + 2;

        for (int i = 0; i < maxLinesOnScreen; i++) {
            int index = firstLine + i;
            if (index >= visibleLines.size()) {
                break;
            }
            int lineY = y + lineOffset + (i * lineHeight);
            if (lineY < y - lineHeight || lineY > y + height) {
                continue;
            }

            String line = visibleLines.get(index);
            boolean section = line.startsWith("##");
            int color = section ? AshwakePalette.LAVA_YELLOW : AshwakePalette.BONE_WHITE;
            String renderedLine = section ? line.substring(2).trim() : line;
            if (font.width(renderedLine) > width) {
                renderedLine = font.plainSubstrByWidth(renderedLine, width - 6);
            }
            guiGraphics.drawString(font, renderedLine, x, lineY, color);
        }
    }

    private void refreshVisibleLines() {
        visibleLines.clear();
        scrollOffset = 0;

        String category = categories.get(selectedCategoryIndex);
        Map<String, List<String>> sections = loaded.sections();
        List<String> lines = sections.getOrDefault(category, List.of());
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        visibleLines.add("## " + category);
        for (String line : lines) {
            if (!query.isEmpty() && !line.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            visibleLines.add(line);
        }

        int lineHeight = 11;
        int scrollableHeight = Math.max(20, contentHeight() - 16);
        int totalHeight = Math.max(lineHeight, visibleLines.size() * lineHeight);
        maxScroll = Math.max(0, (totalHeight + CONTENT_BOTTOM_SAFE_PADDING) - scrollableHeight);
    }

    private void refreshHeaderLabels() {
        String selectedCategory = categories.get(selectedCategoryIndex);
        categoryButton.setMessage(Component.literal(selectedCategory));
        categoryButton.setTooltip(Tooltip.create(Component.literal(selectedCategory)));
        versionButton.setMessage(Component.translatable("menu.ashwake.changelog.version", loaded.version()));
    }

    private void openDiscord() {
        String url = AshwakeClientConfig.discordUrl();
        if (!isValidExternalUrl(url)) {
            return;
        }
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception ignored) {
        }
    }

    private void openGithub() {
        String url = AshwakeClientConfig.changelogGithubUrl();
        if (!isValidExternalUrl(url)) {
            return;
        }
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception ignored) {
        }
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || text.isEmpty() || font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (maxWidth <= ellipsisWidth) {
            return ellipsis;
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    private static boolean isValidExternalUrl(String url) {
        String clean = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        return clean.startsWith("http://") || clean.startsWith("https://");
    }

    private final class CategoryDropdownButton extends Button {
        private static final int LEFT_PADDING = 12;
        private static final int RIGHT_PADDING = 12;
        private static final int ARROW_SPACE = 20;
        private static final int HOVER_RIM_COLOR = 0x8CF2D5AE;
        private static final int HOVER_RIM_SOFT_COLOR = 0x4ADC8A42;

        private CategoryDropdownButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) {
                return;
            }

            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hovered = active && isHoveredOrFocused();
            drawPanelFrame(guiGraphics, x, y, w, h, false);
            if (hovered) {
                // Keep dropdown hover clean: thin rim only, no focus-glow texture.
                guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, HOVER_RIM_COLOR);
                guiGraphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, HOVER_RIM_SOFT_COLOR);
                guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 1, HOVER_RIM_SOFT_COLOR);
                guiGraphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, HOVER_RIM_SOFT_COLOR);
            }

            int maxTextWidth = Math.max(8, w - LEFT_PADDING - RIGHT_PADDING - ARROW_SPACE);
            String value = ellipsize(getMessage().getString(), maxTextWidth);
            int textX = x + LEFT_PADDING;
            int textY = y + ((h - 8) / 2);

            int clipLeft = textX;
            int clipTop = y + 2;
            int clipRight = x + w - RIGHT_PADDING - ARROW_SPACE;
            int clipBottom = y + h - 2;
            if (clipRight > clipLeft && clipBottom > clipTop) {
                guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
                guiGraphics.drawString(
                        font,
                        value,
                        textX,
                        textY,
                        active ? AshwakePalette.BONE_WHITE : AshwakePalette.MUTED_TEXT);
                guiGraphics.disableScissor();
            }

            int arrowColor = active ? AshwakePalette.LAVA_YELLOW : AshwakePalette.MUTED_TEXT;
            int arrowCenterX = x + w - 10;
            int arrowCenterY = y + (h / 2);
            guiGraphics.fill(arrowCenterX - 3, arrowCenterY - 1, arrowCenterX + 3, arrowCenterY, arrowColor);
            guiGraphics.fill(arrowCenterX - 2, arrowCenterY, arrowCenterX + 2, arrowCenterY + 1, arrowColor);
            guiGraphics.fill(arrowCenterX - 1, arrowCenterY + 1, arrowCenterX + 1, arrowCenterY + 2, arrowColor);
        }
    }
}
