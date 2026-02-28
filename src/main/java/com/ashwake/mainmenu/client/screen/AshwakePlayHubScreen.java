package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class AshwakePlayHubScreen extends AshwakeScreenBase {
    private final Screen parent;
    private final List<AshwakeButton> actionButtons = new ArrayList<>();

    private int panelX;
    private int panelWidth;
    private int contentTopY;
    private int contentBottomY;
    private int scrollOffset;
    private int maxScroll;

    public AshwakePlayHubScreen(Screen parent) {
        super(Component.translatable("menu.ashwake.playhub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = safePanelWidth(520);
        panelX = safePanelX(520);
        contentTopY = contentTop();
        contentBottomY = contentBottom();
        scrollOffset = 0;
        maxScroll = 0;
        actionButtons.clear();

        int buttonWidth = panelWidth - 76;
        int buttonX = panelX + (panelWidth - buttonWidth) / 2;
        int buttonHeight = 28;
        int buttonGap = 8;

        actionButtons.add(addRenderableWidget(new AshwakeButton(
                buttonX,
                0,
                buttonWidth,
                buttonHeight,
                Component.translatable("menu.singleplayer"),
                button -> minecraft.setScreen(new AshwakeSelectWorldScreen(this)))));
        actionButtons.add(addRenderableWidget(new AshwakeButton(
                buttonX,
                0,
                buttonWidth,
                buttonHeight,
                Component.translatable("menu.multiplayer"),
                button -> minecraft.setScreen(new AshwakeMultiplayerScreen(this)))));
        actionButtons.add(addRenderableWidget(new AshwakeButton(
                buttonX,
                0,
                buttonWidth,
                buttonHeight,
                Component.translatable("menu.ashwake.guidance"),
                button -> minecraft.setScreen(new AshwakeGuidanceScreen(this)))));

        int requiredHeight = actionButtons.size() * buttonHeight + (actionButtons.size() - 1) * buttonGap;
        int availableHeight = contentBottomY - contentTopY;
        maxScroll = Math.max(0, (requiredHeight + CONTENT_BOTTOM_SAFE_PADDING) - availableHeight);
        relayoutButtons(buttonHeight, buttonGap);

        addRenderableWidget(new AshwakeButton(
                panelX + (panelWidth - 120) / 2,
                footerButtonY(),
                120,
                24,
                Component.translatable("gui.back"),
                button -> minecraft.setScreen(parent)));
    }

    private void relayoutButtons(int buttonHeight, int buttonGap) {
        int startY = contentTopY + 10;
        for (int i = 0; i < actionButtons.size(); i++) {
            AshwakeButton button = actionButtons.get(i);
            int y = startY + i * (buttonHeight + buttonGap) - scrollOffset;
            button.setY(y);
            button.visible = y + buttonHeight >= contentTopY && y <= contentBottomY;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);

        guiGraphics.drawCenteredString(
                font,
                Component.translatable("menu.ashwake.playhub.title"),
                width / 2,
                14,
                AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawCenteredString(
                font,
                Component.translatable("menu.ashwake.playhub.subtitle"),
                width / 2,
                30,
                AshwakePalette.BONE_WHITE);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInContentArea(mouseX, mouseY) || maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 16), 0, maxScroll);
        relayoutButtons(28, 8);
        return true;
    }
}

