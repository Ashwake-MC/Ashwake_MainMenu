package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.client.AshwakeClientRuntime;
import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class AshwakeOnboardingScreen extends AshwakeScreenBase {
    private static final List<Component> PANELS = List.of(
            Component.translatable("menu.ashwake.onboarding.panel1"),
            Component.translatable("menu.ashwake.onboarding.panel2"),
            Component.translatable("menu.ashwake.onboarding.panel3"));

    private final Screen returnScreen;
    private final List<FormattedCharSequence> wrappedLines = new ArrayList<>();

    private int panelIndex;
    private int panelX;
    private int panelWidth;
    private int panelY;
    private int panelHeight;
    private int textAreaTop;
    private int textAreaBottom;
    private int scrollOffset;
    private int maxScroll;

    private AshwakeButton previousButton;
    private AshwakeButton nextButton;

    public AshwakeOnboardingScreen(Screen returnScreen) {
        super(Component.translatable("menu.ashwake.onboarding.title"));
        this.returnScreen = returnScreen;
    }

    @Override
    protected void init() {
        panelWidth = safePanelWidth(700);
        panelHeight = Math.min(contentHeight() - 4, 320);
        panelX = safePanelX(700);
        panelY = contentTop() + Math.max(0, (contentHeight() - panelHeight) / 2);
        textAreaTop = panelY + 58;
        textAreaBottom = panelY + panelHeight - 22;

        previousButton = addRenderableWidget(new AshwakeButton(
                panelX + 12,
                footerButtonY(),
                110,
                24,
                Component.translatable("gui.back"),
                button -> {
                    if (panelIndex > 0) {
                        panelIndex--;
                        refreshControls();
                        refreshWrappedLines();
                    }
                }));

        nextButton = addRenderableWidget(new AshwakeButton(
                panelX + panelWidth - 172,
                footerButtonY(),
                160,
                24,
                Component.empty(),
                button -> {
                    if (panelIndex < PANELS.size() - 1) {
                        panelIndex++;
                        refreshControls();
                        refreshWrappedLines();
                        return;
                    }
                    finish(true);
                }));

        addRenderableWidget(new AshwakeButton(
                panelX + (panelWidth / 2) - 45,
                footerButtonY(),
                90,
                24,
                Component.translatable("menu.ashwake.onboarding.skip"),
                button -> finish(false)));

        refreshControls();
        refreshWrappedLines();
    }

    private void refreshWrappedLines() {
        wrappedLines.clear();
        int textWidth = panelWidth - 50;
        wrappedLines.addAll(font.split(PANELS.get(panelIndex), textWidth));
        scrollOffset = 0;

        int lineHeight = 12;
        int contentH = textAreaBottom - textAreaTop;
        int totalH = wrappedLines.size() * lineHeight;
        maxScroll = Math.max(0, (totalH + 14) - contentH);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < panelX + 18 || mouseX > panelX + panelWidth - 18 || mouseY < textAreaTop || mouseY > textAreaBottom) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 14), 0, maxScroll);
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);

        AshwakeBranding.drawLeftLogo(guiGraphics, font, panelX + 10, 12, 36, AshwakePalette.LAVA_YELLOW);

        guiGraphics.drawCenteredString(font, title, width / 2, 16, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawCenteredString(
                font,
                Component.translatable("menu.ashwake.onboarding.progress", panelIndex + 1, PANELS.size()),
                width / 2,
                32,
                AshwakePalette.MUTED_TEXT);

        drawPanelFrame(guiGraphics, panelX + 10, panelY, panelWidth - 20, panelHeight, true);
        if (panelIndex == PANELS.size() - 1) {
            AshwakeBranding.drawCenteredLogo(guiGraphics, font, width / 2, panelY + 10, 124, 124, 0.4F);
        }

        int lineHeight = 12;
        int firstLine = scrollOffset / lineHeight;
        int lineOffset = -(scrollOffset % lineHeight);
        int visibleLines = ((textAreaBottom - textAreaTop) / lineHeight) + 2;
        int textX = panelX + 26;
        int textY = textAreaTop;
        for (int i = 0; i < visibleLines; i++) {
            int idx = firstLine + i;
            if (idx >= wrappedLines.size()) {
                break;
            }
            int y = textY + lineOffset + i * lineHeight;
            if (y < textAreaTop - lineHeight || y > textAreaBottom) {
                continue;
            }
            guiGraphics.drawString(font, wrappedLines.get(idx), textX, y, AshwakePalette.BONE_WHITE);
        }

        drawProgressDots(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawProgressDots(GuiGraphics guiGraphics) {
        int center = panelX + (panelWidth / 2);
        int y = footerTop() - 12;
        int spacing = 14;
        int startX = center - ((PANELS.size() - 1) * spacing / 2);
        for (int i = 0; i < PANELS.size(); i++) {
            int color = i == panelIndex ? AshwakePalette.EMBER_ORANGE : AshwakePalette.IRON_GRAY;
            int x = startX + (i * spacing);
            guiGraphics.fill(x, y, x + 6, y + 6, color);
        }
    }

    private void refreshControls() {
        previousButton.active = panelIndex > 0;
        if (panelIndex == PANELS.size() - 1) {
            nextButton.setMessage(Component.translatable("menu.ashwake.onboarding.talkToGuidance"));
        } else {
            nextButton.setMessage(Component.translatable("menu.ashwake.onboarding.next"));
        }
    }

    private void finish(boolean openGuidance) {
        AshwakeClientRuntime.markOnboardingCompleted();
        if (openGuidance) {
            minecraft.setScreen(new AshwakeGuidanceScreen(returnScreen));
            return;
        }
        minecraft.setScreen(returnScreen);
    }
}

