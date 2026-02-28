package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.api.AshwakeMenuApi;
import com.ashwake.mainmenu.api.MenuAction;
import com.ashwake.mainmenu.client.AshwakeClientRuntime;
import com.ashwake.mainmenu.client.data.AshwakeChangelogLoader;
import com.ashwake.mainmenu.client.data.AshwakeChangelogLoader.LoadedChangelog;
import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.ashwake.mainmenu.client.widget.AshwakeButton.Icon;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.ashwake.mainmenu.internal.AshwakeMenuApiImpl;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

public final class AshwakeMainMenuScreen extends AshwakeScreenBase {
    private static final int MAIN_HEADER_HEIGHT = 186;
    private static final int PANEL_GAP = 12;
    private static final int STACK_BREAKPOINT = 760;
    private static final int ACTION_PANEL_MIN = 320;
    private static final int ACTION_PANEL_MAX = 520;
    private static final int PREVIEW_PANEL_MIN = 320;
    private static final int ACTION_BUTTON_HEIGHT = 26;
    private static final int ACTION_BUTTON_GAP = 8;
    private static final long TIP_ROTATE_MS = 8000L;
    private static final int TIP_FADE_MS = 150;
    private static final int PREVIEW_LINE_HEIGHT = 10;
    private static final int PREVIEW_CTA_REGION_HEIGHT = 64;
    private static final int PREVIEW_TEXT_REGION_GAP = 8;

    private static final List<String> FALLBACK_TIPS = List.of(
            "Reduce particles in Options if you want more FPS.",
            "Need help? Open Guidance for quick solutions.",
            "Check Changelog for new features and fixes.");

    private final AshwakeChangelogLoader changelogLoader = new AshwakeChangelogLoader();
    private final List<AshwakeButton> actionButtons = new ArrayList<>();
    private final List<String> latestUpdateLines = new ArrayList<>();
    private final List<String> tips = new ArrayList<>();
    private final List<FormattedCharSequence> wrappedLatestLines = new ArrayList<>();
    private final List<FormattedCharSequence> wrappedTipLines = new ArrayList<>();
    private final List<PreviewLine> previewLines = new ArrayList<>();

    private AshwakeButton guidanceCardButton;
    private AshwakeButton creditsButton;
    private AshwakeButton languageButton;

    private int leftPanelX;
    private int leftPanelY;
    private int leftPanelWidth;
    private int leftPanelHeight;
    private int rightPanelX;
    private int rightPanelY;
    private int rightPanelWidth;
    private int rightPanelHeight;

    private int scrollOffset;
    private int maxScroll;
    private int totalContentHeight;
    private int currentTipIndex;
    private long lastTipSwap;
    private boolean stackedLayout;
    private int previewTextScrollOffset;
    private int previewTextMaxScroll;
    private int previewTextAreaX;
    private int previewTextAreaY;
    private int previewTextAreaW;
    private int previewTextAreaH;

    public AshwakeMainMenuScreen() {
        super(Component.literal("Ashwake Main Menu"));
    }

    @Override
    protected int headerBottom() {
        return MAIN_HEADER_HEIGHT;
    }

    @Override
    protected int contentTop() {
        return headerBottom() + CONTENT_TOP_PADDING;
    }

