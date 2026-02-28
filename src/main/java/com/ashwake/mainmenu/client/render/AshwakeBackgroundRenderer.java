package com.ashwake.mainmenu.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import com.ashwake.mainmenu.config.AshwakeClientConfig;

public final class AshwakeBackgroundRenderer {
    private static final float MIN_DARKEN = 0.40F;
    private static final float MAX_DARKEN = 0.55F;

    private final AshwakeEmberSystem embers = new AshwakeEmberSystem();

    public void render(
            GuiGraphics graphics,
            int width,
            int height,
            float partialTick,
            boolean animationsEnabled,
            int animationIntensity,
            boolean particlesEnabled,
            int particleDensity,
            boolean reducedMotion) {
        if (AshwakeUiSkin.hasTexture(AshwakeUiSkin.BACKGROUND_MAIN)) {
            AshwakeUiSkin.drawCover(
                    graphics,
                    AshwakeUiSkin.BACKGROUND_MAIN,
                    0,
                    0,
                    width,
                    height,
                    AshwakeUiSkin.MAIN_BACKGROUND_WIDTH,
                    AshwakeUiSkin.MAIN_BACKGROUND_HEIGHT,
                    0,
                    0);
        } else {
            graphics.fillGradient(0, 0, width, height, AshwakePalette.ASH_DARK, AshwakePalette.ASH_DARKER);
        }

        float darken = Mth.clamp(AshwakeClientConfig.backgroundDarken() / 100.0F, MIN_DARKEN, MAX_DARKEN);
        int overlayAlpha = Mth.clamp((int) (darken * 255.0F), 0, 255);
        graphics.fill(0, 0, width, height, overlayAlpha << 24);

        int vignetteAlpha = reducedMotion ? 64 : 90;
        drawEdgeVignette(graphics, width, height, vignetteAlpha);

        float intensityScale = Mth.clamp(animationIntensity / 100.0F, 0.15F, 1.0F);
        int targetDensity = Mth.clamp(Math.round(particleDensity * intensityScale), 0, 100);
        int effectiveDensity = reducedMotion ? Math.max(4, targetDensity / 5) : Math.max(8, targetDensity / 3);

        if (animationsEnabled && particlesEnabled && effectiveDensity > 0) {
            embers.updateAndRender(
                    graphics,
                    width,
                    height,
                    true,
                    effectiveDensity,
                    reducedMotion);
        } else {
            embers.updateAndRender(
                    graphics,
                    width,
                    height,
                    false,
                    0,
                    true);
        }
    }

    public void resetParticles() {
        embers.reset();
    }

    private static void drawEdgeVignette(GuiGraphics graphics, int width, int height, int alpha) {
        int topBand = Math.max(24, height / 5);
        int sideBand = Math.max(24, width / 7);
        int color = (alpha << 24);

        graphics.fillGradient(0, 0, width, topBand, color, 0x00000000);
        graphics.fillGradient(0, height - topBand, width, height, 0x00000000, color);
        graphics.fillGradient(0, 0, sideBand, height, color, 0x00000000);
        graphics.fillGradient(width - sideBand, 0, width, height, 0x00000000, color);
    }
}
