package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class AshwakeCreateWorldScreen extends AshwakeScreenBase {
    private static final int PANEL_MAX_WIDTH = 1120;
    private static final int PANEL_GAP = 12;
    private static final int TAB_BUTTON_HEIGHT = 24;
    private static final int TAB_BUTTON_GAP = 8;
    private static final int SMALL_SCREEN_BREAKPOINT = 860;
    private static final int ROW_GAP = 8;
    private static final int ROW_HEIGHT_MIN = 30;
    private static final int TEXTFIELD_ROW_HEIGHT = 56;
    private static final int SPLIT_ROW_STACK_BREAKPOINT = 400;
    private static final int SPLIT_ROW_GAP = 10;
    private static final int STACKED_ROW_GAP = 6;
    private static final long TIP_ROTATE_MS = 8000L;

    private static final List<Component> TIPS = List.of(
            Component.translatable("menu.ashwake.createworld.tip.seed"),
            Component.translatable("menu.ashwake.createworld.tip.commands"),
            Component.translatable("menu.ashwake.createworld.tip.preset"),
            Component.translatable("menu.ashwake.createworld.tip.guidance"));

    private final CreateWorldScreen vanilla;
    private final EnumMap<Step, List<AbstractWidget>> stepWidgets = new EnumMap<>(Step.class);
    private final EnumMap<Step, List<WidgetRow>> stepRows = new EnumMap<>(Step.class);
    private final EnumMap<Step, Integer> stepScroll = new EnumMap<>(Step.class);
    private final IdentityHashMap<AbstractWidget, WidgetSize> originalWidgetSizes = new IdentityHashMap<>();
    private final List<AbstractWidget> mountedVanillaWidgets = new ArrayList<>();
    private final IdentityHashMap<AbstractWidget, Boolean> mountedLookup = new IdentityHashMap<>();

    private final EnumMap<Step, AshwakeButton> stepButtons = new EnumMap<>(Step.class);
    private AshwakeButton helpButton;
    private AshwakeButton guidanceButton;
    private AshwakeButton backButton;
    private AshwakeButton createButton;
    private AshwakeButton moreOptionsButton;

    private TabNavigationBar vanillaTabBar;
    private TabManager vanillaTabManager;
    private Button vanillaCreateButton;
    private Button vanillaCancelButton;

    private Step activeStep = Step.BASICS;

    private int panelX;
    private int panelWidth;
    private int contentLeft;
    private int contentTopY;
    private int contentHeightValue;
    private int leftPanelX;
    private int leftPanelY;
    private int leftPanelWidth;
    private int leftPanelHeight;
    private int rightPanelX;
    private int rightPanelY;
    private int rightPanelWidth;
    private int rightPanelHeight;
    private int controlsViewportX;
    private int controlsViewportY;
    private int controlsViewportWidth;
    private int controlsViewportHeight;
    private int controlsContentHeight;
    private int scrollOffset;
    private int maxScroll;
    private boolean stackedLayout;

    private int tipIndex;
    private long lastTipSwapMs;

    public AshwakeCreateWorldScreen(CreateWorldScreen vanilla) {
        super(Component.translatable("selectWorld.create"));
        this.vanilla = vanilla;
    }

    @Override
    protected void init() {
        stepWidgets.clear();
        stepRows.clear();
        stepButtons.clear();
        stepScroll.clear();
        originalWidgetSizes.clear();
        mountedVanillaWidgets.clear();
        mountedLookup.clear();
        vanillaCreateButton = null;
        vanillaCancelButton = null;
        vanillaTabBar = null;
        vanillaTabManager = null;
        activeStep = Step.BASICS;
        for (Step step : Step.values()) {
            stepScroll.put(step, 0);
        }
        captureVanillaWidgets();
        buildAshwakeControls();
        tipIndex = 0;
        lastTipSwapMs = Util.getMillis();
        relayout();
    }

    @Override
    public void tick() {
        super.tick();
        vanilla.tick();

        if (!TIPS.isEmpty()) {
            long now = Util.getMillis();
            if (now - lastTipSwapMs >= TIP_ROTATE_MS) {
                tipIndex = (tipIndex + 1) % TIPS.size();
                lastTipSwapMs = now;
            }
        }
    }

    @Override
    public void repositionElements() {
        vanilla.init(minecraft, width, height);
        relayout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInControlsViewport(mouseX, mouseY) || maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 18), 0, maxScroll);
        stepScroll.put(activeStep, scrollOffset);
        relayout();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.hasControlDown() && keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_3) {
            int index = keyCode - InputConstants.KEY_1;
            Step target = Step.fromIndex(index);
            if (target != null) {
                setActiveStep(target);
                return true;
            }
        }

        if (maxScroll > 0) {
            if (keyCode == InputConstants.KEY_HOME) {
                scrollOffset = 0;
                stepScroll.put(activeStep, scrollOffset);
                relayout();
                return true;
            }
            if (keyCode == InputConstants.KEY_END) {
                scrollOffset = maxScroll;
                stepScroll.put(activeStep, scrollOffset);
                relayout();
                return true;
            }
        }

        if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && createButton.active) {
            triggerCreate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        vanilla.popScreen();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateVanillaWidgetLayout();
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);
        drawHeader(guiGraphics);
        drawContentPanels(guiGraphics);
        drawRightPanelContents(guiGraphics);
        drawFooterHint(guiGraphics);

        guiGraphics.enableScissor(
                controlsViewportX,
                controlsViewportY,
                controlsViewportX + controlsViewportWidth,
                controlsViewportY + controlsViewportHeight);
        for (Renderable renderable : renderables) {
            if (isMountedVanillaWidget(renderable)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        guiGraphics.disableScissor();

        for (Renderable renderable : renderables) {
            if (!isMountedVanillaWidget(renderable)) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void captureVanillaWidgets() {
        vanilla.init(minecraft, width, height);

        vanillaTabBar = readField(vanilla, "tabNavigationBar", TabNavigationBar.class);
        vanillaTabManager = readField(vanilla, "tabManager", TabManager.class);

        List<Button> footerButtons = discoverFooterButtons();
        if (!footerButtons.isEmpty()) {
            vanillaCreateButton = footerButtons.getFirst();
            if (footerButtons.size() > 1) {
                vanillaCancelButton = footerButtons.get(1);
            }
        }

        for (Step step : Step.values()) {
            selectVanillaStep(step);
            List<AbstractWidget> captured = discoverStepWidgets();
            stepWidgets.put(step, captured);
            stepRows.put(step, buildRows(captured));
            for (AbstractWidget widget : captured) {
                originalWidgetSizes.putIfAbsent(widget, new WidgetSize(widget.getWidth(), widget.getHeight()));
                if (!mountedVanillaWidgets.contains(widget)) {
                    mountedVanillaWidgets.add(addRenderableWidget(widget));
                    mountedLookup.put(widget, true);
                }
            }
        }

        for (AbstractWidget widget : mountedVanillaWidgets) {
            widget.visible = false;
            widget.active = false;
            widget.setAlpha(1.0F);
        }
    }

    private void buildAshwakeControls() {
        for (Step step : Step.values()) {
            AshwakeButton button = addRenderableWidget(new AshwakeButton(
                    0,
                    0,
                    110,
                    TAB_BUTTON_HEIGHT,
                    step.title(),
                    AshwakeButton.Icon.NONE,
                    b -> setActiveStep(step)));
            stepButtons.put(step, button);
        }

        helpButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                28,
                24,
                Component.literal("?"),
                AshwakeButton.Icon.NONE,
                b -> openGuidance()));

        guidanceButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                200,
                24,
                Component.translatable("menu.ashwake.createworld.guidanceCta"),
                AshwakeButton.Icon.GUIDANCE,
                b -> openGuidance()));

        backButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                110,
                24,
                Component.translatable("gui.back"),
                AshwakeButton.Icon.NONE,
                b -> vanilla.popScreen()));

        moreOptionsButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                122,
                24,
                Component.translatable("menu.ashwake.createworld.moreOptions"),
                AshwakeButton.Icon.NONE,
                b -> setActiveStep(Step.MORE)));

        createButton = addRenderableWidget(new AshwakeButton(
                0,
                0,
                146,
                24,
                Component.translatable("selectWorld.create"),
                AshwakeButton.Icon.PLAY,
                b -> triggerCreate()));
    }

    private void relayout() {
        panelWidth = safePanelWidth(PANEL_MAX_WIDTH);
        panelX = safePanelX(PANEL_MAX_WIDTH);
        contentLeft = panelX + PANEL_PADDING;
        contentTopY = contentTop();
        contentHeightValue = contentHeight();

        int usableWidth = panelWidth - (PANEL_PADDING * 2);
        stackedLayout = usableWidth < SMALL_SCREEN_BREAKPOINT;
        if (stackedLayout) {
            leftPanelWidth = usableWidth;
            rightPanelWidth = usableWidth;
            leftPanelHeight = Math.max(220, (int) (contentHeightValue * 0.60F));
            rightPanelHeight = Math.max(170, contentHeightValue - leftPanelHeight - PANEL_GAP);
            if (rightPanelHeight < 150) {
                rightPanelHeight = 150;
                leftPanelHeight = Math.max(180, contentHeightValue - rightPanelHeight - PANEL_GAP);
            }
            leftPanelX = contentLeft;
            leftPanelY = contentTopY;
            rightPanelX = contentLeft;
            rightPanelY = leftPanelY + leftPanelHeight + PANEL_GAP;
        } else {
            leftPanelWidth = Mth.clamp((int) (usableWidth * 0.62F), 360, 700);
            rightPanelWidth = usableWidth - leftPanelWidth - PANEL_GAP;
            leftPanelHeight = contentHeightValue;
            rightPanelHeight = contentHeightValue;
            leftPanelX = contentLeft;
            leftPanelY = contentTopY;
            rightPanelX = leftPanelX + leftPanelWidth + PANEL_GAP;
            rightPanelY = contentTopY;
        }

        controlsViewportX = leftPanelX + PANEL_PADDING;
        controlsViewportY = leftPanelY + 52;
        controlsViewportWidth = leftPanelWidth - (PANEL_PADDING * 2) - 8;
        controlsViewportHeight = Math.max(52, leftPanelHeight - 64);

        layoutHeaderButtons();
        layoutFooterButtons();
        layoutRightPanelButtons();
        updateVanillaWidgetLayout();
        updateCreateButtonState();
    }

    private void layoutHeaderButtons() {
        int totalWidth = 0;
        for (Step step : Step.values()) {
            totalWidth += stepButtons.get(step).getWidth();
        }
        totalWidth += (Step.values().length - 1) * TAB_BUTTON_GAP;

        int x = panelX + Math.max(10, (panelWidth - totalWidth) / 2);
        int y = 30;
        for (Step step : Step.values()) {
            AshwakeButton button = stepButtons.get(step);
            button.setX(x);
            button.setY(y);
            button.setHeight(TAB_BUTTON_HEIGHT);
            button.active = step != activeStep;
            x += button.getWidth() + TAB_BUTTON_GAP;
        }

        helpButton.setX(panelX + panelWidth - helpButton.getWidth() - 12);
        helpButton.setY(14);
    }

    private void layoutFooterButtons() {
        int y = footerButtonY();
        backButton.setX(panelX + 10);
        backButton.setY(y);

        createButton.setX(panelX + panelWidth - createButton.getWidth() - 10);
        createButton.setY(y);

        moreOptionsButton.setX(createButton.getX() - moreOptionsButton.getWidth() - 8);
        moreOptionsButton.setY(y);
    }

    private void layoutRightPanelButtons() {
        guidanceButton.setX(rightPanelX + PANEL_PADDING);
        guidanceButton.setWidth(Math.max(130, rightPanelWidth - (PANEL_PADDING * 2)));
        guidanceButton.setY(rightPanelY + rightPanelHeight - guidanceButton.getHeight() - PANEL_PADDING);
    }

    private void updateVanillaWidgetLayout() {
        for (Step step : Step.values()) {
            if (step == activeStep) {
                continue;
            }
            for (AbstractWidget widget : stepWidgets.getOrDefault(step, List.of())) {
                widget.visible = false;
                widget.active = false;
                if (widget.isFocused()) {
                    widget.setFocused(false);
                }
            }
        }

        controlsContentHeight = layoutStepWidgets(activeStep, scrollOffset, true);
        maxScroll = Math.max(0, controlsContentHeight - controlsViewportHeight);
        int clampedScroll = Mth.clamp(scrollOffset, 0, maxScroll);
        if (clampedScroll != scrollOffset) {
            scrollOffset = clampedScroll;
            controlsContentHeight = layoutStepWidgets(activeStep, scrollOffset, true);
        }
        stepScroll.put(activeStep, scrollOffset);
    }

    private int layoutStepWidgets(Step step, int stepScrollValue, boolean interactive) {
        List<AbstractWidget> widgets = stepWidgets.getOrDefault(step, List.of());
        if (widgets.isEmpty()) {
            return 0;
        }

        List<WidgetRow> rows = stepRows.getOrDefault(step, List.of());
        if (rows.isEmpty()) {
            return 0;
        }

        for (AbstractWidget widget : widgets) {
            WidgetSize original = originalWidgetSizes.get(widget);
            if (original != null) {
                widget.setWidth(original.width());
                widget.setHeight(original.height());
            }
        }

        applyTabArea(step);

        int logicalY = 0;
        for (WidgetRow row : rows) {
            int rowHeight = effectiveRowHeight(row);
            logicalY = layoutRowWidgets(row, logicalY, rowHeight, stepScrollValue, interactive);
            logicalY += ROW_GAP;
        }

        return Math.max(0, logicalY + CONTENT_BOTTOM_PADDING);
    }

    private void applyTabArea(Step step) {
        selectVanillaStep(step);
        if (vanillaTabManager != null) {
            vanillaTabManager.setTabArea(new ScreenRectangle(
                    controlsViewportX,
                    controlsViewportY,
                    Math.max(60, controlsViewportWidth),
                    Math.max(80, controlsViewportHeight)));
        }
    }

    private int effectiveRowHeight(WidgetRow row) {
        if (row.widgets().size() == 2 && controlsViewportWidth < SPLIT_ROW_STACK_BREAKPOINT) {
            int h0 = originalWidgetSizes.getOrDefault(row.widgets().get(0), new WidgetSize(row.widgets().get(0).getWidth(), row.widgets().get(0).getHeight())).height();
            int h1 = originalWidgetSizes.getOrDefault(row.widgets().get(1), new WidgetSize(row.widgets().get(1).getWidth(), row.widgets().get(1).getHeight())).height();
            return Math.max(row.height(), h0 + h1 + STACKED_ROW_GAP);
        }
        return row.height();
    }

    private int layoutRowWidgets(WidgetRow row, int logicalY, int rowHeight, int stepScrollValue, boolean interactive) {
        List<AbstractWidget> widgets = row.widgets();
        if (widgets.isEmpty()) {
            return logicalY + rowHeight;
        }

        int rowTop = controlsViewportY + logicalY - stepScrollValue;
        int xLeft = controlsViewportX;
        int rowWidth = controlsViewportWidth;

        if (row.kind() == RowKind.TEXT_FIELD && widgets.size() == 1 && widgets.getFirst() instanceof EditBox) {
            AbstractWidget widget = widgets.getFirst();
            WidgetSize original = originalWidgetSizes.getOrDefault(widget, new WidgetSize(widget.getWidth(), widget.getHeight()));
            widget.setWidth(rowWidth);
            widget.setHeight(original.height());
            widget.setX(xLeft);
            widget.setY(rowTop + 24);
            applyWidgetVisibility(widget, interactive);
            return logicalY + rowHeight;
        }

        if (widgets.size() == 1) {
            AbstractWidget widget = widgets.getFirst();
            WidgetSize original = originalWidgetSizes.getOrDefault(widget, new WidgetSize(widget.getWidth(), widget.getHeight()));
            int widgetHeight = original.height();
            widget.setWidth(rowWidth);
            widget.setHeight(widgetHeight);
            widget.setX(xLeft);
            widget.setY(rowTop + Math.max(0, (rowHeight - widgetHeight) / 2));
            applyWidgetVisibility(widget, interactive);
            return logicalY + rowHeight;
        }

        if (widgets.size() == 2) {
            AbstractWidget first = widgets.get(0);
            AbstractWidget second = widgets.get(1);
            WidgetSize firstSize = originalWidgetSizes.getOrDefault(first, new WidgetSize(first.getWidth(), first.getHeight()));
            WidgetSize secondSize = originalWidgetSizes.getOrDefault(second, new WidgetSize(second.getWidth(), second.getHeight()));

            if (controlsViewportWidth < SPLIT_ROW_STACK_BREAKPOINT) {
                int firstY = rowTop;
                int secondY = rowTop + firstSize.height() + STACKED_ROW_GAP;
                first.setWidth(rowWidth);
                second.setWidth(rowWidth);
                first.setHeight(firstSize.height());
                second.setHeight(secondSize.height());
                first.setX(xLeft);
                second.setX(xLeft);
                first.setY(firstY);
                second.setY(secondY);
            } else {
                int total = Math.max(1, firstSize.width() + secondSize.width());
                int contentWidth = rowWidth - SPLIT_ROW_GAP;
                int firstWidth = Mth.clamp(Math.round(contentWidth * (firstSize.width() / (float) total)), 90, contentWidth - 90);
                int secondWidth = Math.max(90, contentWidth - firstWidth);
                int firstY = rowTop + Math.max(0, (rowHeight - firstSize.height()) / 2);
                int secondY = rowTop + Math.max(0, (rowHeight - secondSize.height()) / 2);

                first.setWidth(firstWidth);
                second.setWidth(secondWidth);
                first.setHeight(firstSize.height());
                second.setHeight(secondSize.height());
                first.setX(xLeft);
                second.setX(xLeft + firstWidth + SPLIT_ROW_GAP);
                first.setY(firstY);
                second.setY(secondY);
            }

            applyWidgetVisibility(first, interactive);
            applyWidgetVisibility(second, interactive);
            return logicalY + rowHeight;
        }

        int minX = row.minX();
        int maxX = Math.max(minX + 1, row.maxX());
        int span = maxX - minX;
        for (AbstractWidget widget : widgets) {
            WidgetSize size = originalWidgetSizes.getOrDefault(widget, new WidgetSize(widget.getWidth(), widget.getHeight()));
            int relX = widget.getX() - minX;
            int relW = Math.max(20, widget.getWidth());
            int x = xLeft + Math.round((relX / (float) span) * rowWidth);
            int w = Math.max(22, Math.round((relW / (float) span) * rowWidth));
            int y = rowTop + Math.max(0, (rowHeight - size.height()) / 2);
            widget.setX(x);
            widget.setY(y);
            widget.setWidth(w);
            widget.setHeight(size.height());
            applyWidgetVisibility(widget, interactive);
        }

        return logicalY + rowHeight;
    }

    private void applyWidgetVisibility(AbstractWidget widget, boolean interactive) {
        widget.setAlpha(1.0F);
        boolean visibleInViewport = widget.getY() + widget.getHeight() > controlsViewportY
                && widget.getY() < controlsViewportY + controlsViewportHeight
                && widget.getX() + widget.getWidth() > controlsViewportX
                && widget.getX() < controlsViewportX + controlsViewportWidth;
        widget.visible = visibleInViewport;
        widget.active = interactive && visibleInViewport;
        if (!visibleInViewport && widget.isFocused()) {
            widget.setFocused(false);
        }
    }

    private List<WidgetRow> buildRows(List<AbstractWidget> widgets) {
        if (widgets.isEmpty()) {
            return List.of();
        }

        List<AbstractWidget> sorted = new ArrayList<>(widgets);
        sorted.sort(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));

        List<RowBuild> builds = new ArrayList<>();
        for (AbstractWidget widget : sorted) {
            RowBuild target = null;
            for (RowBuild candidate : builds) {
                if (Math.abs(candidate.baseY - widget.getY()) <= 4) {
                    target = candidate;
                    break;
                }
            }
            if (target == null) {
                target = new RowBuild(widget.getY());
                builds.add(target);
            }
            target.widgets.add(widget);
            target.minX = Math.min(target.minX, widget.getX());
            target.maxX = Math.max(target.maxX, widget.getX() + widget.getWidth());
            target.maxHeight = Math.max(target.maxHeight, widget.getHeight());
            if (widget instanceof EditBox) {
                target.hasTextField = true;
            }
        }

        builds.sort(Comparator.comparingInt(b -> b.baseY));
        List<WidgetRow> rows = new ArrayList<>(builds.size());
        for (RowBuild build : builds) {
            build.widgets.sort(Comparator.comparingInt(AbstractWidget::getX));
            RowKind kind = build.hasTextField ? RowKind.TEXT_FIELD : RowKind.STANDARD;
            int rowHeight = build.hasTextField
                    ? TEXTFIELD_ROW_HEIGHT
                    : Math.max(ROW_HEIGHT_MIN, build.maxHeight + 8);
            rows.add(new WidgetRow(List.copyOf(build.widgets), build.minX, build.maxX, kind, rowHeight));
        }
        return rows;
    }

    private void setActiveStep(Step target) {
        if (target == activeStep) {
            return;
        }

        stepScroll.put(activeStep, scrollOffset);
        activeStep = target;
        scrollOffset = stepScroll.getOrDefault(activeStep, 0);
        selectVanillaStep(activeStep);
        relayout();
    }

    private void updateCreateButtonState() {
        boolean createActive = vanillaCreateButton == null || vanillaCreateButton.active;
        createButton.active = createActive;
    }

    private void triggerCreate() {
        if (vanillaCreateButton != null) {
            vanillaCreateButton.onPress();
            return;
        }
        invokeNoArg(vanilla, "onCreate");
    }

    private void openGuidance() {
        minecraft.setScreen(new AshwakeGuidanceScreen(this));
    }

    private void selectVanillaStep(Step step) {
        if (vanillaTabBar != null) {
            vanillaTabBar.selectTab(step.index(), false);
        }
    }

    private List<Button> discoverFooterButtons() {
        List<Button> buttons = new ArrayList<>();
        int footerThreshold = height - 42;
        for (var renderable : vanilla.renderables) {
            if (renderable instanceof Button button && button.getY() >= footerThreshold) {
                buttons.add(button);
            }
        }
        buttons.sort(Comparator.comparingInt(AbstractWidget::getX));
        return buttons;
    }

    private List<AbstractWidget> discoverStepWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>();
        for (var renderable : vanilla.renderables) {
            if (!(renderable instanceof AbstractWidget widget)) {
                continue;
            }
            if (widget == vanillaCreateButton || widget == vanillaCancelButton) {
                continue;
            }
            widgets.add(widget);
        }
        return widgets;
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        int logoSize = AshwakeBranding.drawLeftLogo(guiGraphics, font, panelX + 10, 12, 26, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                font,
                Component.translatable("menu.ashwake.createworld.title"),
                panelX + logoSize + 16,
                18,
                AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                font,
                Component.translatable("menu.ashwake.createworld.subtitle"),
                panelX + logoSize + 16,
                30,
                AshwakePalette.MUTED_TEXT);

        AshwakeButton activeButton = stepButtons.get(activeStep);
        if (activeButton != null) {
            guiGraphics.fill(
                    activeButton.getX() - 2,
                    activeButton.getY() - 2,
                    activeButton.getX() + activeButton.getWidth() + 2,
                    activeButton.getY() + activeButton.getHeight() + 2,
                    0x1EF08A2B);
        }
    }

    private void drawContentPanels(GuiGraphics guiGraphics) {
        drawPanelFrame(guiGraphics, leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight, false);
        drawPanelFrame(guiGraphics, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight, false);

        guiGraphics.drawString(font, activeStep.header(), leftPanelX + PANEL_PADDING, leftPanelY + 14, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                font,
                Component.translatable("menu.ashwake.createworld.controls"),
                leftPanelX + PANEL_PADDING,
                leftPanelY + 28,
                AshwakePalette.MUTED_TEXT);

        if (maxScroll > 0) {
            drawScrollbar(
                    guiGraphics,
                    controlsViewportX + controlsViewportWidth + 2,
                    controlsViewportY,
                    controlsViewportY + controlsViewportHeight,
                    controlsViewportHeight,
                    controlsContentHeight,
                    scrollOffset);
        }
    }

    private void drawRightPanelContents(GuiGraphics guiGraphics) {
        int x = rightPanelX + PANEL_PADDING;
        int y = rightPanelY + PANEL_PADDING;
        int width = rightPanelWidth - (PANEL_PADDING * 2);

        guiGraphics.drawString(font, Component.translatable("menu.ashwake.createworld.preview"), x, y, AshwakePalette.LAVA_YELLOW);
        int previewY = y + 14;
        int previewH = 72;
        drawPanelFrame(guiGraphics, x, previewY, width, previewH, false);
        guiGraphics.fill(x + 2, previewY + 2, x + width - 2, previewY + previewH - 2, 0x64110F0D);

        List<FormattedCharSequence> presetLines = font.split(currentPresetLine(), Math.max(80, width - 12));
        int lineY = previewY + 10;
        for (FormattedCharSequence line : presetLines) {
            guiGraphics.drawString(font, line, x + 6, lineY, AshwakePalette.BONE_WHITE);
            lineY += 10;
            if (lineY > previewY + previewH - 12) {
                break;
            }
        }

        List<FormattedCharSequence> summaryLines = font.split(currentSummaryLine(), Math.max(80, width - 12));
        for (FormattedCharSequence line : summaryLines) {
            guiGraphics.drawString(font, line, x + 6, lineY, AshwakePalette.MUTED_TEXT);
            lineY += 10;
            if (lineY > previewY + previewH - 4) {
                break;
            }
        }

        int tipsY = previewY + previewH + 10;
        guiGraphics.drawString(font, Component.translatable("menu.ashwake.createworld.tipTitle"), x, tipsY, AshwakePalette.LAVA_YELLOW);
        List<FormattedCharSequence> tipLines = font.split(TIPS.get(Math.floorMod(tipIndex, TIPS.size())), Math.max(80, width));
        int tipLineY = tipsY + 12;
        for (FormattedCharSequence line : tipLines) {
            guiGraphics.drawString(font, line, x, tipLineY, AshwakePalette.MUTED_TEXT);
            tipLineY += 10;
        }

        int warningsY = tipLineY + 8;
        List<Component> warnings = currentWarnings();
        if (!warnings.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("menu.ashwake.createworld.warningTitle"), x, warningsY, AshwakePalette.EMBER_ORANGE);
            int warningLineY = warningsY + 12;
            for (Component warning : warnings) {
                List<FormattedCharSequence> lines = font.split(warning, Math.max(80, width));
                for (FormattedCharSequence line : lines) {
                    guiGraphics.drawString(font, line, x, warningLineY, AshwakePalette.BONE_WHITE);
                    warningLineY += 10;
                }
            }
        }
    }

    private void drawFooterHint(GuiGraphics guiGraphics) {
        Component hint = Component.translatable("menu.ashwake.createworld.footerHint");
        int maxRight = moreOptionsButton.getX() - 10;
        int x = backButton.getX() + backButton.getWidth() + 10;
        if (x + font.width(hint) <= maxRight) {
            guiGraphics.drawString(font, hint, x, footerButtonY() + 7, AshwakePalette.MUTED_TEXT);
        }
    }

    private Component currentPresetLine() {
        WorldCreationUiState uiState = vanilla.getUiState();
        return Component.translatable("menu.ashwake.createworld.presetLine", uiState.getWorldType().describePreset());
    }

    private Component currentSummaryLine() {
        WorldCreationUiState uiState = vanilla.getUiState();
        Component gameMode = uiState.getGameMode().displayName;
        Component difficulty = uiState.getDifficulty().getDisplayName();
        Component commands = uiState.isAllowCommands()
                ? Component.translatable("menu.ashwake.createworld.commandsOn")
                : Component.translatable("menu.ashwake.createworld.commandsOff");
        return Component.translatable("menu.ashwake.createworld.summaryLine", gameMode, difficulty, commands);
    }

    private List<Component> currentWarnings() {
        List<Component> warnings = new ArrayList<>();
        WorldCreationUiState uiState = vanilla.getUiState();
        if (uiState.isHardcore()) {
            warnings.add(Component.translatable("menu.ashwake.createworld.warningHardcore"));
        }
        if (uiState.isAllowCommands()) {
            warnings.add(Component.translatable("menu.ashwake.createworld.warningCommands"));
        }
        return warnings;
    }

    private boolean isInControlsViewport(double mouseX, double mouseY) {
        return mouseX >= controlsViewportX
                && mouseX <= controlsViewportX + controlsViewportWidth
                && mouseY >= controlsViewportY
                && mouseY <= controlsViewportY + controlsViewportHeight;
    }

    private boolean isMountedVanillaWidget(Renderable renderable) {
        return renderable instanceof AbstractWidget widget && mountedLookup.containsKey(widget);
    }

    private static void invokeNoArg(Object owner, String methodName) {
        try {
            Method method = findMethod(owner.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            method.invoke(owner);
        } catch (Exception ignored) {
        }
    }

    private static Method findMethod(Class<?> type, String methodName) {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                    return method;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private static <T> T readField(Object owner, String fieldName, Class<T> type) {
        try {
            Field field = findField(owner.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object value = field.get(owner);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private enum Step {
        BASICS(0, "menu.ashwake.createworld.step.basics"),
        WORLD(1, "menu.ashwake.createworld.step.world"),
        MORE(2, "menu.ashwake.createworld.step.more");

        private final int index;
        private final String key;

        Step(int index, String key) {
            this.index = index;
            this.key = key;
        }

        int index() {
            return index;
        }

        Component title() {
            return Component.translatable(key);
        }

        Component header() {
            return switch (this) {
                case BASICS -> Component.translatable("menu.ashwake.createworld.header.basics");
                case WORLD -> Component.translatable("menu.ashwake.createworld.header.world");
                case MORE -> Component.translatable("menu.ashwake.createworld.header.more");
            };
        }

        static Step fromIndex(int index) {
            for (Step step : values()) {
                if (step.index == index) {
                    return step;
                }
            }
            return null;
        }
    }

    private record WidgetSize(int width, int height) {
    }

    private enum RowKind {
        STANDARD,
        TEXT_FIELD
    }

    private record WidgetRow(List<AbstractWidget> widgets, int minX, int maxX, RowKind kind, int height) {
    }

    private static final class RowBuild {
        private final int baseY;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private int minX = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxHeight;
        private boolean hasTextField;

        private RowBuild(int baseY) {
            this.baseY = baseY;
        }
    }
}
