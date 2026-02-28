package com.ashwake.mainmenu.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AshwakeBranding {
    private AshwakeBranding() {
    }

    public static int drawCenteredLogo(
            GuiGraphics guiGraphics,
            Font font,
            int centerX,
            int topY,
            int maxWidth,
            int maxHeight,
            float pulseFactor) {
        ResourceLocation logoTexture = resolveLogoTexture();
        if (logoTexture == null) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.literal("ASHWAKE"),
                    centerX,
                    topY + Math.max(12, (maxHeight - 8) / 2),
                    AshwakePalette.LAVA_YELLOW);
            return 12;
        }

        int boxWidth = Math.max(36, maxWidth);
        int boxHeight = Math.max(24, maxHeight);
        int drawWidth = boxWidth;
        int drawHeight = (int) (drawWidth / logoAspect(logoTexture));
        if (drawHeight > boxHeight) {
            drawHeight = boxHeight;
            drawWidth = (int) (drawHeight * logoAspect(logoTexture));
        }
        int boxX = centerX - (boxWidth / 2);

        // Shadow-only logo treatment to preserve readability without any halo/circle backplate.
        drawLogoLayer(
                guiGraphics,
                logoTexture,
                boxX + 1,
                topY + 2,
                boxWidth,
                boxHeight,
                logoTextureWidth(logoTexture),
                logoTextureHeight(logoTexture),
                0.0F,
                0.0F,
                0.0F,
                0.35F);

        float pulse = Mth.clamp(pulseFactor, 0.0F, 1.0F);
        float brightness = Mth.clamp(1.0F + ((pulse - 0.5F) * 0.06F), 0.95F, 1.05F);
        drawLogoLayer(
                guiGraphics,
                logoTexture,
                boxX,
                topY,
                boxWidth,
                boxHeight,
                logoTextureWidth(logoTexture),
                logoTextureHeight(logoTexture),
                brightness,
                brightness,
                brightness,
                1.0F);

        return drawHeight;
    }

    public static int drawLeftLogo(
            GuiGraphics guiGraphics,
            Font font,
            int leftX,
            int topY,
            int size,
            int color) {
        int clamped = Math.max(18, size);
        ResourceLocation logoTexture = resolveLogoTexture();
        if (logoTexture != null) {
            drawLogoLayer(
                    guiGraphics,
                    logoTexture,
                    leftX,
                    topY,
                    clamped,
                    clamped,
                    logoTextureWidth(logoTexture),
                    logoTextureHeight(logoTexture),
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F);
            return clamped;
        }

        guiGraphics.drawString(font, "A", leftX + (clamped / 2) - 3, topY + (clamped / 2) - 4, color);
        return clamped;
    }

    private static void drawLogoLayer(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            float red,
            float green,
            float blue,
            float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        try {
            guiGraphics.blitInscribed(
                    texture,
                    x,
                    y,
                    width,
                    height,
                    textureWidth,
                    textureHeight);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static float logoAspect(ResourceLocation texture) {
        return texture.equals(AshwakeUiSkin.LOGO_TRANSPARENT)
                ? (AshwakeUiSkin.LOGO_TRANSPARENT_WIDTH / (float) AshwakeUiSkin.LOGO_TRANSPARENT_HEIGHT)
                : 1.0F;
    }

    private static ResourceLocation resolveLogoTexture() {
        if (hasTexture(AshwakeUiSkin.LOGO_TRANSPARENT)) {
            return AshwakeUiSkin.LOGO_TRANSPARENT;
        }
        if (hasTexture(AshwakeUiSkin.LOGO_LEGACY)) {
            return AshwakeUiSkin.LOGO_LEGACY;
        }
        return null;
    }

    private static int logoTextureWidth(ResourceLocation texture) {
        return texture.equals(AshwakeUiSkin.LOGO_TRANSPARENT)
                ? AshwakeUiSkin.LOGO_TRANSPARENT_WIDTH
                : AshwakeUiSkin.LOGO_LEGACY_SIZE;
    }

    private static int logoTextureHeight(ResourceLocation texture) {
        return texture.equals(AshwakeUiSkin.LOGO_TRANSPARENT)
                ? AshwakeUiSkin.LOGO_TRANSPARENT_HEIGHT
                : AshwakeUiSkin.LOGO_LEGACY_SIZE;
    }

    private static boolean hasTexture(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }
        return minecraft.getResourceManager().getResource(texture).isPresent();
    }
}
