package com.ashwake.mainmenu.client.overlay;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.client.render.AshwakeBranding;
import com.ashwake.mainmenu.client.render.AshwakeEmberSystem;
import com.ashwake.mainmenu.client.render.AshwakePalette;
import com.ashwake.mainmenu.client.render.AshwakeUiSkin;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;

public final class AshwakeLoadingOverlay extends LoadingOverlay {
    private static final long TIP_ROTATE_MS = 7000L;
    private static final float SMOOTHING = 0.10F;
    private static final int PANEL_MAX_WIDTH = 520;

    @Nullable
    private static final Field RELOAD_FIELD = resolveField("reload");
    @Nullable
    private static final Field ON_FINISH_FIELD = resolveField("onFinish");
    @Nullable
    private static final Field FADE_IN_FIELD = resolveField("fadeIn");
    @Nullable
    private static final Field CURRENT_PROGRESS_FIELD = resolveField("currentProgress");
    @Nullable
    private static final Field FADE_OUT_START_FIELD = resolveField("fadeOutStart");
    @Nullable
    private static final Field FADE_IN_START_FIELD = resolveField("fadeInStart");

    private static boolean wrapFailureLogged;
    private static boolean renderFailureLogged;

    private final Minecraft minecraft;
    private final ReloadInstance reloadInstance;
    private final AshwakeEmberSystem emberSystem = new AshwakeEmberSystem();

    private long lastTipSwap = Util.getMillis();
    private int tipIndex;
    private float smoothedProgress;
    private boolean renderFailed;

    public AshwakeLoadingOverlay(
            Minecraft minecraft,
            ReloadInstance reload,
            Consumer<Optional<Throwable>> onFinish,
            boolean fadeIn) {
        super(minecraft, reload, onFinish, fadeIn);
        this.minecraft = minecraft;
        this.reloadInstance = reload;
    }

