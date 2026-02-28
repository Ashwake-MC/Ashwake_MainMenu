package com.ashwake.mainmenu.client;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.client.screen.AshwakeOnboardingScreen;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

public final class AshwakeClientRuntime {
    private static boolean loadingOverlayShown;
    private static boolean onboardingShownThisSession;

    private AshwakeClientRuntime() {
    }

    public static void onMainMenuDisplayed(Minecraft minecraft, Screen returnScreen) {
        if (!loadingOverlayShown) {
            loadingOverlayShown = true;
        }

        if (shouldShowOnboarding() && !onboardingShownThisSession) {
            onboardingShownThisSession = true;
            minecraft.tell(() -> minecraft.setScreen(new AshwakeOnboardingScreen(returnScreen)));
        }
    }

    public static boolean shouldShowOnboarding() {
        if (!AshwakeClientConfig.onboardingEnabled()) {
            return false;
        }

        if (!AshwakeClientConfig.onboardingCompleted()) {
            return true;
        }

        if (!AshwakeClientConfig.showOnMajorUpdate()) {
            return false;
        }

        String currentMajor = getCurrentMajorVersion();
        return !currentMajor.equals(AshwakeClientConfig.lastSeenMajorVersion());
    }

    public static void markOnboardingCompleted() {
        AshwakeClientConfig.setOnboardingCompleted(true);
        AshwakeClientConfig.setLastSeenMajorVersion(getCurrentMajorVersion());
        AshwakeClientConfig.save();
    }

    public static String getCurrentMajorVersion() {
        String modVersion = ModList.get().getModContainerById(AshwakeMainMenuMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0.0.0");
        return extractMajor(modVersion);
    }

    private static String extractMajor(String version) {
        int dotIndex = version.indexOf('.');
        if (dotIndex < 0) {
            return version;
        }
        return version.substring(0, dotIndex);
    }
}
