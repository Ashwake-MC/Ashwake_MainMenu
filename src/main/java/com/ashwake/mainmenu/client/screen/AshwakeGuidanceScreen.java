package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.api.GuidanceCard;
import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.ashwake.mainmenu.internal.AshwakeMenuApiImpl;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class AshwakeGuidanceScreen extends AshwakeScreenBase {
    private static final int CARD_HEIGHT = 46;
    private static final int CARD_GAP = 8;
    private static final List<String> CONTROLS_CLASS_CANDIDATES = List.of(
            "net.minecraft.client.gui.screens.options.controls.ControlsScreen",
            "net.minecraft.client.gui.screens.controls.ControlsScreen");
    private static final List<String> KEYBINDS_CLASS_CANDIDATES = List.of(
            "net.minecraft.client.gui.screens.options.controls.KeyBindsScreen",
            "net.minecraft.client.gui.screens.controls.KeyBindsScreen");
    private static final Set<String> LOGGED_CONTROLS_FALLBACK_FAILURES = ConcurrentHashMap.newKeySet();

    private final Screen parent;
    private final List<GuidanceCard> cards = new ArrayList<>();
    private final List<AshwakeButton> cardButtons = new ArrayList<>();

    private int panelX;
    private int panelWidth;
    private int scrollOffset;
    private int maxScroll;

    public AshwakeGuidanceScreen(Screen parent) {
        super(Component.translatable("menu.ashwake.guidance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = safePanelWidth(680);
        panelX = safePanelX(680);
        scrollOffset = 0;
        maxScroll = 0;

        cards.clear();
        cards.addAll(defaultCards());
        cards.addAll(AshwakeMenuApiImpl.getInstance().getGuidanceCards());
        cards.sort(Comparator.comparingInt(GuidanceCard::sortOrder).thenComparing(GuidanceCard::id));

        cardButtons.clear();
        int buttonWidth = panelWidth - (PANEL_PADDING * 2);
        int buttonX = panelX + PANEL_PADDING;
        for (GuidanceCard card : cards) {
            AshwakeButton button = addRenderableWidget(new AshwakeButton(
                    buttonX,
                    0,
                    buttonWidth,
                    CARD_HEIGHT,
                    Component.translatable(card.titleKey()),
                    AshwakeButton.Icon.GUIDANCE,
                    b -> runCardAction(card)));
            if ("ashwake:controls".equals(card.id())) {
                button.setTooltip(Tooltip.create(Component.translatable("menu.ashwake.guidance.controls.tooltip")));
            }
            cardButtons.add(button);
        }

        addRenderableWidget(new AshwakeButton(
                panelX + panelWidth - 122,
                footerButtonY(),
                110,
                24,
                Component.translatable("gui.back"),
                button -> minecraft.setScreen(parent)));

        relayout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInContentArea(mouseX, mouseY) || maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 16), 0, maxScroll);
        relayout();
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);

        int logoSize = AshwakeBranding.drawLeftLogo(guiGraphics, font, panelX + 10, 11, 34, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(font, title, panelX + logoSize + 16, 18, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                font,
                Component.translatable("menu.ashwake.guidance.subtitle"),
                panelX + logoSize + 16,
                31,
                AshwakePalette.MUTED_TEXT);

        drawCardDescriptions(guiGraphics);
        if (maxScroll > 0) {
            drawScrollbar(
                    guiGraphics,
                    panelX + panelWidth - 7,
                    contentTop(),
                    contentBottom(),
                    contentHeight(),
                    totalCardHeight() + CONTENT_BOTTOM_SAFE_PADDING,
                    scrollOffset);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void relayout() {
        int buttonX = panelX + PANEL_PADDING;
        int buttonWidth = panelWidth - (PANEL_PADDING * 2);
        int requiredHeight = totalCardHeight();
        maxScroll = Math.max(0, (requiredHeight + CONTENT_BOTTOM_SAFE_PADDING) - contentHeight());
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        int startY = contentTop() + 6 - scrollOffset;

        for (int i = 0; i < cardButtons.size(); i++) {
            AshwakeButton button = cardButtons.get(i);
            int y = startY + i * (CARD_HEIGHT + CARD_GAP);
            button.setX(buttonX);
            button.setY(y);
            button.setWidth(buttonWidth);
            button.setHeight(CARD_HEIGHT);
            button.visible = y + CARD_HEIGHT >= contentTop() && y <= contentBottom();
        }
    }

    private int totalCardHeight() {
        return cards.size() * CARD_HEIGHT + Math.max(0, cards.size() - 1) * CARD_GAP;
    }

    private void drawCardDescriptions(GuiGraphics guiGraphics) {
        for (int i = 0; i < cards.size() && i < cardButtons.size(); i++) {
            AshwakeButton button = cardButtons.get(i);
            if (!button.visible) {
                continue;
            }
            GuidanceCard card = cards.get(i);
            guiGraphics.drawString(
                    font,
                    Component.translatable(card.bodyKey()),
                    button.getX() + 26,
                    button.getY() + 27,
                    AshwakePalette.MUTED_TEXT);
        }
    }

    private void runCardAction(GuidanceCard card) {
        GuidanceCard.CardAction action = card.action();
        try {
            if (action instanceof GuidanceCard.OpenUrl openUrl) {
                Util.getPlatform().openUri(openUrl.url());
                return;
            }
            if (action instanceof GuidanceCard.OpenScreen openScreen) {
                openCardScreen(openScreen.screenId());
                return;
            }
            if (action instanceof GuidanceCard.OpenMinecraftScreen openMinecraftScreen) {
                Screen screen = openMinecraftScreen.screenFactory().apply(this);
                if (screen != null) {
                    minecraft.setScreen(screen);
                }
                return;
            }
            if (action instanceof GuidanceCard.RunTask runTask) {
                AshwakeMenuApiImpl.getInstance().invokeExternalCallback("guidance:" + card.id(), runTask.task());
            }
        } catch (Exception exception) {
            AshwakeMainMenuMod.LOGGER.warn("Failed handling guidance card action {}", card.id(), exception);
        }
    }

    private void openCardScreen(String screenId) {
        String normalized = screenId == null ? "" : screenId.trim().toLowerCase();
        switch (normalized) {
            case "changelog" -> minecraft.setScreen(new AshwakeChangelogScreen(this));
            case "play", "playhub" -> minecraft.setScreen(new AshwakePlayHubScreen(this));
            case "settings" -> minecraft.setScreen(new AshwakeSettingsScreen(this));
            case "faq" -> minecraft.setScreen(new AshwakeFaqScreen(this));
            case "controls" -> minecraft.setScreen(createControlsScreenWithFallback(this));
            default -> {
            }
        }
    }

    private Screen createControlsScreenWithFallback(Screen parent) {
        Screen target = tryCreateVanillaOptionsChild(CONTROLS_CLASS_CANDIDATES, "controls", parent);
        if (target == null) {
            target = tryCreateVanillaOptionsChild(KEYBINDS_CLASS_CANDIDATES, "keybinds", parent);
        }
        if (target == null) {
            target = new OptionsScreen(parent, minecraft.options);
        }
        return target;
    }

    private Screen tryCreateVanillaOptionsChild(List<String> classCandidates, String logKeyPrefix, Screen parent) {
        if (minecraft == null || minecraft.options == null) {
            return null;
        }

        for (String className : classCandidates) {
            try {
                Class<?> raw = Class.forName(className);
                for (Constructor<?> ctor : raw.getConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length != 2) {
                        continue;
                    }
                    if (!Screen.class.isAssignableFrom(params[0])) {
                        continue;
                    }
                    if (!params[1].isAssignableFrom(minecraft.options.getClass())) {
                        continue;
                    }
                    Object created = ctor.newInstance(parent, minecraft.options);
                    if (created instanceof Screen screen) {
                        return screen;
                    }
                }
                logControlsFallbackOnce(logKeyPrefix + ":ctor_missing", null, className);
            } catch (Throwable throwable) {
                logControlsFallbackOnce(logKeyPrefix + ":class_fail", throwable, className);
            }
        }
        return null;
    }

    private static void logControlsFallbackOnce(String key, Throwable throwable, String className) {
        if (!LOGGED_CONTROLS_FALLBACK_FAILURES.add(key)) {
            return;
        }
        if (throwable == null) {
            AshwakeMainMenuMod.LOGGER.warn("Guidance controls fallback: constructor not found for {}", className);
            return;
        }
        AshwakeMainMenuMod.LOGGER.warn("Guidance controls fallback: failed loading {}", className, throwable);
    }

    private static List<GuidanceCard> defaultCards() {
        return List.of(
                new GuidanceCard(
                        "ashwake:getting_started",
                        "menu.ashwake.guidance.card.getting_started",
                        "menu.ashwake.guidance.body.getting_started",
                        "",
                        10,
                        new GuidanceCard.OpenScreen("playhub")),
                new GuidanceCard(
                        "ashwake:performance",
                        "menu.ashwake.guidance.card.performance",
                        "menu.ashwake.guidance.body.performance",
                        "",
                        20,
                        new GuidanceCard.OpenScreen("settings")),
                new GuidanceCard(
                        "ashwake:bug_report",
                        "menu.ashwake.guidance.card.bug_report",
                        "menu.ashwake.guidance.body.bug_report",
                        "",
                        30,
                        new GuidanceCard.OpenScreen("changelog")),
                new GuidanceCard(
                        "ashwake:faq",
                        "menu.ashwake.guidance.card.faq",
                        "menu.ashwake.guidance.body.faq",
                        "",
                        40,
                        new GuidanceCard.OpenScreen("faq")),
                new GuidanceCard(
                        "ashwake:controls",
                        "menu.ashwake.guidance.card.controls",
                        "menu.ashwake.guidance.body.controls",
                        "",
                        50,
                        new GuidanceCard.OpenScreen("controls")));
    }
}