    public static Overlay wrapVanilla(Overlay overlay) {
        if (!(overlay instanceof LoadingOverlay vanilla)
                || overlay instanceof AshwakeLoadingOverlay
                || !AshwakeClientConfig.loadingOverlayEnabled()) {
            return overlay;
        }

        ReloadInstance reload = readReload(vanilla);
        Consumer<Optional<Throwable>> onFinish = readOnFinish(vanilla);
        Boolean fadeIn = readBoolean(vanilla, FADE_IN_FIELD);
        if (reload == null || onFinish == null || fadeIn == null) {
            logWrapFailureOnce("loading overlay fields unavailable");
            return overlay;
        }

        try {
            AshwakeLoadingOverlay wrapped = new AshwakeLoadingOverlay(Minecraft.getInstance(), reload, onFinish, fadeIn);
            copyField(CURRENT_PROGRESS_FIELD, vanilla, wrapped);
            copyField(FADE_OUT_START_FIELD, vanilla, wrapped);
            copyField(FADE_IN_START_FIELD, vanilla, wrapped);
            return wrapped;
        } catch (Throwable throwable) {
            logWrapFailureOnce("failed creating custom loading overlay", throwable);
            return overlay;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.renderFailed || !AshwakeClientConfig.loadingOverlayEnabled()) {
            return;
        }

        try {
            renderAshwakeContents(guiGraphics, 1.0F);
        } catch (Throwable throwable) {
            this.renderFailed = true;
            this.emberSystem.reset();
            if (!renderFailureLogged) {
                renderFailureLogged = true;
                AshwakeMainMenuMod.LOGGER.warn("Ashwake loading overlay render failed; falling back to vanilla.", throwable);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderAshwakeContents(GuiGraphics guiGraphics, float alpha) {
        long now = Util.getMillis();
        if (now - this.lastTipSwap >= TIP_ROTATE_MS) {
            this.tipIndex++;
            this.lastTipSwap = now;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        float clampedAlpha = Mth.clamp(alpha, 0.0F, 1.0F);

        drawBackground(guiGraphics, width, height, clampedAlpha);
        drawParticles(guiGraphics, width, height);

        int panelWidth = Math.min(PANEL_MAX_WIDTH, width - 64);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height / 2) - 8;
        int panelHeight = AshwakeClientConfig.loadingShowTips() ? 120 : 104;

        if (AshwakeClientConfig.loadingShowLogo()) {
            int logoTop = Math.max(18, panelY - 128);
            int logoWidth = Math.min(320, width - 80);
            AshwakeBranding.drawCenteredLogo(
                    guiGraphics,
                    this.minecraft.font,
                    width / 2,
                    logoTop,
                    logoWidth,
                    120,
                    0.40F);
        }

        drawProgressPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight, clampedAlpha);
    }

    private void drawBackground(GuiGraphics guiGraphics, int width, int height, float alpha) {
        if (AshwakeClientConfig.loadingUseVolcanoBackground() && AshwakeUiSkin.hasTexture(AshwakeUiSkin.BACKGROUND_MAIN)) {
            AshwakeUiSkin.drawCover(
                    guiGraphics,
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
            guiGraphics.fillGradient(0, 0, width, height, AshwakePalette.ASH_DARK, AshwakePalette.ASH_DARKER);
        }

        int darkAlpha = Mth.clamp(
                (int) ((AshwakeClientConfig.loadingBackgroundDarken() / 100.0F) * 255.0F * alpha),
                0,
                255);
        guiGraphics.fill(0, 0, width, height, darkAlpha << 24);

        if (AshwakeUiSkin.hasTexture(AshwakeUiSkin.GUI_VIGNETTE)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F * alpha);
            guiGraphics.blit(
                    AshwakeUiSkin.GUI_VIGNETTE,
                    0,
                    0,
                    0,
                    0,
                    width,
                    height,
                    AshwakeUiSkin.VIGNETTE_TEXTURE_SIZE,
                    AshwakeUiSkin.VIGNETTE_TEXTURE_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawParticles(GuiGraphics guiGraphics, int width, int height) {
        boolean reducedMotion = AshwakeClientConfig.reducedMotion();
        int density = Mth.clamp(AshwakeClientConfig.loadingParticleDensity(), 0, 100);
        boolean enabled = AshwakeClientConfig.particlesEnabled() && !reducedMotion && density > 0;
        this.emberSystem.updateAndRender(guiGraphics, width, height, enabled, density, reducedMotion);
    }

    private void drawProgressPanel(GuiGraphics guiGraphics, int panelX, int panelY, int panelWidth, int panelHeight, float alpha) {
        if (AshwakeUiSkin.hasTexture(AshwakeUiSkin.PANEL_FRAME)) {
            AshwakeUiSkin.drawNineSlice(guiGraphics, AshwakeUiSkin.PANEL_FRAME, panelX, panelY, panelWidth, panelHeight);
        } else {
            guiGraphics.fillGradient(
                    panelX,
                    panelY,
                    panelX + panelWidth,
                    panelY + panelHeight,
                    0xDE181311,
                    0xCE110D0C);
        }

        int panelFillAlpha = Mth.clamp((int) (170 * alpha), 0, 255);
        guiGraphics.fill(
                panelX + 2,
                panelY + 2,
                panelX + panelWidth - 2,
                panelY + panelHeight - 2,
                (panelFillAlpha << 24) | 0x0E0B0A);

        Component status = resolveStatus();
        int titleY = panelY + 16;
        guiGraphics.drawCenteredString(this.minecraft.font, status, panelX + (panelWidth / 2), titleY, AshwakePalette.BONE_WHITE);

        float rawProgress = Mth.clamp(this.reloadInstance.getActualProgress(), 0.0F, 1.0F);
        this.smoothedProgress = Mth.clamp(
                Mth.lerp(SMOOTHING, this.smoothedProgress, rawProgress),
                0.0F,
                1.0F);

        int progressPercent = Math.round(this.smoothedProgress * 100.0F);
        Component subStatus = Component.literal(progressPercent + "% " + LoadingDotsText.get(Util.getMillis()));
        guiGraphics.drawCenteredString(
                this.minecraft.font,
                subStatus,
                panelX + (panelWidth / 2),
                titleY + 14,
                AshwakePalette.MUTED_TEXT);

        int barX = panelX + 24;
        int barY = panelY + 46;
        int barWidth = panelWidth - 48;
        int barHeight = 16;
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, rawProgress);

        if (AshwakeClientConfig.loadingShowTips()) {
            guiGraphics.drawCenteredString(
                    this.minecraft.font,
                    Component.literal(currentTip()),
                    panelX + (panelWidth / 2),
                    barY + 26,
                    AshwakePalette.MUTED_TEXT);
        }
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int barX, int barY, int barWidth, int barHeight, float rawProgress) {
        if (AshwakeUiSkin.hasTexture(AshwakeUiSkin.PANEL_FRAME)) {
            AshwakeUiSkin.drawNineSlice(guiGraphics, AshwakeUiSkin.PANEL_FRAME, barX - 2, barY - 2, barWidth + 4, barHeight + 4);
        }

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xE0120F0D);

        int fillLeft = barX + 1;
        int fillRightMax = barX + barWidth - 1;
        int fillTop = barY + 1;
        int fillBottom = barY + barHeight - 1;

        if (rawProgress > 0.001F || this.reloadInstance.isDone()) {
            int fillWidth = Mth.clamp((int) ((barWidth - 2) * this.smoothedProgress), 2, barWidth - 2);
            guiGraphics.fillGradient(
                    fillLeft,
                    fillTop,
                    fillLeft + fillWidth,
                    fillBottom,
                    AshwakePalette.EMBER_ORANGE,
                    AshwakePalette.EMBER_DEEP);
        } else {
            int segmentWidth = Math.max(20, barWidth / 4);
            int cycle = barWidth + segmentWidth;
            int offset = (int) ((Util.getMillis() / 8L) % cycle) - segmentWidth;
            int segmentLeft = Mth.clamp(fillLeft + offset, fillLeft, fillRightMax);
            int segmentRight = Mth.clamp(segmentLeft + segmentWidth, fillLeft, fillRightMax);
            guiGraphics.fillGradient(
                    segmentLeft,
                    fillTop,
                    segmentRight,
                    fillBottom,
                    AshwakePalette.LAVA_YELLOW,
                    AshwakePalette.EMBER_ORANGE);
        }

        guiGraphics.fill(barX, barY, barX + barWidth, barY + 1, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(barX, barY, barX + 1, barY + barHeight, AshwakePalette.BASALT_EDGE);
        guiGraphics.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, AshwakePalette.BASALT_EDGE);
    }

    private Component resolveStatus() {
        if (this.reloadInstance.isDone()) {
            return Component.translatable("menu.ashwake.loading.finalizing");
        }

        Screen screen = this.minecraft.screen;
        if (screen == null) {
            return Component.translatable("menu.ashwake.loading.preparing");
        }

        String key = translatableKey(screen.getTitle());
        if (key != null) {
            String lowerKey = key.toLowerCase(Locale.ROOT);
            if (lowerKey.contains("connect") || lowerKey.contains("join")) {
                return Component.translatable("menu.ashwake.loading.joiningServer");
            }
            if (lowerKey.contains("terrain") || lowerKey.contains("receiving")) {
                return Component.translatable("menu.ashwake.loading.buildingTerrain");
            }
            if (lowerKey.contains("world") || lowerKey.contains("loading")) {
                return Component.translatable("menu.ashwake.loading.loadingWorld");
            }
            if (lowerKey.contains("resource") || lowerKey.contains("pack")) {
                return Component.translatable("menu.ashwake.loading.preparing");
            }
        }

        String screenName = screen.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (screenName.contains("connect")) {
            return Component.translatable("menu.ashwake.loading.joiningServer");
        }
        if (screenName.contains("receiving") || screenName.contains("terrain")) {
            return Component.translatable("menu.ashwake.loading.buildingTerrain");
        }
        if (screenName.contains("level") || screenName.contains("progress")) {
            return Component.translatable("menu.ashwake.loading.loadingWorld");
        }
        return Component.translatable("menu.ashwake.loading.preparing");
    }

    private String currentTip() {
        List<? extends String> tips = AshwakeClientConfig.loadingTips();
        if (tips == null || tips.isEmpty()) {
            return "Ash settles... but danger never does.";
        }
        return tips.get(Math.floorMod(this.tipIndex, tips.size()));
    }

    @Nullable
    private static String translatableKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }

    @Nullable
    private static ReloadInstance readReload(LoadingOverlay overlay) {
        if (RELOAD_FIELD == null) {
            return null;
        }
        try {
            Object value = RELOAD_FIELD.get(overlay);
            return value instanceof ReloadInstance reload ? reload : null;
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Consumer<Optional<Throwable>> readOnFinish(LoadingOverlay overlay) {
        if (ON_FINISH_FIELD == null) {
            return null;
        }
        try {
            Object value = ON_FINISH_FIELD.get(overlay);
            return value instanceof Consumer<?> consumer ? (Consumer<Optional<Throwable>>) consumer : null;
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    @Nullable
    private static Boolean readBoolean(LoadingOverlay overlay, @Nullable Field field) {
        if (field == null) {
            return null;
        }
        try {
            return field.getBoolean(overlay);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private static void copyField(@Nullable Field field, LoadingOverlay from, LoadingOverlay to) {
        if (field == null) {
            return;
        }
        try {
            field.set(to, field.get(from));
        } catch (IllegalAccessException ignored) {
        }
    }

    @Nullable
    private static Field resolveField(String name) {
        try {
            Field field = LoadingOverlay.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            return null;
        }
    }

    private static void logWrapFailureOnce(String message) {
        if (wrapFailureLogged) {
            return;
        }
        wrapFailureLogged = true;
        AshwakeMainMenuMod.LOGGER.warn("Ashwake loading overlay disabled for this session: {}", message);
    }

    private static void logWrapFailureOnce(String message, Throwable throwable) {
        if (wrapFailureLogged) {
            return;
        }
        wrapFailureLogged = true;
        AshwakeMainMenuMod.LOGGER.warn("Ashwake loading overlay disabled for this session: {}", message, throwable);
    }
}
