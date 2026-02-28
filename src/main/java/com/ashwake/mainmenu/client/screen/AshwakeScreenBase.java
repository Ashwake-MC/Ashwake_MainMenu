package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.render.AshwakeBackgroundRenderer;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.render.AshwakeUiSkin;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AshwakeScreenBase extends Screen {
    protected static final int HEADER_HEIGHT = 64;
    protected static final int FOOTER_HEIGHT = 56;
    protected static final int SIDE_MARGIN = 18;
    protected static final int CONTENT_TOP_PADDING = 12;
    protected static final int CONTENT_BOTTOM_PADDING = 10;
    protected static final int CONTENT_BOTTOM_SAFE_PADDING = FOOTER_HEIGHT + 12;
    protected static final int PANEL_PADDING = 16;

    private static final AshwakeBackgroundRenderer BACKGROUND_RENDERER = new AshwakeBackgroundRenderer();

    protected AshwakeScreenBase(Component title) {
        super(title);
    }

    protected void renderAshwakeBackground(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
        BACKGROUND_RENDERER.render(
                guiGraphics,
                screenWidth,
                screenHeight,
                partialTick,
                AshwakeClientConfig.animationsEnabled(),
                AshwakeClientConfig.animationIntensity(),
                AshwakeClientConfig.particlesEnabled(),
                AshwakeClientConfig.particleDensity(),
                AshwakeClientConfig.reducedMotion());
    }

    protected void drawSafePanel(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        drawPanelFrame(guiGraphics, x, y, w, h, false);
    }

    protected static int centerX(int width, int panelWidth) {
        return (width - panelWidth) / 2;
    }

    protected static int clampPanelWidth(int width, int maxWidth) {
        return Math.max(220, Math.min(maxWidth, width - (SIDE_MARGIN * 2)));
    }

    protected int headerTop() {
        return 0;
    }

    protected int headerBottom() {
        return HEADER_HEIGHT;
    }

    protected int contentTop() {
        return HEADER_HEIGHT + CONTENT_TOP_PADDING;
    }

    protected int contentBottom() {
        return height - FOOTER_HEIGHT - CONTENT_BOTTOM_PADDING;
    }

    protected int contentHeight() {
        return Math.max(24, contentBottom() - contentTop());
    }

    protected int footerTop() {
        return height - FOOTER_HEIGHT;
    }

    protected int footerButtonY() {
        return footerTop() + 14;
    }

    protected int safePanelX(int maxWidth) {
        return centerX(width, clampPanelWidth(width, maxWidth));
    }

    protected int safePanelWidth(int maxWidth) {
        return clampPanelWidth(width, maxWidth);
    }

    protected void drawPanelFrame(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean focused) {
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

    protected void drawHeaderFooterBands(GuiGraphics guiGraphics, int panelX, int panelWidth) {
        guiGraphics.fill(panelX + 1, headerTop(), panelX + panelWidth - 1, headerBottom(), 0x58151110);
        guiGraphics.fill(panelX + 1, footerTop(), panelX + panelWidth - 1, height, 0x6A151110);
        guiGraphics.fill(panelX + 1, headerBottom(), panelX + panelWidth - 1, headerBottom() + 1, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(panelX + 1, footerTop() - 1, panelX + panelWidth - 1, footerTop(), AshwakePalette.BASALT_EDGE);
    }

    protected void drawLayoutBands(GuiGraphics guiGraphics, int panelX, int panelWidth) {
        int panelTop = 8;
        int panelBottom = height - 8;
        drawPanelFrame(guiGraphics, panelX, panelTop, panelWidth, panelBottom - panelTop, false);
        drawHeaderFooterBands(guiGraphics, panelX, panelWidth);
    }

    protected void drawScrollbar(
            GuiGraphics guiGraphics,
            int trackX,
            int top,
            int bottom,
            int visibleHeight,
            int contentHeight,
            int scrollOffset) {
        if (contentHeight <= visibleHeight || visibleHeight <= 0) {
            return;
        }

        int trackHeight = Math.max(12, bottom - top);
        int thumbHeight = Mth.clamp((visibleHeight * trackHeight) / contentHeight, 18, trackHeight - 2);
        int maxScroll = Math.max(1, contentHeight - visibleHeight);
        int thumbTravel = Math.max(1, trackHeight - thumbHeight - 2);
        int thumbY = top + 1 + (scrollOffset * thumbTravel) / maxScroll;

        guiGraphics.fill(trackX, top, trackX + 5, bottom, 0x9C191512);
        guiGraphics.fill(trackX, top, trackX + 5, top + 1, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(trackX, bottom - 1, trackX + 5, bottom, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(trackX + 1, thumbY, trackX + 4, thumbY + thumbHeight, AshwakePalette.EMBER_ORANGE);
    }

    protected boolean isInContentArea(double mouseX, double mouseY) {
        return mouseX >= SIDE_MARGIN && mouseX <= width - SIDE_MARGIN && mouseY >= contentTop() && mouseY <= contentBottom();
    }
}
