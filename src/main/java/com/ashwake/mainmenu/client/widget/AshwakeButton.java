package com.ashwake.mainmenu.client.widget;

import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.render.AshwakeUiSkin;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class AshwakeButton extends Button {
    private static final float HOVER_ENTER_SPEED = 8.0F;
    private static final float HOVER_EXIT_SPEED = 6.4F;

    public enum Icon {
        NONE,
        PLAY,
        OPTIONS,
        CHANGELOG,
        DISCORD,
        QUIT,
        GUIDANCE
    }

    private final Icon icon;
    private float hoverProgress;
    private float clickFlash;
    private float ambientAccumulator;
    private long lastFrameNanos = Util.getNanos();
    private final HoverParticleSystem particleSystem = new HoverParticleSystem();

    public AshwakeButton(int x, int y, int width, int height, Component label, OnPress onPress) {
        this(x, y, width, height, label, Icon.NONE, onPress);
    }

    public AshwakeButton(int x, int y, int width, int height, Component label, Icon icon, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.icon = icon == null ? Icon.NONE : icon;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long nowNanos = Util.getNanos();
        float dt = Mth.clamp((nowNanos - lastFrameNanos) / 1_000_000_000F, 0.0F, 0.075F);
        lastFrameNanos = nowNanos;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        boolean hovered = isHoveredOrFocused();
        boolean particlesEnabledForScreen = areUiParticlesEnabledForCurrentScreen();
        float previousHover = hoverProgress;
        hoverProgress = animateHoverProgress(hoverProgress, active && hovered, dt);

        if (active && hoverProgress > 0.01F && previousHover <= 0.01F) {
            playHoverSound();
            if (particlesEnabledForScreen) {
                spawnHoverEnterBurst(x, y, w, h);
            }
        }

        maybeSpawnHoverAmbient(x, y, w, h, dt, active && hovered, particlesEnabledForScreen);
        clickFlash = Math.max(0F, clickFlash - (dt * 7.5F));
        if (particlesEnabledForScreen) {
            particleSystem.updateAndRender(guiGraphics, dt);
        } else {
            ambientAccumulator = 0F;
            particleSystem.clear();
        }
        boolean pressed = active && hovered && Minecraft.getInstance().mouseHandler.isLeftPressed();
        int drawY = y + Math.round((hoverProgress * -1.0F) + (pressed ? hoverProgress : 0F));

        if (active && hoverProgress > 0.28F) {
            AshwakeUiSkin.drawFocusGlow(guiGraphics, x, drawY, w, h);
        }

        ResourceLocation texture = selectStateTexture(hovered, pressed);
        if (AshwakeUiSkin.hasTexture(texture)) {
            AshwakeUiSkin.drawNineSlice(guiGraphics, texture, x, drawY, w, h);
            int innerAlpha = active ? Mth.clamp((int) (20 + (22 * hoverProgress)), 20, 44) : 72;
            guiGraphics.fill(x + 2, drawY + 2, x + w - 2, drawY + h - 2, (innerAlpha << 24) | 0x0E0B0A);
        } else {
            drawFallbackFrame(guiGraphics, x, drawY, w, h, hovered, pressed);
        }
        drawShimmer(guiGraphics, x, drawY, w, h, hoverProgress);

        if (clickFlash > 0.0F) {
            int flashAlpha = Mth.clamp((int) (clickFlash * 56.0F), 0, 56);
            guiGraphics.fill(x + 1, drawY + 1, x + w - 1, drawY + h - 1, (flashAlpha << 24) | 0xF5A14B);
        }

        int iconSpace = icon == Icon.NONE ? 0 : 16;
        if (icon != Icon.NONE) {
            int iconX = x + 6;
            int iconY = drawY + (h - 12) / 2;
            if (!drawTextureIcon(guiGraphics, iconX, iconY, icon, active, hoverProgress)) {
                drawIcon(guiGraphics, iconX, iconY, icon, tintedByHover(active ? AshwakePalette.BONE_WHITE : 0xFF9E9890, hoverProgress, 0.12F));
            }
        }

        int textColor = tintedByHover(active ? AshwakePalette.BONE_WHITE : 0xFFA6A09A, hoverProgress, 0.11F);
        int alphaBits = Mth.ceil(this.alpha * 255.0F) << 24;
        int packedTextColor = (textColor & 0x00FFFFFF) | alphaBits;
        int textY = drawY + (h - 8) / 2;

        if (icon == Icon.NONE) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + (w / 2), textY, packedTextColor);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, getMessage(), x + 8 + iconSpace, textY, packedTextColor);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.9F, 0.88F));
        clickFlash = 0.12F;
        spawnClickBurst(getX(), getY(), getWidth(), getHeight());
    }

    private static void playHoverSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.32F, 1.45F));
    }

    private static float animateHoverProgress(float current, boolean hovered, float dt) {
        float speed = hovered ? HOVER_ENTER_SPEED : HOVER_EXIT_SPEED;
        if (hovered) {
            return Mth.clamp(current + speed * dt, 0.0F, 1.0F);
        }
        return Mth.clamp(current - speed * dt, 0.0F, 1.0F);
    }

    private void maybeSpawnHoverAmbient(int x, int y, int w, int h, float dt, boolean hovered, boolean particlesEnabledForScreen) {
        if (!hovered
                || hoverProgress <= 0.15F
                || AshwakeClientConfig.reducedMotion()
                || !AshwakeClientConfig.hoverParticlesEnabled()
                || !particlesEnabledForScreen) {
            ambientAccumulator = 0F;
            return;
        }

        float densityScale = Mth.clamp(AshwakeClientConfig.hoverParticleDensity() / 100.0F, 0.0F, 1.0F);
        float ambientRate = Mth.lerp(hoverProgress, 1.0F, 4.0F) * densityScale;
        ambientAccumulator += ambientRate * dt;

        int softCap = 8 + (int) (densityScale * 22);
        while (ambientAccumulator >= 1.0F) {
            ambientAccumulator -= 1.0F;
            particleSystem.spawnAmbient(x, y, w, h, softCap);
        }
    }

    private void spawnHoverEnterBurst(int x, int y, int w, int h) {
        if (!AshwakeClientConfig.hoverParticlesEnabled() || !areUiParticlesEnabledForCurrentScreen()) {
            return;
        }
        float densityScale = Mth.clamp(AshwakeClientConfig.hoverParticleDensity() / 100.0F, 0.0F, 1.0F);
        int count = Mth.clamp(8 + Math.round(densityScale * 10.0F), 8, 18);
        int softCap = 14 + (int) (densityScale * 28);
        particleSystem.spawnBurst(x, y, w, h, count, 0.40F, 0.80F, 20F, 42F, 0.50F, 0.88F, 1.0F, 2.9F, softCap);
    }

    private void spawnClickBurst(int x, int y, int w, int h) {
        if (!AshwakeClientConfig.hoverParticlesEnabled() || !areUiParticlesEnabledForCurrentScreen()) {
            return;
        }
        float densityScale = Mth.clamp(AshwakeClientConfig.hoverParticleDensity() / 100.0F, 0.0F, 1.0F);
        int count = Mth.clamp(16 + Math.round(densityScale * 12.0F), 16, 28);
        int softCap = 22 + (int) (densityScale * 36);
        particleSystem.spawnBurst(x, y, w, h, count, 0.40F, 0.85F, 30F, 56F, 0.68F, 1.0F, 1.0F, 3.5F, softCap);
    }

    private static boolean areUiParticlesEnabledForCurrentScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return true;
        }
        return !(minecraft.screen instanceof SelectWorldScreen);
    }

    private ResourceLocation selectStateTexture(boolean hovered, boolean pressed) {
        if (!active) {
            return AshwakeUiSkin.BUTTON_DISABLED;
        }
        if (pressed) {
            return AshwakeUiSkin.BUTTON_PRESS;
        }
        if (hovered) {
            return AshwakeUiSkin.BUTTON_HOVER;
        }
        return AshwakeUiSkin.BUTTON_IDLE;
    }

    private static boolean drawTextureIcon(GuiGraphics guiGraphics, int x, int y, Icon icon, boolean active, float hoverProgress) {
        ResourceLocation texture = iconTexture(icon);
        if (texture == null || !AshwakeUiSkin.hasTexture(texture)) {
            return false;
        }

        guiGraphics.blit(
                texture,
                x,
                y,
                0,
                0,
                12,
                12,
                AshwakeUiSkin.ICON_TEXTURE_SIZE,
                AshwakeUiSkin.ICON_TEXTURE_SIZE);
        if (active && hoverProgress > 0.08F) {
            int sparkleAlpha = Mth.clamp((int) (22 * hoverProgress), 0, 22);
            guiGraphics.fill(x + 1, y + 1, x + 10, y + 4, (sparkleAlpha << 24) | 0xFFECC6);
        }
        if (!active) {
            guiGraphics.fill(x, y, x + 12, y + 12, 0x5A0A0908);
        }
        return true;
    }

    private static ResourceLocation iconTexture(Icon icon) {
        return switch (icon) {
            case PLAY -> AshwakeUiSkin.ICON_PLAY;
            case OPTIONS -> AshwakeUiSkin.ICON_OPTIONS;
            case CHANGELOG -> AshwakeUiSkin.ICON_CHANGELOG;
            case DISCORD -> AshwakeUiSkin.ICON_DISCORD;
            case QUIT -> AshwakeUiSkin.ICON_QUIT;
            case GUIDANCE -> AshwakeUiSkin.ICON_GUIDANCE;
            case NONE -> null;
        };
    }

    private void drawFallbackFrame(GuiGraphics guiGraphics, int x, int drawY, int w, int h, boolean hovered, boolean pressed) {
        int top;
        int bottom;
        int borderOuter;
        int borderInner;
        if (!active) {
            top = 0xAA2D2723;
            bottom = 0xAA221D1A;
            borderOuter = 0xFF4C4540;
            borderInner = 0xFF3B3530;
        } else if (pressed) {
            top = 0xEC823F1F;
            bottom = 0xE85A2A15;
            borderOuter = AshwakePalette.EMBER_ORANGE;
            borderInner = AshwakePalette.EMBER_DEEP;
        } else if (hovered) {
            top = 0xF09A4E21;
            bottom = 0xEE5F2B14;
            borderOuter = AshwakePalette.LAVA_YELLOW;
            borderInner = AshwakePalette.EMBER_ORANGE;
        } else {
            top = 0xD44A3C33;
            bottom = 0xCF312924;
            borderOuter = AshwakePalette.BASALT_EDGE;
            borderInner = AshwakePalette.IRON_GRAY;
        }

        guiGraphics.fillGradient(x, drawY, x + w, drawY + h, top, bottom);
        guiGraphics.fill(x, drawY, x + w, drawY + 1, borderOuter);
        guiGraphics.fill(x, drawY + h - 1, x + w, drawY + h, borderOuter);
        guiGraphics.fill(x, drawY, x + 1, drawY + h, borderOuter);
        guiGraphics.fill(x + w - 1, drawY, x + w, drawY + h, borderOuter);

        guiGraphics.fill(x + 1, drawY + 1, x + w - 1, drawY + 2, borderInner);
        guiGraphics.fill(x + 1, drawY + h - 2, x + w - 1, drawY + h - 1, borderInner);
    }

    private static void drawShimmer(GuiGraphics guiGraphics, int x, int y, int w, int h, float hoverProgress) {
        if (hoverProgress <= 0.1F) {
            return;
        }
        int shimmerWidth = 6;
        int sweepSpan = w + 24;
        int sweepX = (int) ((Util.getMillis() / 8L) % sweepSpan) - 12;
        int px = x + sweepX;
        int alpha = Mth.clamp((int) (28 * hoverProgress), 0, 28);
        guiGraphics.fillGradient(px, y + 2, px + shimmerWidth, y + h - 2, (alpha << 24) | 0xFFF3CB, 0x00F3CB);
    }

    private static int tintedByHover(int color, float hoverProgress, float maxBoost) {
        float factor = 1.0F + (hoverProgress * maxBoost);
        int r = Mth.clamp((int) (((color >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((color >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((color & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static final class HoverParticleSystem {
        private static final int MAX_PARTICLES = 96;

        private final boolean[] active = new boolean[MAX_PARTICLES];
        private final float[] x = new float[MAX_PARTICLES];
        private final float[] y = new float[MAX_PARTICLES];
        private final float[] vx = new float[MAX_PARTICLES];
        private final float[] vy = new float[MAX_PARTICLES];
        private final float[] life = new float[MAX_PARTICLES];
        private final float[] maxLife = new float[MAX_PARTICLES];
        private final float[] size = new float[MAX_PARTICLES];
        private final float[] alpha = new float[MAX_PARTICLES];

        private final RandomSource random = RandomSource.create();
        private int cursor;
        private int activeCount;

        void spawnBurst(
                int buttonX,
                int buttonY,
                int buttonWidth,
                int buttonHeight,
                int count,
                float minLife,
                float maxLife,
                float minSpeed,
                float maxSpeed,
                float minAlpha,
                float maxAlpha,
                float minSize,
                float maxSize,
                int softCap) {
            for (int i = 0; i < count; i++) {
                spawnFromTopOrCorners(
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight,
                        minLife,
                        maxLife,
                        minSpeed,
                        maxSpeed,
                        minAlpha,
                        maxAlpha,
                        minSize,
                        maxSize,
                        softCap);
            }
        }

        void spawnAmbient(int buttonX, int buttonY, int buttonWidth, int buttonHeight, int softCap) {
            spawnFromTopOrCorners(buttonX, buttonY, buttonWidth, buttonHeight, 0.45F, 0.9F, 9F, 18F, 0.22F, 0.44F, 1.0F, 2.2F, softCap);
        }

        void updateAndRender(GuiGraphics guiGraphics, float dt) {
            for (int i = 0; i < MAX_PARTICLES; i++) {
                if (!active[i]) {
                    continue;
                }
                life[i] += dt;
                if (life[i] >= maxLife[i]) {
                    active[i] = false;
                    activeCount = Math.max(0, activeCount - 1);
                    continue;
                }

                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
                vy[i] += 28F * dt;

                float lifeRatio = 1.0F - (life[i] / maxLife[i]);
                int drawAlpha = Mth.clamp((int) (alpha[i] * lifeRatio * lifeRatio * 255.0F), 0, 255);
                if (drawAlpha <= 0) {
                    continue;
                }

                int drawSize = Math.max(1, Math.round(size[i]));
                int color = (drawAlpha << 24) | colorFromLife(lifeRatio);
                int drawX = Mth.floor(x[i]);
                int drawY = Mth.floor(y[i]);
                guiGraphics.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, color);
            }
        }

        void clear() {
            for (int i = 0; i < MAX_PARTICLES; i++) {
                active[i] = false;
            }
            activeCount = 0;
            cursor = 0;
        }

        private void spawnFromTopOrCorners(
                int buttonX,
                int buttonY,
                int buttonWidth,
                int buttonHeight,
                float minLife,
                float maxLifeValue,
                float minSpeed,
                float maxSpeed,
                float minAlpha,
                float maxAlphaValue,
                float minSize,
                float maxSizeValue,
                int softCap) {
            int index = nextSlot(softCap);

            float spawnX;
            float spawnY;
            int source = random.nextInt(10);
            if (source < 6) {
                // Mostly top-edge emitters for subtle upward drift.
                spawnX = buttonX + 2 + random.nextFloat() * Math.max(2, buttonWidth - 4);
                spawnY = buttonY + 1 + random.nextFloat() * 2.0F;
            } else if (source < 8) {
                // Top-left corner cluster.
                spawnX = buttonX + 2 + random.nextFloat() * 6.0F;
                spawnY = buttonY + 1 + random.nextFloat() * 4.0F;
            } else {
                // Top-right corner cluster.
                spawnX = buttonX + buttonWidth - 2 - random.nextFloat() * 6.0F;
                spawnY = buttonY + 1 + random.nextFloat() * 4.0F;
            }

            float speed = Mth.lerp(random.nextFloat(), minSpeed, maxSpeed);
            x[index] = spawnX;
            y[index] = spawnY;
            vx[index] = (random.nextFloat() - 0.5F) * speed;
            vy[index] = -(0.6F + random.nextFloat() * 0.8F) * speed;
            life[index] = 0F;
            maxLife[index] = Mth.lerp(random.nextFloat(), minLife, maxLifeValue);
            size[index] = Mth.lerp(random.nextFloat(), minSize, maxSizeValue);
            alpha[index] = Mth.lerp(random.nextFloat(), minAlpha, maxAlphaValue);
        }

        private int nextSlot(int softCap) {
            if (activeCount < softCap) {
                for (int i = 0; i < MAX_PARTICLES; i++) {
                    int idx = (cursor + i) % MAX_PARTICLES;
                    if (!active[idx]) {
                        cursor = (idx + 1) % MAX_PARTICLES;
                        active[idx] = true;
                        activeCount++;
                        return idx;
                    }
                }
            }

            int idx = cursor;
            cursor = (cursor + 1) % MAX_PARTICLES;
            if (!active[idx]) {
                activeCount++;
            }
            active[idx] = true;
            return idx;
        }

        private static int colorFromLife(float lifeRatio) {
            int from = AshwakePalette.EMBER_ORANGE;
            int to = AshwakePalette.LAVA_YELLOW;
            int r1 = (from >> 16) & 0xFF;
            int g1 = (from >> 8) & 0xFF;
            int b1 = from & 0xFF;
            int r2 = (to >> 16) & 0xFF;
            int g2 = (to >> 8) & 0xFF;
            int b2 = to & 0xFF;
            int r = Mth.clamp((int) Mth.lerp(lifeRatio, r1, r2), 0, 255);
            int g = Mth.clamp((int) Mth.lerp(lifeRatio, g1, g2), 0, 255);
            int b = Mth.clamp((int) Mth.lerp(lifeRatio, b1, b2), 0, 255);
            return (r << 16) | (g << 8) | b;
        }
    }

    private static void drawIcon(GuiGraphics guiGraphics, int x, int y, Icon icon, int color) {
        switch (icon) {
            case PLAY -> {
                for (int i = 0; i < 7; i++) {
                    guiGraphics.fill(x + i, y + 2 + (i / 2), x + i + 1, y + 10 - (i / 2), color);
                }
            }
            case OPTIONS -> {
                guiGraphics.fill(x + 2, y + 2, x + 10, y + 10, color);
                guiGraphics.fill(x + 4, y + 4, x + 8, y + 8, 0xAA1A1513);
                guiGraphics.fill(x, y + 5, x + 12, y + 7, color);
                guiGraphics.fill(x + 5, y, x + 7, y + 12, color);
            }
            case CHANGELOG -> {
                guiGraphics.fill(x + 1, y + 1, x + 11, y + 11, color);
                guiGraphics.fill(x + 2, y + 3, x + 10, y + 4, 0xAA1A1513);
                guiGraphics.fill(x + 2, y + 6, x + 9, y + 7, 0xAA1A1513);
                guiGraphics.fill(x + 2, y + 8, x + 8, y + 9, 0xAA1A1513);
            }
            case DISCORD -> {
                guiGraphics.fill(x + 1, y + 2, x + 11, y + 10, color);
                guiGraphics.fill(x + 3, y + 10, x + 6, y + 12, color);
                guiGraphics.fill(x + 3, y + 5, x + 4, y + 6, 0xAA1A1513);
                guiGraphics.fill(x + 8, y + 5, x + 9, y + 6, 0xAA1A1513);
            }
            case QUIT -> {
                guiGraphics.fill(x + 1, y + 1, x + 8, y + 11, color);
                guiGraphics.fill(x + 2, y + 2, x + 7, y + 10, 0xAA1A1513);
                guiGraphics.fill(x + 8, y + 5, x + 11, y + 7, color);
            }
            case GUIDANCE -> {
                guiGraphics.fill(x + 1, y + 1, x + 11, y + 11, color);
                guiGraphics.fill(x + 3, y + 3, x + 9, y + 4, 0xAA1A1513);
                guiGraphics.fill(x + 3, y + 6, x + 9, y + 7, 0xAA1A1513);
                guiGraphics.fill(x + 5, y + 8, x + 7, y + 10, 0xAA1A1513);
            }
            case NONE -> {
            }
        }
    }
}
