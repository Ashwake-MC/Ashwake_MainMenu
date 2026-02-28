package com.ashwake.mainmenu.client.render;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AshwakeUiSkin {
    public static final ResourceLocation PANEL_FRAME = texture("gui/panel_frame_9slice.png");
    public static final ResourceLocation BUTTON_IDLE = texture("gui/button_idle_9slice.png");
    public static final ResourceLocation BUTTON_HOVER = texture("gui/button_hover_9slice.png");
    public static final ResourceLocation BUTTON_PRESS = texture("gui/button_press_9slice.png");
    public static final ResourceLocation BUTTON_DISABLED = texture("gui/button_disabled_9slice.png");
    public static final ResourceLocation FOCUS_GLOW = texture("gui/focus_glow.png");

    public static final ResourceLocation ICON_PLAY = texture("gui/icons/icon_play.png");
    public static final ResourceLocation ICON_OPTIONS = texture("gui/icons/icon_options.png");
    public static final ResourceLocation ICON_CHANGELOG = texture("gui/icons/icon_changelog.png");
    public static final ResourceLocation ICON_DISCORD = texture("gui/icons/icon_discord.png");
    public static final ResourceLocation ICON_QUIT = texture("gui/icons/icon_quit.png");
    public static final ResourceLocation ICON_GUIDANCE = texture("gui/icons/icon_guidance.png");

    public static final ResourceLocation LOGO_TRANSPARENT = texture("gui/logo_transparent.png");
    public static final ResourceLocation LOGO_LEGACY = texture("gui/logo.png");

    public static final ResourceLocation BACKGROUND_MAIN = texture("background/main_volcano.png");
    public static final ResourceLocation BACKGROUND_FAR = texture("background/volcano_far.png");
    public static final ResourceLocation BACKGROUND_MID = texture("background/ridges_mid.png");
    public static final ResourceLocation BACKGROUND_NEAR = texture("background/ash_near.png");
    public static final ResourceLocation GUI_VIGNETTE = texture("gui/vignette.png");
    public static final ResourceLocation BACKGROUND_VIGNETTE = texture("background/vignette.png");

    public static final ResourceLocation PARTICLE_EMBER_0 = texture("particles/ember_0.png");
    public static final ResourceLocation PARTICLE_EMBER_1 = texture("particles/ember_1.png");
    public static final ResourceLocation PARTICLE_EMBER_2 = texture("particles/ember_2.png");

    public static final int NINE_SLICE_TEXTURE_SIZE = 32;
    public static final int NINE_SLICE_CORNER = 8;
    public static final int FOCUS_GLOW_TEXTURE_SIZE = 64;
    public static final int ICON_TEXTURE_SIZE = 16;
    public static final int BACKGROUND_TEXTURE_WIDTH = 512;
    public static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    public static final int MAIN_BACKGROUND_WIDTH = 1536;
    public static final int MAIN_BACKGROUND_HEIGHT = 1024;
    public static final int LOGO_TRANSPARENT_WIDTH = 2048;
    public static final int LOGO_TRANSPARENT_HEIGHT = 2048;
    public static final int LOGO_LEGACY_SIZE = 1024;
    public static final int VIGNETTE_TEXTURE_SIZE = 256;

    private AshwakeUiSkin() {
    }

    public static ResourceLocation texture(String relativePath) {
        return ResourceLocation.fromNamespaceAndPath(
                AshwakeMainMenuMod.MOD_ID,
                "textures/" + relativePath);
    }

    public static boolean hasTexture(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }
        return minecraft.getResourceManager().getResource(texture).isPresent();
    }

    public static void drawNineSlice(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || !hasTexture(texture)) {
            return;
        }

        int corner = Math.min(NINE_SLICE_CORNER, Math.min(width / 2, height / 2));
        if (corner <= 0) {
            drawTiled(guiGraphics, texture, x, y, width, height, NINE_SLICE_TEXTURE_SIZE, NINE_SLICE_TEXTURE_SIZE);
            return;
        }

        int centerSourceSize = NINE_SLICE_TEXTURE_SIZE - (corner * 2);
        int innerWidth = width - (corner * 2);
        int innerHeight = height - (corner * 2);

        // Corners.
        guiGraphics.blit(texture, x, y, 0, 0, corner, corner, NINE_SLICE_TEXTURE_SIZE, NINE_SLICE_TEXTURE_SIZE);
        guiGraphics.blit(texture, x + width - corner, y, NINE_SLICE_TEXTURE_SIZE - corner, 0, corner, corner, NINE_SLICE_TEXTURE_SIZE, NINE_SLICE_TEXTURE_SIZE);
        guiGraphics.blit(texture, x, y + height - corner, 0, NINE_SLICE_TEXTURE_SIZE - corner, corner, corner, NINE_SLICE_TEXTURE_SIZE, NINE_SLICE_TEXTURE_SIZE);
        guiGraphics.blit(
                texture,
                x + width - corner,
                y + height - corner,
                NINE_SLICE_TEXTURE_SIZE - corner,
                NINE_SLICE_TEXTURE_SIZE - corner,
                corner,
                corner,
                NINE_SLICE_TEXTURE_SIZE,
                NINE_SLICE_TEXTURE_SIZE);

        if (innerWidth > 0) {
            tileHorizontal(guiGraphics, texture, x + corner, y, innerWidth, corner, corner, 0, centerSourceSize);
            tileHorizontal(
                    guiGraphics,
                    texture,
                    x + corner,
                    y + height - corner,
                    innerWidth,
                    corner,
                    corner,
                    NINE_SLICE_TEXTURE_SIZE - corner,
                    centerSourceSize);
        }

        if (innerHeight > 0) {
            tileVertical(guiGraphics, texture, x, y + corner, corner, innerHeight, 0, corner, centerSourceSize);
            tileVertical(
                    guiGraphics,
                    texture,
                    x + width - corner,
                    y + corner,
                    corner,
                    innerHeight,
                    NINE_SLICE_TEXTURE_SIZE - corner,
                    corner,
                    centerSourceSize);
        }

        if (innerWidth > 0 && innerHeight > 0) {
            tileArea(guiGraphics, texture, x + corner, y + corner, innerWidth, innerHeight, corner, corner, centerSourceSize, centerSourceSize);
        }
    }

    public static void drawFocusGlow(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int glowInset = 5;
        int glowX = x - glowInset;
        int glowY = y - glowInset;
        int glowW = width + glowInset * 2;
        int glowH = height + glowInset * 2;
        drawRectFocusGlow(guiGraphics, glowX, glowY, glowW, glowH);
    }

    private static void drawRectFocusGlow(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int outerGlow = (0x42 << 24) | (AshwakePalette.EMBER_ORANGE & 0x00FFFFFF);
        int innerGlow = (0x2A << 24) | (AshwakePalette.LAVA_YELLOW & 0x00FFFFFF);

        guiGraphics.fill(x, y, x + width, y + 1, outerGlow);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, outerGlow);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, outerGlow);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, outerGlow);

        if (width > 2 && height > 2) {
            guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, innerGlow);
            guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, innerGlow);
            guiGraphics.fill(x + 1, y + 2, x + 2, y + height - 2, innerGlow);
            guiGraphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, innerGlow);
        }
    }

    public static void drawTiled(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0 || !hasTexture(texture)) {
            return;
        }

        int offsetY = 0;
        while (offsetY < height) {
            int drawH = Math.min(textureHeight, height - offsetY);
            int offsetX = 0;
            while (offsetX < width) {
                int drawW = Math.min(textureWidth, width - offsetX);
                guiGraphics.blit(
                        texture,
                        x + offsetX,
                        y + offsetY,
                        0,
                        0,
                        drawW,
                        drawH,
                        textureWidth,
                        textureHeight);
                offsetX += drawW;
            }
            offsetY += drawH;
        }
    }

    public static void drawCover(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int viewportX,
            int viewportY,
            int viewportWidth,
            int viewportHeight,
            int textureWidth,
            int textureHeight,
            int offsetX,
            int offsetY) {
        if (viewportWidth <= 0 || viewportHeight <= 0 || textureWidth <= 0 || textureHeight <= 0 || !hasTexture(texture)) {
            return;
        }

        float scale = Math.max(viewportWidth / (float) textureWidth, viewportHeight / (float) textureHeight);
        int drawWidth = Math.max(1, Mth.ceil(textureWidth * scale));
        int drawHeight = Math.max(1, Mth.ceil(textureHeight * scale));
        int drawX = viewportX + ((viewportWidth - drawWidth) / 2) + offsetX;
        int drawY = viewportY + ((viewportHeight - drawHeight) / 2) + offsetY;

        guiGraphics.blit(
                texture,
                drawX,
                drawY,
                0,
                0,
                drawWidth,
                drawHeight,
                textureWidth,
                textureHeight);
    }

    private static void tileHorizontal(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int sourceU,
            int sourceV,
            int sourceWidth) {
        int drawn = 0;
        while (drawn < width) {
            int drawW = Math.min(sourceWidth, width - drawn);
            guiGraphics.blit(
                    texture,
                    x + drawn,
                    y,
                    sourceU,
                    sourceV,
                    drawW,
                    height,
                    NINE_SLICE_TEXTURE_SIZE,
                    NINE_SLICE_TEXTURE_SIZE);
            drawn += drawW;
        }
    }

    private static void tileVertical(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int sourceU,
            int sourceV,
            int sourceHeight) {
        int drawn = 0;
        while (drawn < height) {
            int drawH = Math.min(sourceHeight, height - drawn);
            guiGraphics.blit(
                    texture,
                    x,
                    y + drawn,
                    sourceU,
                    sourceV,
                    width,
                    drawH,
                    NINE_SLICE_TEXTURE_SIZE,
                    NINE_SLICE_TEXTURE_SIZE);
            drawn += drawH;
        }
    }

    private static void tileArea(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int sourceU,
            int sourceV,
            int sourceWidth,
            int sourceHeight) {
        int offsetY = 0;
        while (offsetY < height) {
            int drawH = Math.min(sourceHeight, height - offsetY);
            int offsetX = 0;
            while (offsetX < width) {
                int drawW = Math.min(sourceWidth, width - offsetX);
                guiGraphics.blit(
                        texture,
                        x + offsetX,
                        y + offsetY,
                        sourceU,
                        sourceV,
                        drawW,
                        drawH,
                        NINE_SLICE_TEXTURE_SIZE,
                        NINE_SLICE_TEXTURE_SIZE);
                offsetX += drawW;
            }
            offsetY += drawH;
        }
    }
}