    @Override
    protected void init() {
        actionButtons.clear();
        scrollOffset = 0;
        maxScroll = 0;
        previewTextScrollOffset = 0;
        previewTextMaxScroll = 0;
        currentTipIndex = 0;
        lastTipSwap = Util.getMillis();

        loadPreviewData();
        buildActionButtons();

        guidanceCardButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                220,
                34,
                Component.translatable("menu.ashwake.main.talkToGuidance"),
                Icon.GUIDANCE,
                button -> minecraft.setScreen(new AshwakeGuidanceScreen(this))));

        creditsButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                96,
                24,
                Component.translatable("menu.ashwake.main.credits"),
                Icon.CHANGELOG,
                button -> minecraft.setScreen(new AshwakeChangelogScreen(this))));

        languageButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                110,
                24,
                Component.translatable("menu.ashwake.main.language"),
                Icon.OPTIONS,
                button -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager()))));

        relayout();
        AshwakeClientRuntime.onMainMenuDisplayed(minecraft, this);
    }

    private void buildActionButtons() {
        List<ButtonSpec> specs = new ArrayList<>();
        specs.add(new ButtonSpec(Component.translatable("menu.ashwake.play"), Icon.PLAY, button -> minecraft.setScreen(new AshwakePlayHubScreen(this))));
        specs.add(new ButtonSpec(Component.translatable("menu.ashwake.options"), Icon.OPTIONS, button -> minecraft.setScreen(new AshwakeSettingsScreen(this))));
        specs.add(new ButtonSpec(Component.translatable("menu.ashwake.changelog"), Icon.CHANGELOG, button -> minecraft.setScreen(new AshwakeChangelogScreen(this))));
        specs.add(new ButtonSpec(Component.translatable("menu.ashwake.discord"), Icon.DISCORD, button -> openDiscordLink()));
        specs.add(new ButtonSpec(Component.translatable("menu.ashwake.quit"), Icon.QUIT, button -> minecraft.stop()));

        for (MenuAction action : AshwakeMenuApiImpl.getInstance().getMainMenuActions()) {
            specs.add(new ButtonSpec(
                    Component.translatable(action.labelKey()),
                    Icon.NONE,
                    button -> AshwakeMenuApiImpl.getInstance().invokeExternalCallback("mainmenu:" + action.id(), action.onClick())));
        }

        for (int i = 0; i < specs.size(); i++) {
            ButtonSpec spec = specs.get(i);
            AshwakeButton button = new AshwakeButton(0, 0, 200, ACTION_BUTTON_HEIGHT, spec.label(), spec.icon(), spec.onPress());
            if (i == 3 && AshwakeClientConfig.discordUrl().isBlank()) {
                button.active = false;
                button.setTooltip(Tooltip.create(Component.translatable("menu.ashwake.discord.comingSoon")));
            }
            addRenderableWidget(button);
            actionButtons.add(button);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!tips.isEmpty()) {
            long now = Util.getMillis();
            if (now - lastTipSwap >= TIP_ROTATE_MS) {
                currentTipIndex = (currentTipIndex + 1) % tips.size();
                lastTipSwap = now;
                refreshWrappedPreviewText();
                relayout();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (previewTextMaxScroll > 0
                && mouseX >= previewTextAreaX
                && mouseX <= previewTextAreaX + previewTextAreaW
                && mouseY >= previewTextAreaY
                && mouseY <= previewTextAreaY + previewTextAreaH) {
            previewTextScrollOffset = Mth.clamp(previewTextScrollOffset - (int) (scrollY * 16), 0, previewTextMaxScroll);
            return true;
        }
        if (!isInContentArea(mouseX, mouseY) || maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 18), 0, maxScroll);
        relayout();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_HOME && maxScroll > 0) {
            scrollOffset = 0;
            relayout();
            return true;
        }
        if (keyCode == InputConstants.KEY_END && maxScroll > 0) {
            scrollOffset = maxScroll;
            relayout();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);

        int shellX = 0;
        int shellWidth = width;
        drawHeaderFooterBands(guiGraphics, shellX, shellWidth);

        drawHeader(guiGraphics, shellX, shellWidth);
        drawContent(guiGraphics);
        drawFooter(guiGraphics, shellX, shellWidth);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphics guiGraphics, int shellX, int shellWidth) {
        float pulse = AshwakeClientConfig.reducedMotion()
                ? 0.5F
                : (0.5F + (0.5F * Mth.sin((Util.getMillis() / 5000.0F) * Mth.TWO_PI)));
        int logoMaxWidth = Mth.clamp((int) (width * 0.55F), 96, 440);
        int logoHeight = AshwakeBranding.drawCenteredLogo(guiGraphics, font, width / 2, 16, logoMaxWidth, 170, pulse);

        String version = ModList.get().getModContainerById(AshwakeMainMenuMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("dev");
        String versionTag = "v" + version;
        guiGraphics.drawString(font, versionTag, shellX + 12, 12, AshwakePalette.BONE_WHITE);

        String badge = AshwakeMenuApi.get().getMenuContext().badgeText();
        if (!badge.isBlank()) {
            int badgeWidth = font.width(badge) + 14;
            int badgeX = shellX + shellWidth - badgeWidth - 12;
            drawPanelFrame(guiGraphics, badgeX, 10, badgeWidth, 20, true);
            guiGraphics.drawString(font, badge, badgeX + 7, 16, AshwakePalette.BONE_WHITE);
        }

        int logoBottom = 16 + logoHeight;
        if (logoBottom + 4 < headerBottom()) {
            guiGraphics.fill(shellX + 1, logoBottom + 4, shellX + shellWidth - 1, logoBottom + 5, AshwakePalette.BASALT_EDGE);
        }
    }

    private void drawContent(GuiGraphics guiGraphics) {
        int clipLeft = SIDE_MARGIN;
        int clipRight = width - SIDE_MARGIN;
        int clipTop = contentTop();
        int clipBottom = contentBottom();

        guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        drawPanelFrame(guiGraphics, leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight, false);
        drawPanelFrame(guiGraphics, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight, false);

        drawActionPanel(guiGraphics);
        drawPreviewPanel(guiGraphics);
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(guiGraphics, clipRight - 6, clipTop, clipBottom, contentHeight(), totalContentHeight + CONTENT_BOTTOM_SAFE_PADDING, scrollOffset);
        }
    }

    private void drawActionPanel(GuiGraphics guiGraphics) {
        int x = leftPanelX + PANEL_PADDING;
        int y = leftPanelY + PANEL_PADDING;
        guiGraphics.drawString(font, Component.translatable("menu.ashwake.main.actions"), x, y, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(font, Component.translatable("menu.ashwake.main.actionsSub"), x, y + 12, AshwakePalette.MUTED_TEXT);
    }

    private void drawPreviewPanel(GuiGraphics guiGraphics) {
        drawPanelFrame(guiGraphics, previewTextAreaX, previewTextAreaY, previewTextAreaW, previewTextAreaH, false);
        drawPreviewTextLines(guiGraphics);

        int ctaLabelY = guidanceCardButton.getY() - 12;
        guiGraphics.drawString(
                font,
                Component.translatable("menu.ashwake.main.guidanceCard"),
                rightPanelX + PANEL_PADDING,
                ctaLabelY,
                AshwakePalette.BONE_WHITE);
    }

    private void drawFooter(GuiGraphics guiGraphics, int shellX, int shellWidth) {
        if (AshwakeClientConfig.debugUi()) {
            Component text = Component.translatable(
                    "menu.ashwake.main.sharpMode",
                    AshwakeClientConfig.forceSharpBackground() ? "ON" : "OFF");
            int textWidth = font.width(text);
            guiGraphics.drawString(font, text, shellX + shellWidth - textWidth - 12, footerButtonY() + 7, AshwakePalette.MUTED_TEXT);
        }
    }

    private void relayout() {
        int contentLeft = SIDE_MARGIN;
        int contentWidth = width - (SIDE_MARGIN * 2);
        int availableHeight = contentHeight();
        int contentTop = contentTop();

        stackedLayout = contentWidth < STACK_BREAKPOINT;
        if (!stackedLayout) {
            int tentativeLeft = Mth.clamp((int) (contentWidth * 0.44F), ACTION_PANEL_MIN, ACTION_PANEL_MAX);
            int tentativeRight = contentWidth - tentativeLeft - PANEL_GAP;
            if (tentativeRight < PREVIEW_PANEL_MIN) {
                int deficit = PREVIEW_PANEL_MIN - tentativeRight;
                tentativeLeft = Math.max(ACTION_PANEL_MIN, tentativeLeft - deficit);
                tentativeRight = contentWidth - tentativeLeft - PANEL_GAP;
            }
            if (tentativeRight < PREVIEW_PANEL_MIN) {
                stackedLayout = true;
            } else {
                leftPanelWidth = tentativeLeft;
                rightPanelWidth = tentativeRight;
            }
        }

        if (stackedLayout) {
            leftPanelWidth = contentWidth;
            rightPanelWidth = contentWidth;
        }

        refreshWrappedPreviewText();
        leftPanelHeight = computeActionPanelHeight();
        rightPanelHeight = computePreviewPanelHeight();

        if (stackedLayout) {
            totalContentHeight = leftPanelHeight + PANEL_GAP + rightPanelHeight;
            maxScroll = Math.max(0, (totalContentHeight + CONTENT_BOTTOM_SAFE_PADDING) - availableHeight);
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

            leftPanelX = contentLeft;
            leftPanelY = contentTop - scrollOffset;
            rightPanelX = contentLeft;
            rightPanelY = leftPanelY + leftPanelHeight + PANEL_GAP;
        } else {
            totalContentHeight = Math.max(leftPanelHeight, rightPanelHeight);
            maxScroll = Math.max(0, (totalContentHeight + CONTENT_BOTTOM_SAFE_PADDING) - availableHeight);
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

            leftPanelX = contentLeft;
            leftPanelY = contentTop - scrollOffset;
            rightPanelX = contentLeft + leftPanelWidth + PANEL_GAP;
            rightPanelY = contentTop - scrollOffset;
        }

        relayoutActionButtons();
        relayoutPreviewButton();
        relayoutPreviewTextRegion();
        relayoutFooterButtons();
    }

    private void relayoutActionButtons() {
        int buttonWidth = leftPanelWidth - (PANEL_PADDING * 2);
        int buttonX = leftPanelX + PANEL_PADDING;
        int startY = leftPanelY + PANEL_PADDING + 24;

        for (int i = 0; i < actionButtons.size(); i++) {
            AshwakeButton button = actionButtons.get(i);
            int y = startY + (i * (ACTION_BUTTON_HEIGHT + ACTION_BUTTON_GAP));
            button.setX(buttonX);
            button.setY(y);
            button.setWidth(buttonWidth);
            button.setHeight(ACTION_BUTTON_HEIGHT);
            button.visible = isVisibleInContent(y, ACTION_BUTTON_HEIGHT);
        }
    }

    private void relayoutPreviewButton() {
        int width = rightPanelWidth - (PANEL_PADDING * 2);
        int x = rightPanelX + PANEL_PADDING;
        int y = rightPanelY + rightPanelHeight - PANEL_PADDING - 34;
        guidanceCardButton.setX(x);
        guidanceCardButton.setY(y);
        guidanceCardButton.setWidth(width);
        guidanceCardButton.setHeight(34);
        guidanceCardButton.visible = isVisibleInContent(y, 34);
    }

    private void relayoutFooterButtons() {
        int y = footerButtonY();
        creditsButton.setX(SIDE_MARGIN + 2);
        creditsButton.setY(y);
        languageButton.setX(SIDE_MARGIN + 104);
        languageButton.setY(y);
        languageButton.visible = width >= 420;
    }

    private boolean isVisibleInContent(int y, int h) {
        return y + h >= contentTop() && y <= contentBottom();
    }

    private int computeActionPanelHeight() {
        int stackHeight = actionButtons.size() * ACTION_BUTTON_HEIGHT + Math.max(0, actionButtons.size() - 1) * ACTION_BUTTON_GAP;
        return PANEL_PADDING + 22 + stackHeight + PANEL_PADDING;
    }

    private int computePreviewPanelHeight() {
        int total = PANEL_PADDING
                + 14
                + 110
                + PREVIEW_TEXT_REGION_GAP
                + PREVIEW_CTA_REGION_HEIGHT
                + PANEL_PADDING;
        return Math.max(248, total);
    }

    private void refreshWrappedPreviewText() {
        int wrapWidth = Math.max(120, rightPanelWidth - (PANEL_PADDING * 2) - 8);
        wrappedLatestLines.clear();
        if (latestUpdateLines.isEmpty()) {
            wrappedLatestLines.addAll(font.split(Component.translatable("menu.ashwake.main.updatesSoon"), wrapWidth));
        } else {
            for (String line : latestUpdateLines) {
                wrappedLatestLines.addAll(font.split(Component.literal("- " + line), wrapWidth));
            }
        }

        wrappedTipLines.clear();
        if (tips.isEmpty()) {
            wrappedTipLines.addAll(font.split(Component.literal(FALLBACK_TIPS.getFirst()), wrapWidth));
        } else {
            String tip = tips.get(Math.floorMod(currentTipIndex, tips.size()));
            wrappedTipLines.addAll(font.split(Component.literal(tip), wrapWidth));
        }
        rebuildPreviewLines();
    }

    private void loadPreviewData() {
        latestUpdateLines.clear();
        tips.clear();

        LoadedChangelog loaded = changelogLoader.load();
        Map<String, List<String>> sections = loaded.sections();
        List<String> highlights = sections.getOrDefault("Highlights", List.of());
        if (highlights.isEmpty()) {
            for (List<String> sectionLines : sections.values()) {
                if (!sectionLines.isEmpty()) {
                    highlights = sectionLines;
                    break;
                }
            }
        }

        for (String line : highlights) {
            String cleaned = line.replaceFirst("^[*-]\\s*", "").trim();
            if (!cleaned.isBlank()) {
                latestUpdateLines.add(cleaned);
            }
            if (latestUpdateLines.size() >= 5) {
                break;
            }
        }

        tips.addAll(FALLBACK_TIPS);
        for (String configured : AshwakeClientConfig.loadingTips()) {
            String clean = configured == null ? "" : configured.trim();
            if (!clean.isBlank() && !tips.contains(clean)) {
                tips.add(clean);
            }
        }
    }

    private void openDiscordLink() {
        String url = AshwakeClientConfig.discordUrl();
        if (url.isBlank()) {
            return;
        }

        try {
            Util.getPlatform().openUri(url);
        } catch (Exception exception) {
            AshwakeMainMenuMod.LOGGER.warn("Failed opening configured Discord URL {}", url, exception);
        }
    }

    private int currentTipFadeAlpha() {
        if (AshwakeClientConfig.reducedMotion()) {
            return 255;
        }
        long elapsed = Util.getMillis() - lastTipSwap;
        if (elapsed >= TIP_FADE_MS) {
            return 255;
        }
        return Mth.clamp((int) ((elapsed / (float) TIP_FADE_MS) * 255.0F), 0, 255);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private void relayoutPreviewTextRegion() {
        previewTextAreaX = rightPanelX + PANEL_PADDING;
        previewTextAreaY = rightPanelY + PANEL_PADDING;
        previewTextAreaW = Math.max(100, rightPanelWidth - (PANEL_PADDING * 2));
        previewTextAreaH = Math.max(
                52,
                rightPanelHeight - (PANEL_PADDING * 2) - PREVIEW_CTA_REGION_HEIGHT - PREVIEW_TEXT_REGION_GAP);
        updatePreviewTextScrollBounds();
    }

    private void rebuildPreviewLines() {
        previewLines.clear();
        previewLines.add(new PreviewLine(Component.translatable("menu.ashwake.main.latestUpdate").getVisualOrderText(), AshwakePalette.LAVA_YELLOW));
        for (FormattedCharSequence line : wrappedLatestLines) {
            previewLines.add(new PreviewLine(line, AshwakePalette.BONE_WHITE));
        }

        previewLines.add(new PreviewLine(Component.empty().getVisualOrderText(), AshwakePalette.BONE_WHITE));
        int tipAlpha = currentTipFadeAlpha();
        previewLines.add(new PreviewLine(
                Component.translatable("menu.ashwake.main.tipOfDay").getVisualOrderText(),
                withAlpha(AshwakePalette.LAVA_YELLOW, tipAlpha)));
        for (FormattedCharSequence line : wrappedTipLines) {
            previewLines.add(new PreviewLine(line, withAlpha(AshwakePalette.MUTED_TEXT, tipAlpha)));
        }
        updatePreviewTextScrollBounds();
    }

    private void updatePreviewTextScrollBounds() {
        int totalHeight = Math.max(PREVIEW_LINE_HEIGHT, previewLines.size() * PREVIEW_LINE_HEIGHT);
        previewTextMaxScroll = Math.max(0, totalHeight - Math.max(1, previewTextAreaH - 8));
        previewTextScrollOffset = Mth.clamp(previewTextScrollOffset, 0, previewTextMaxScroll);
    }

    private void drawPreviewTextLines(GuiGraphics guiGraphics) {
        int drawX = previewTextAreaX + 6;
        int drawY = previewTextAreaY + 6;
        int drawH = previewTextAreaH - 12;
        int firstLine = previewTextScrollOffset / PREVIEW_LINE_HEIGHT;
        int lineOffset = -(previewTextScrollOffset % PREVIEW_LINE_HEIGHT);
        int maxLines = (drawH / PREVIEW_LINE_HEIGHT) + 3;

        guiGraphics.enableScissor(
                previewTextAreaX + 1,
                previewTextAreaY + 1,
                previewTextAreaX + previewTextAreaW - 1,
                previewTextAreaY + previewTextAreaH - 1);

        for (int i = 0; i < maxLines; i++) {
            int index = firstLine + i;
            if (index >= previewLines.size()) {
                break;
            }
            int lineY = drawY + lineOffset + (i * PREVIEW_LINE_HEIGHT);
            if (lineY < drawY - PREVIEW_LINE_HEIGHT || lineY > drawY + drawH) {
                continue;
            }
            PreviewLine line = previewLines.get(index);
            guiGraphics.drawString(font, line.text(), drawX, lineY, line.color());
        }

        guiGraphics.disableScissor();
    }

    private record ButtonSpec(
            Component label,
            Icon icon,
            AshwakeButton.OnPress onPress) {
    }

    private record PreviewLine(FormattedCharSequence text, int color) {
    }
}

