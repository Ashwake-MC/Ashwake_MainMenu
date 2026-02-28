package com.ashwake.mainmenu.client.screen;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.widget.AshwakeButton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class AshwakeFaqScreen extends AshwakeScreenBase {
    private static final ResourceLocation FAQ_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            AshwakeMainMenuMod.MOD_ID,
            "faq/faq.md");
    private static final int LINE_HEIGHT = 10;

    private final Screen parent;
    private final List<FaqLine> parsedLines = new ArrayList<>();
    private final List<DrawLine> wrappedLines = new ArrayList<>();

    private int panelX;
    private int panelWidth;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int scrollOffset;
    private int maxScroll;

    public AshwakeFaqScreen(Screen parent) {
        super(Component.translatable("menu.ashwake.faq.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = safePanelWidth(900);
        panelX = safePanelX(900);
        scrollOffset = 0;
        maxScroll = 0;
        parseFaqMarkdown();

        addRenderableWidget(new AshwakeButton(
                panelX + panelWidth - 114,
                footerButtonY(),
                102,
                24,
                Component.translatable("gui.back"),
                b -> minecraft.setScreen(parent)));

        relayout();
    }

    @Override
    public void repositionElements() {
        relayout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0 || mouseX < contentX || mouseX > contentX + contentW || mouseY < contentY || mouseY > contentY + contentH) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 16), 0, maxScroll);
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderAshwakeBackground(guiGraphics, width, height, partialTick);
        drawLayoutBands(guiGraphics, panelX, panelWidth);

        int logoSize = AshwakeBranding.drawLeftLogo(guiGraphics, font, panelX + 10, 11, 30, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(font, title, panelX + logoSize + 18, 18, AshwakePalette.LAVA_YELLOW);
        guiGraphics.drawString(
                font,
                Component.translatable("menu.ashwake.faq.subtitle"),
                panelX + logoSize + 18,
                31,
                AshwakePalette.MUTED_TEXT);

        drawPanelFrame(guiGraphics, contentX, contentY, contentW, contentH, false);
        drawFaqLines(guiGraphics);

        if (maxScroll > 0) {
            drawScrollbar(
                    guiGraphics,
                    contentX + contentW - 7,
                    contentY + 2,
                    contentY + contentH - 2,
                    contentH - 4,
                    maxScroll + contentH,
                    scrollOffset);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void relayout() {
        contentX = panelX + 10;
        contentY = contentTop();
        contentW = panelWidth - 20;
        contentH = contentHeight();
        wrapFaqLines();
    }

    private void parseFaqMarkdown() {
        parsedLines.clear();
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FAQ_RESOURCE);
        if (resource.isEmpty()) {
            parsedLines.add(new FaqLine(Component.translatable("menu.ashwake.faq.missing"), AshwakePalette.BONE_WHITE));
            return;
        }

        String markdown;
        try (InputStream stream = resource.get().open()) {
            markdown = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            parsedLines.add(new FaqLine(Component.translatable("menu.ashwake.faq.missing"), AshwakePalette.BONE_WHITE));
            AshwakeMainMenuMod.LOGGER.warn("Failed reading FAQ resource {}", FAQ_RESOURCE, exception);
            return;
        }

        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                parsedLines.add(new FaqLine(Component.empty(), AshwakePalette.BONE_WHITE));
                continue;
            }
            if (line.startsWith("# ")) {
                continue;
            }
            if (line.startsWith("## ")) {
                parsedLines.add(new FaqLine(Component.literal(line.substring(3).trim()), AshwakePalette.LAVA_YELLOW));
                continue;
            }
            if (line.startsWith("- ") || line.startsWith("* ")) {
                parsedLines.add(new FaqLine(Component.literal("- " + line.substring(2).trim()), AshwakePalette.BONE_WHITE));
                continue;
            }
            parsedLines.add(new FaqLine(Component.literal(line), AshwakePalette.MUTED_TEXT));
        }
    }

    private void wrapFaqLines() {
        wrappedLines.clear();
        int wrapWidth = Math.max(120, contentW - 20);
        for (FaqLine line : parsedLines) {
            if (line.text().getString().isBlank()) {
                wrappedLines.add(new DrawLine(Component.empty().getVisualOrderText(), line.color()));
                continue;
            }
            List<FormattedCharSequence> split = font.split(line.text(), wrapWidth);
            if (split.isEmpty()) {
                wrappedLines.add(new DrawLine(line.text().getVisualOrderText(), line.color()));
            } else {
                for (FormattedCharSequence part : split) {
                    wrappedLines.add(new DrawLine(part, line.color()));
                }
            }
        }

        int totalHeight = Math.max(LINE_HEIGHT, wrappedLines.size() * LINE_HEIGHT);
        maxScroll = Math.max(0, totalHeight - (contentH - 16));
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    private void drawFaqLines(GuiGraphics guiGraphics) {
        int x = contentX + 8;
        int y = contentY + 8;
        int width = contentW - 16;
        int height = contentH - 16;

        guiGraphics.enableScissor(contentX + 1, contentY + 1, contentX + contentW - 1, contentY + contentH - 1);

        int firstLine = scrollOffset / LINE_HEIGHT;
        int lineOffset = -(scrollOffset % LINE_HEIGHT);
        int maxLines = (height / LINE_HEIGHT) + 2;
        for (int i = 0; i < maxLines; i++) {
            int index = firstLine + i;
            if (index >= wrappedLines.size()) {
                break;
            }
            int lineY = y + lineOffset + (i * LINE_HEIGHT);
            if (lineY < y - LINE_HEIGHT || lineY > y + height) {
                continue;
            }
            DrawLine line = wrappedLines.get(index);
            guiGraphics.drawString(font, line.text(), x, lineY, line.color());
        }

        guiGraphics.disableScissor();
    }

    private record FaqLine(Component text, int color) {
    }

    private record DrawLine(FormattedCharSequence text, int color) {
    }
}
