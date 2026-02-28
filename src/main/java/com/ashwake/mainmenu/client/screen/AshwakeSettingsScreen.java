package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.ashwake.mainmenu.config.AshwakeClientConfig.ChangelogMode;
import com.ashwake.mainmenu.config.AshwakeClientConfig.PerformancePreset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class AshwakeSettingsScreen extends AshwakeScreenBase {
    private static final int PANEL_MAX_WIDTH = 920;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_GAP = 8;
    private static final int CONTENT_PAD = 10;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 8;
    private static final int HEADER_HEIGHT_LOCAL = 20;
    private static final int LINE_HEIGHT = 10;
    private static final long RESET_CONFIRM_MS = 4200L;
    private static final int LINK_ROW_HEIGHT = 44;
    private static final int LINK_BUTTON_WIDTH = 76;

    private final Screen parent;
    private final EnumMap<SettingsTab, AshwakeButton> tabButtons = new EnumMap<>(SettingsTab.class);
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<TextLine> contentText = new ArrayList<>();

    private SettingsTab activeTab = SettingsTab.VISUAL;
    private boolean showDebugPage;
    private long resetConfirmUntil;

    private AshwakeButton openMinecraftOptionsButton;
    private AshwakeButton doneButton;
    private AshwakeButton backButton;

    private AshwakeButton animationsToggle;
    private IntSlider animationIntensitySlider;
    private AshwakeButton particlesToggle;
    private IntSlider particleDensitySlider;
    private AshwakeButton hoverParticlesToggle;
    private IntSlider hoverParticleDensitySlider;
    private AshwakeButton reducedMotionToggle;

    private AshwakeButton performancePresetButton;
    private IntSlider backgroundDarkenSlider;

    private AshwakeButton discordOpenButton;
    private AshwakeButton discordCopyButton;
    private AshwakeButton githubOpenButton;
    private AshwakeButton githubCopyButton;
    private AshwakeButton changelogModeButton;

    private AshwakeButton forceSharpUiButton;
    private AshwakeButton blurCompatGlobalButton;
    private AshwakeButton debugPageButton;

    private AshwakeButton allowBlurForDebugButton;
    private AshwakeButton showSharpOverlayButton;
    private AshwakeButton resetSettingsButton;

    private int panelX;
    private int panelWidth;
    private int tabsY;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int contentLogicalHeight;
    private int scrollOffset;
    private int maxScroll;

    public AshwakeSettingsScreen(Screen parent) {
        super(Component.translatable("menu.ashwake.settings"));
        this.parent = parent;
    }

    @Override
    protected int headerBottom() {
        return 56;
    }

    @Override
    protected int contentTop() {
        return headerBottom() + 6;
    }

    @Override
    protected void init() {
        scrollOffset = 0;
        maxScroll = 0;
        showDebugPage = false;
        resetConfirmUntil = 0L;

        buildTabs();
        buildControls();
        buildFooterButtons();

        refreshState();
        relayout();
    }

    @Override
    public void tick() {
        super.tick();
        if (resetConfirmUntil > 0L && Util.getMillis() > resetConfirmUntil) {
            resetConfirmUntil = 0L;
            refreshState();
            relayout();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0 || !isInScrollableContent(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 16), 0, maxScroll);
        relayoutContentOnly();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (maxScroll > 0) {
            if (keyCode == 268) {
                scrollOffset = 0;
                relayoutContentOnly();
                return true;
            }
            if (keyCode == 269) {
                scrollOffset = maxScroll;
                relayoutContentOnly();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);

        drawHeader(guiGraphics);
        drawTabsBand(guiGraphics);
        drawContent(guiGraphics);
        drawFooterText(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        saveValues();
        AshwakeClientConfig.save();
        super.removed();
    }

    private void buildTabs() {
        for (SettingsTab tab : SettingsTab.values()) {
            AshwakeButton button = addRenderableWidget(new AshwakeButton(
                    0, 0, 100, TAB_HEIGHT, tab.title(), AshwakeButton.Icon.NONE, b -> {
                activeTab = tab;
                showDebugPage = false;
                scrollOffset = 0;
                relayout();
            }));
            tabButtons.put(tab, button);
        }
    }

    private void buildControls() {
        animationsToggle = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            AshwakeClientConfig.setAnimationsEnabled(!AshwakeClientConfig.animationsEnabled());
            onChanged();
        }));

        animationIntensitySlider = addContent(new IntSlider(0, 0, 120, ROW_HEIGHT, 0, 100,
                AshwakeClientConfig::animationIntensity, AshwakeClientConfig::setAnimationIntensity, this::onChanged));

        particlesToggle = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            AshwakeClientConfig.setParticlesEnabled(!AshwakeClientConfig.particlesEnabled());
            onChanged();
        }));

        particleDensitySlider = addContent(new IntSlider(0, 0, 120, ROW_HEIGHT, 0, 100,
                AshwakeClientConfig::particleDensity, AshwakeClientConfig::setParticleDensity, this::onChanged));

        hoverParticlesToggle = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            AshwakeClientConfig.setHoverParticlesEnabled(!AshwakeClientConfig.hoverParticlesEnabled());
            onChanged();
        }));

        hoverParticleDensitySlider = addContent(new IntSlider(0, 0, 120, ROW_HEIGHT, 0, 100,
                AshwakeClientConfig::hoverParticleDensity, AshwakeClientConfig::setHoverParticleDensity, this::onChanged));

        reducedMotionToggle = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            AshwakeClientConfig.setReducedMotion(!AshwakeClientConfig.reducedMotion());
            onChanged();
        }));

        performancePresetButton = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            PerformancePreset next = nextPresetFromCurrent();
            AshwakeClientConfig.applyPerformancePreset(next);
            onChanged();
        }));

        backgroundDarkenSlider = addContent(new IntSlider(0, 0, 120, ROW_HEIGHT, 0, 100,
                AshwakeClientConfig::backgroundDarken, AshwakeClientConfig::setBackgroundDarken, this::onChanged));

        discordOpenButton = addContent(new AshwakeButton(
                0,
                0,
                LINK_BUTTON_WIDTH,
                ROW_HEIGHT,
                Component.translatable("menu.ashwake.settings.link.open"),
                AshwakeButton.Icon.NONE,
                b -> openUrl(AshwakeClientConfig.discordUrl())));
        discordCopyButton = addContent(new AshwakeButton(
                0,
                0,
                LINK_BUTTON_WIDTH,
                ROW_HEIGHT,
                Component.translatable("menu.ashwake.settings.link.copy"),
                AshwakeButton.Icon.NONE,
                b -> copyUrl(AshwakeClientConfig.discordUrl())));

        githubOpenButton = addContent(new AshwakeButton(
                0,
                0,
                LINK_BUTTON_WIDTH,
                ROW_HEIGHT,
                Component.translatable("menu.ashwake.settings.link.open"),
                AshwakeButton.Icon.NONE,
                b -> openUrl(AshwakeClientConfig.changelogGithubUrl())));
        githubCopyButton = addContent(new AshwakeButton(
                0,
                0,
                LINK_BUTTON_WIDTH,
                ROW_HEIGHT,
                Component.translatable("menu.ashwake.settings.link.copy"),
                AshwakeButton.Icon.NONE,
                b -> copyUrl(AshwakeClientConfig.changelogGithubUrl())));

        changelogModeButton = addContent(new AshwakeButton(0, 0, 140, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            if (AshwakeClientConfig.changelogRemoteUrl().isBlank()) {
                AshwakeClientConfig.setChangelogMode(ChangelogMode.LOCAL_ONLY);
            } else {
                ChangelogMode mode = AshwakeClientConfig.changelogMode() == ChangelogMode.LOCAL_ONLY
                        ? ChangelogMode.REMOTE_OK : ChangelogMode.LOCAL_ONLY;
                AshwakeClientConfig.setChangelogMode(mode);
            }
            onChanged();
        }));

        forceSharpUiButton = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            boolean next = !AshwakeClientConfig.disableBlurOnAshwakeScreens();
            AshwakeClientConfig.setDisableBlurOnAshwakeScreens(next);
            if (next) {
                AshwakeClientConfig.setForceSharpBackground(true);
            }
            onChanged();
        }));
        forceSharpUiButton.setTooltip(Tooltip.create(Component.translatable("menu.ashwake.settings.forceSharp.tooltip")));

        blurCompatGlobalButton = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            AshwakeClientConfig.setDisableMenuBlurGlobally(!AshwakeClientConfig.disableMenuBlurGlobally());
            onChanged();
        }));
        blurCompatGlobalButton.setTooltip(Tooltip.create(Component.translatable("menu.ashwake.settings.blurGlobal.tooltip")));

        debugPageButton = addContent(new AshwakeButton(
                0, 0, 120, ROW_HEIGHT, Component.translatable("menu.ashwake.settings.debug.openPage"), AshwakeButton.Icon.NONE, b -> {
            showDebugPage = true;
            scrollOffset = 0;
            relayoutContentOnly();
        }));

        allowBlurForDebugButton = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            boolean next = !AshwakeClientConfig.allowBlurForDebug();
            AshwakeClientConfig.setAllowBlurForDebug(next);
            if (!next) {
                AshwakeClientConfig.setForceSharpBackground(true);
            }
            onChanged();
        }));

        showSharpOverlayButton = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            AshwakeClientConfig.setDebugUi(!AshwakeClientConfig.debugUi());
            if (!AshwakeClientConfig.debugUi()) {
                showDebugPage = false;
            }
            onChanged();
        }));

        resetSettingsButton = addContent(new AshwakeButton(0, 0, 120, ROW_HEIGHT, Component.empty(), AshwakeButton.Icon.NONE, b -> {
            long now = Util.getMillis();
            if (resetConfirmUntil > now) {
                applyDefaults();
                resetConfirmUntil = 0L;
            } else {
                resetConfirmUntil = now + RESET_CONFIRM_MS;
            }
            onChanged();
        }));
    }

    private void buildFooterButtons() {
        openMinecraftOptionsButton = addRenderableWidget(new AshwakeButton(
                0, 0, 190, 24, Component.translatable("menu.ashwake.openMinecraftOptions"), AshwakeButton.Icon.OPTIONS,
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));

        doneButton = addRenderableWidget(new AshwakeButton(
                0, 0, 98, 24, Component.translatable("gui.done"), AshwakeButton.Icon.NONE,
                b -> {
                    saveValues();
                    minecraft.setScreen(parent);
                }));

        backButton = addRenderableWidget(new AshwakeButton(
                0, 0, 98, 24, Component.translatable("gui.back"), AshwakeButton.Icon.NONE,
                b -> {
                    saveValues();
                    minecraft.setScreen(parent);
                }));
    }

    private void onChanged() {
        refreshState();
        relayout();
    }

    private void refreshState() {
        animationsToggle.setMessage(onOff(AshwakeClientConfig.animationsEnabled()));
        particlesToggle.setMessage(onOff(AshwakeClientConfig.particlesEnabled()));
        hoverParticlesToggle.setMessage(onOff(AshwakeClientConfig.hoverParticlesEnabled()));
        reducedMotionToggle.setMessage(onOff(AshwakeClientConfig.reducedMotion()));

        animationIntensitySlider.sync();
        particleDensitySlider.sync();
        hoverParticleDensitySlider.sync();
        backgroundDarkenSlider.sync();

        animationIntensitySlider.active = AshwakeClientConfig.animationsEnabled();
        particleDensitySlider.active = AshwakeClientConfig.particlesEnabled();
        hoverParticleDensitySlider.active = AshwakeClientConfig.hoverParticlesEnabled();

        performancePresetButton.setMessage(Component.literal(displayPreset()));
        boolean hasRemoteUrl = !AshwakeClientConfig.changelogRemoteUrl().isBlank();
        if (!hasRemoteUrl && AshwakeClientConfig.changelogMode() == ChangelogMode.REMOTE_OK) {
            AshwakeClientConfig.setChangelogMode(ChangelogMode.LOCAL_ONLY);
        }
        changelogModeButton.setMessage(Component.literal(AshwakeClientConfig.changelogMode().name()));
        changelogModeButton.active = hasRemoteUrl;
        changelogModeButton.setTooltip(hasRemoteUrl
                ? null
                : Tooltip.create(Component.translatable("menu.ashwake.settings.remoteUnavailable")));

        refreshLinkButtonState(
                AshwakeClientConfig.discordUrl(),
                discordOpenButton,
                discordCopyButton,
                Component.translatable("menu.ashwake.discord.comingSoon"));
        refreshLinkButtonState(
                AshwakeClientConfig.changelogGithubUrl(),
                githubOpenButton,
                githubCopyButton,
                Component.translatable("menu.ashwake.changelog.github.comingSoon"));

        forceSharpUiButton.setMessage(onOff(AshwakeClientConfig.disableBlurOnAshwakeScreens()));
        blurCompatGlobalButton.setMessage(onOff(AshwakeClientConfig.disableMenuBlurGlobally()));
        allowBlurForDebugButton.setMessage(onOff(AshwakeClientConfig.allowBlurForDebug()));
        showSharpOverlayButton.setMessage(onOff(AshwakeClientConfig.debugUi()));
        resetSettingsButton.setMessage(resetConfirmUntil > Util.getMillis()
                ? Component.translatable("menu.ashwake.settings.debug.resetConfirm")
                : Component.translatable("menu.ashwake.settings.debug.reset"));
    }

    private void relayout() {
        panelWidth = safePanelWidth(PANEL_MAX_WIDTH);
        panelX = safePanelX(PANEL_MAX_WIDTH);
        tabsY = headerBottom() + 6;

        layoutTabs();
        layoutFooter();
        relayoutContentOnly();
    }

    private void relayoutContentOnly() {
        contentX = panelX + CONTENT_PAD;
        contentY = tabsY + TAB_HEIGHT + 10;
        contentWidth = panelWidth - (CONTENT_PAD * 2);
        contentHeight = Math.max(30, contentBottom() - contentY);

        int oldScroll = scrollOffset;
        layoutContentRows();
        maxScroll = Math.max(0, contentLogicalHeight - contentHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        if (scrollOffset != oldScroll) {
            layoutContentRows();
            maxScroll = Math.max(0, contentLogicalHeight - contentHeight);
        }
    }

    private void layoutTabs() {
        int total = 0;
        for (SettingsTab tab : SettingsTab.values()) {
            total += tabWidth(tab);
        }
        total += (SettingsTab.values().length - 1) * TAB_GAP;
        int x = panelX + Math.max(10, (panelWidth - total) / 2);
        for (SettingsTab tab : SettingsTab.values()) {
            AshwakeButton button = tabButtons.get(tab);
            int w = tabWidth(tab);
            button.setX(x);
            button.setY(tabsY);
            button.setWidth(w);
            button.setHeight(TAB_HEIGHT);
            button.visible = true;
            button.active = tab != activeTab;
            x += w + TAB_GAP;
        }
    }

    private void layoutFooter() {
        int y = footerButtonY();
        openMinecraftOptionsButton.setX(panelX + 10);
        openMinecraftOptionsButton.setY(y);
        doneButton.setX(panelX + panelWidth - doneButton.getWidth() - backButton.getWidth() - 18);
        doneButton.setY(y);
        backButton.setX(panelX + panelWidth - backButton.getWidth() - 10);
        backButton.setY(y);
    }

    private void layoutContentRows() {
        hideAllContentWidgets();
        contentText.clear();
        int y = 0;
        y = switch (activeTab) {
            case VISUAL -> layoutVisualRows(y);
            case PERFORMANCE -> layoutPerformanceRows(y);
            case LINKS -> layoutLinkRows(y);
            case ADVANCED -> layoutAdvancedRows(y);
        };
        contentLogicalHeight = y + 8;
    }

    private int layoutVisualRows(int y) {
        y = section(Component.translatable("menu.ashwake.settings.section.visual"), y);
        y = row(Component.translatable("menu.ashwake.settings.row.animations"), animationsToggle, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.animationIntensity"), animationIntensitySlider, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.particles"), particlesToggle, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.particleDensity"), particleDensitySlider, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.hoverParticles"), hoverParticlesToggle, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.hoverParticleDensity"), hoverParticleDensitySlider, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.reducedMotion"), reducedMotionToggle, y, null);
        return info(Component.translatable("menu.ashwake.settings.info.reducedMotion"), y);
    }

    private int layoutPerformanceRows(int y) {
        y = section(Component.translatable("menu.ashwake.settings.section.performance"), y);
        y = row(Component.translatable("menu.ashwake.settings.row.performancePreset"), performancePresetButton, y, null);
        y = info(Component.translatable("menu.ashwake.settings.info.performancePreset"), y);
        y = row(Component.translatable("menu.ashwake.settings.row.backgroundDarken"), backgroundDarkenSlider, y, null);
        return info(Component.translatable("menu.ashwake.settings.info.backgroundDarken"), y);
    }

    private int layoutLinkRows(int y) {
        y = section(Component.translatable("menu.ashwake.settings.section.links"), y);
        y = linkRow(
                Component.translatable("menu.ashwake.settings.discordUrl"),
                AshwakeClientConfig.discordUrl(),
                discordOpenButton,
                discordCopyButton,
                y);
        y = info(Component.translatable("menu.ashwake.settings.info.discordUrl"), y);
        y = linkRow(
                Component.translatable("menu.ashwake.settings.githubUrl"),
                AshwakeClientConfig.changelogGithubUrl(),
                githubOpenButton,
                githubCopyButton,
                y);
        y = info(Component.translatable("menu.ashwake.settings.info.githubUrl"), y);
        y = row(Component.translatable("menu.ashwake.settings.row.changelogMode"), changelogModeButton, y, null);
        return info(Component.translatable("menu.ashwake.settings.info.changelogMode"), y);
    }

    private int layoutAdvancedRows(int y) {
        y = section(Component.translatable("menu.ashwake.settings.section.advanced"), y);
        y = row(
                Component.translatable("menu.ashwake.settings.row.forceSharpUi"),
                forceSharpUiButton,
                y,
                Component.translatable("menu.ashwake.settings.forceSharp.tooltip"));
        y = row(
                Component.translatable("menu.ashwake.settings.row.blurGlobal"),
                blurCompatGlobalButton,
                y,
                Component.translatable("menu.ashwake.settings.blurGlobal.tooltip"));
        if (!AshwakeClientConfig.debugUi()) {
            return y;
        }
        if (!showDebugPage) {
            y = section(Component.translatable("menu.ashwake.settings.section.debug"), y + 8);
            return row(Component.translatable("menu.ashwake.settings.row.debugPage"), debugPageButton, y, null);
        }
        y = section(Component.translatable("menu.ashwake.settings.section.debug"), y + 8);
        y = row(Component.translatable("menu.ashwake.settings.row.allowBlurForDebug"), allowBlurForDebugButton, y, null);
        y = row(Component.translatable("menu.ashwake.settings.row.showSharpOverlay"), showSharpOverlayButton, y, null);
        y = info(Component.translatable("menu.ashwake.settings.info.debug"), y);
        return row(Component.translatable("menu.ashwake.settings.row.resetSettings"), resetSettingsButton, y, null);
    }

    private int section(Component title, int y) {
        addText(title, y + 4, AshwakePalette.LAVA_YELLOW);
        return y + HEADER_HEIGHT_LOCAL + 12;
    }

    private int row(Component label, AbstractWidget widget, int y, Component tooltip) {
        addText(label, y + 2, AshwakePalette.BONE_WHITE);
        int widgetY = contentY + y + 12 - scrollOffset;
        int widgetX = contentX + CONTENT_PAD;
        int widgetW = contentWidth - (CONTENT_PAD * 2);
        widget.setX(widgetX);
        widget.setY(widgetY);
        widget.setWidth(widgetW);
        widget.setHeight(ROW_HEIGHT);
        widget.visible = inView(widgetY, ROW_HEIGHT);
        if (!widget.visible && widget.isFocused()) {
            widget.setFocused(false);
        }
        if (tooltip != null) {
            widget.setTooltip(Tooltip.create(tooltip));
        }
        return y + 12 + ROW_HEIGHT + ROW_GAP;
    }

    private int linkRow(
            Component label,
            String url,
            AshwakeButton openButton,
            AshwakeButton copyButton,
            int y) {
        addText(label, y + 2, AshwakePalette.BONE_WHITE);

        int rowY = contentY + y + 8 - scrollOffset;
        int rowRight = contentX + contentWidth - CONTENT_PAD;
        int copyX = rowRight - LINK_BUTTON_WIDTH;
        int openX = copyX - LINK_BUTTON_WIDTH - 6;
        int buttonHeight = ROW_HEIGHT;
        int previewX = contentX + CONTENT_PAD;
        int previewY = contentY + y + 14 - scrollOffset;
        int previewWidth = Math.max(20, openX - previewX - 8);
        String clippedPreview = font.plainSubstrByWidth(urlPreview(url), previewWidth);
        if (inView(previewY, LINE_HEIGHT)) {
            contentText.add(new TextLine(Component.literal(clippedPreview).getVisualOrderText(), previewX, previewY, AshwakePalette.MUTED_TEXT));
        }

        openButton.setX(openX);
        openButton.setY(rowY);
        openButton.setWidth(LINK_BUTTON_WIDTH);
        openButton.setHeight(buttonHeight);
        openButton.visible = inView(rowY, buttonHeight);

        copyButton.setX(copyX);
        copyButton.setY(rowY);
        copyButton.setWidth(LINK_BUTTON_WIDTH);
        copyButton.setHeight(buttonHeight);
        copyButton.visible = inView(rowY, buttonHeight);

        if (!openButton.visible && openButton.isFocused()) {
            openButton.setFocused(false);
        }
        if (!copyButton.visible && copyButton.isFocused()) {
            copyButton.setFocused(false);
        }

        return y + LINK_ROW_HEIGHT + ROW_GAP;
    }

    private int info(Component text, int y) {
        int width = Math.max(80, contentWidth - (CONTENT_PAD * 2));
        List<FormattedCharSequence> lines = font.split(text, width);
        for (int i = 0; i < lines.size(); i++) {
            int ly = contentY + y + (i * LINE_HEIGHT) - scrollOffset;
            if (ly + LINE_HEIGHT >= contentY && ly <= contentY + contentHeight) {
                contentText.add(new TextLine(lines.get(i), contentX + CONTENT_PAD, ly, AshwakePalette.MUTED_TEXT));
            }
        }
        return y + (lines.size() * LINE_HEIGHT) + ROW_GAP;
    }

    private void addText(Component text, int y, int color) {
        int ly = contentY + y - scrollOffset;
        if (ly + LINE_HEIGHT >= contentY && ly <= contentY + contentHeight) {
            contentText.add(new TextLine(text.getVisualOrderText(), contentX + CONTENT_PAD, ly, color));
        }
    }

    private void hideAllContentWidgets() {
        for (AbstractWidget widget : contentWidgets) {
            widget.visible = false;
            if (widget.isFocused()) {
                widget.setFocused(false);
            }
        }
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(font, title, width / 2, 14, AshwakePalette.LAVA_YELLOW);
        Component subtitle = Component.translatable("menu.ashwake.settings.subtitle");
        List<FormattedCharSequence> lines = font.split(subtitle, panelWidth - 40);
        int y = 28;
        for (FormattedCharSequence line : lines) {
            int x = panelX + (panelWidth - font.width(line)) / 2;
            guiGraphics.drawString(font, line, x, y, AshwakePalette.MUTED_TEXT);
            y += LINE_HEIGHT;
            if (y > headerBottom() - 8) {
                break;
            }
        }
    }

    private void drawTabsBand(GuiGraphics guiGraphics) {
        guiGraphics.fill(panelX + 10, tabsY - 3, panelX + panelWidth - 10, tabsY - 2, AshwakePalette.BASALT_EDGE);
    }

    private void drawContent(GuiGraphics guiGraphics) {
        drawPanelFrame(guiGraphics, contentX, contentY, contentWidth, contentHeight, false);
        guiGraphics.enableScissor(contentX + 1, contentY + 1, contentX + contentWidth - 1, contentY + contentHeight - 1);
        for (TextLine line : contentText) {
            guiGraphics.drawString(font, line.text(), line.x(), line.y(), line.color());
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(
                    guiGraphics,
                    contentX + contentWidth - 7,
                    contentY,
                    contentY + contentHeight,
                    contentHeight,
                    contentLogicalHeight,
                    scrollOffset);
        }
    }

    private void drawFooterText(GuiGraphics guiGraphics) {
        Component hint = Component.translatable("menu.ashwake.settings.footerHint");
        int hintWidth = font.width(hint);
        int hintX = openMinecraftOptionsButton.getX() + openMinecraftOptionsButton.getWidth() + 12;
        int hintMaxX = doneButton.getX() - 12;
        if (hintX + hintWidth <= hintMaxX) {
            guiGraphics.drawString(font, hint, hintX, footerButtonY() + 7, AshwakePalette.MUTED_TEXT);
        }
    }

    private void refreshLinkButtonState(
            String url,
            AshwakeButton openButton,
            AshwakeButton copyButton,
            Component disabledTooltip) {
        boolean valid = isValidExternalUrl(url);
        boolean present = !url.isBlank();
        openButton.active = valid;
        copyButton.active = present;
        openButton.setTooltip(valid ? null : Tooltip.create(disabledTooltip));
        copyButton.setTooltip(present ? null : Tooltip.create(Component.translatable("menu.ashwake.settings.link.notSet")));
    }

    private static boolean isValidExternalUrl(String url) {
        String clean = url == null ? "" : url.trim().toLowerCase();
        return clean.startsWith("http://") || clean.startsWith("https://");
    }

    private static String urlPreview(String url) {
        String clean = url == null ? "" : url.trim();
        if (clean.isBlank()) {
            return Component.translatable("menu.ashwake.settings.link.notSet").getString();
        }
        String withoutScheme = clean.replaceFirst("^(?i)https?://", "");
        return withoutScheme.length() <= 40 ? withoutScheme : withoutScheme.substring(0, 37) + "...";
    }

    private void openUrl(String url) {
        if (!isValidExternalUrl(url)) {
            return;
        }
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception ignored) {
        }
    }

    private void copyUrl(String url) {
        String clean = url == null ? "" : url.trim();
        if (clean.isBlank() || minecraft == null || minecraft.keyboardHandler == null) {
            return;
        }
        minecraft.keyboardHandler.setClipboard(clean);
    }

    private void saveValues() {
        AshwakeClientConfig.save();
    }

    private void applyDefaults() {
        AshwakeClientConfig.applyPerformancePreset(PerformancePreset.MEDIUM);
        AshwakeClientConfig.setAnimationsEnabled(true);
        AshwakeClientConfig.setAnimationIntensity(80);
        AshwakeClientConfig.setParticlesEnabled(true);
        AshwakeClientConfig.setParticleDensity(70);
        AshwakeClientConfig.setHoverParticlesEnabled(true);
        AshwakeClientConfig.setHoverParticleDensity(70);
        AshwakeClientConfig.setBackgroundDarken(45);
        AshwakeClientConfig.setReducedMotion(false);
        AshwakeClientConfig.setDisableBlurOnAshwakeScreens(true);
        AshwakeClientConfig.setDisableMenuBlurGlobally(true);
        AshwakeClientConfig.setAllowBlurForDebug(false);
        AshwakeClientConfig.setForceSharpBackground(true);
        AshwakeClientConfig.setDebugUi(false);
        AshwakeClientConfig.setChangelogMode(ChangelogMode.LOCAL_ONLY);
        AshwakeClientConfig.setDiscordUrl("https://discord.gg/EXAMPLE");
        AshwakeClientConfig.setChangelogGithubUrl("https://github.com/Ashwake-MC/Ashwake_MainMenu");
        AshwakeClientConfig.setChangelogRemoteUrl("");
        showDebugPage = false;
    }

    private String displayPreset() {
        PerformancePreset preset = detectedPreset();
        return preset == null ? "CUSTOM" : preset.name();
    }

    private PerformancePreset nextPresetFromCurrent() {
        PerformancePreset preset = detectedPreset();
        if (preset == null) {
            return PerformancePreset.LOW;
        }
        return switch (preset) {
            case LOW -> PerformancePreset.MEDIUM;
            case MEDIUM -> PerformancePreset.HIGH;
            case HIGH -> PerformancePreset.LOW;
        };
    }

    private PerformancePreset detectedPreset() {
        for (PerformancePreset preset : PerformancePreset.values()) {
            if (matchesPreset(preset)) {
                return preset;
            }
        }
        return null;
    }

    private static boolean matchesPreset(PerformancePreset preset) {
        return switch (preset) {
            case LOW -> AshwakeClientConfig.animationsEnabled()
                    && AshwakeClientConfig.animationIntensity() == 35
                    && !AshwakeClientConfig.particlesEnabled()
                    && AshwakeClientConfig.particleDensity() == 15
                    && AshwakeClientConfig.hoverParticlesEnabled()
                    && AshwakeClientConfig.hoverParticleDensity() == 35
                    && AshwakeClientConfig.backgroundDarken() == 52
                    && AshwakeClientConfig.reducedMotion();
            case MEDIUM -> AshwakeClientConfig.animationsEnabled()
                    && AshwakeClientConfig.animationIntensity() == 70
                    && AshwakeClientConfig.particlesEnabled()
                    && AshwakeClientConfig.particleDensity() == 55
                    && AshwakeClientConfig.hoverParticlesEnabled()
                    && AshwakeClientConfig.hoverParticleDensity() == 70
                    && AshwakeClientConfig.backgroundDarken() == 45
                    && !AshwakeClientConfig.reducedMotion();
            case HIGH -> AshwakeClientConfig.animationsEnabled()
                    && AshwakeClientConfig.animationIntensity() == 100
                    && AshwakeClientConfig.particlesEnabled()
                    && AshwakeClientConfig.particleDensity() == 90
                    && AshwakeClientConfig.hoverParticlesEnabled()
                    && AshwakeClientConfig.hoverParticleDensity() == 90
                    && AshwakeClientConfig.backgroundDarken() == 40
                    && !AshwakeClientConfig.reducedMotion();
        };
    }

    private boolean isInScrollableContent(double mouseX, double mouseY) {
        return mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= contentY && mouseY <= contentY + contentHeight;
    }

    private boolean inView(int y, int h) {
        return y + h >= contentY && y <= contentY + contentHeight;
    }

    private int tabWidth(SettingsTab tab) {
        return Math.max(96, font.width(tab.title()) + 20);
    }

    private static Component onOff(boolean value) {
        return value
                ? Component.translatable("menu.ashwake.settings.value.on")
                : Component.translatable("menu.ashwake.settings.value.off");
    }

    private <T extends AbstractWidget> T addContent(T widget) {
        addRenderableWidget(widget);
        contentWidgets.add(widget);
        return widget;
    }

    private record TextLine(FormattedCharSequence text, int x, int y, int color) {
    }

    private enum SettingsTab {
        VISUAL("menu.ashwake.settings.tab.visual"),
        PERFORMANCE("menu.ashwake.settings.tab.performance"),
        LINKS("menu.ashwake.settings.tab.links"),
        ADVANCED("menu.ashwake.settings.tab.advanced");

        private final String key;

        SettingsTab(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable(key);
        }
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final Runnable onChange;
        private boolean syncing;

        private IntSlider(
                int x,
                int y,
                int width,
                int height,
                int min,
                int max,
                IntSupplier getter,
                IntConsumer setter,
                Runnable onChange) {
            super(x, y, width, height, Component.empty(), 0.0D);
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.onChange = onChange;
            sync();
        }

        void sync() {
            syncing = true;
            value = encode(getter.getAsInt());
            updateMessage();
            syncing = false;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Integer.toString(decode(value))));
        }

        @Override
        protected void applyValue() {
            if (syncing) {
                return;
            }
            setter.accept(decode(value));
            onChange.run();
        }

        private double encode(int raw) {
            return (Mth.clamp(raw, min, max) - min) / (double) (max - min);
        }

        private int decode(double val) {
            return Mth.clamp((int) Math.round(min + (val * (max - min))), min, max);
        }
    }
}
