package com.ashwake.mainmenu.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class AshwakeClientConfig {
    private static final String MAINMENU_GITHUB_URL = "https://github.com/Ashwake-MC/Ashwake_MainMenu";

    public enum ChangelogMode {
        LOCAL_ONLY,
        REMOTE_OK
    }

    public enum PerformancePreset {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum WindowTitleFormat {
        ASHWAKE_PREFIX,
        ASHWAKE_FIXED
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_MENU_REPLACEMENT;
    private static final ModConfigSpec.BooleanValue ANIMATIONS_ENABLED;
    private static final ModConfigSpec.IntValue ANIMATION_INTENSITY;
    private static final ModConfigSpec.BooleanValue PARTICLES_ENABLED;
    private static final ModConfigSpec.IntValue PARTICLE_DENSITY;
    private static final ModConfigSpec.BooleanValue HOVER_PARTICLES_ENABLED;
    private static final ModConfigSpec.IntValue HOVER_PARTICLE_DENSITY;
    private static final ModConfigSpec.IntValue BACKGROUND_DARKEN;
    private static final ModConfigSpec.BooleanValue FORCE_SHARP_BACKGROUND;
    private static final ModConfigSpec.BooleanValue ALLOW_BLUR_FOR_DEBUG;
    private static final ModConfigSpec.BooleanValue HIDE_REALMS_BUTTON;
    private static final ModConfigSpec.BooleanValue SHOW_MODS_BUTTON_DEV_ONLY;
    private static final ModConfigSpec.BooleanValue REDUCED_MOTION;
    private static final ModConfigSpec.EnumValue<PerformancePreset> PERFORMANCE_PRESET;
    private static final ModConfigSpec.BooleanValue DEBUG_UI;

    private static final ModConfigSpec.BooleanValue DISABLE_MENU_BLUR_GLOBALLY;
    private static final ModConfigSpec.BooleanValue DISABLE_BLUR_ON_ASHWAKE_SCREENS;

    private static final ModConfigSpec.ConfigValue<String> DISCORD_URL;

    private static final ModConfigSpec.EnumValue<ChangelogMode> CHANGELOG_MODE;
    private static final ModConfigSpec.ConfigValue<String> CHANGELOG_GITHUB_URL;
    private static final ModConfigSpec.ConfigValue<String> CHANGELOG_REMOTE_URL;
    private static final ModConfigSpec.IntValue CHANGELOG_CACHE_TTL_HOURS;

    private static final ModConfigSpec.BooleanValue ONBOARDING_ENABLED;
    private static final ModConfigSpec.BooleanValue ONBOARDING_SHOW_ON_MAJOR_UPDATE;
    private static final ModConfigSpec.BooleanValue ONBOARDING_COMPLETED;
    private static final ModConfigSpec.ConfigValue<String> ONBOARDING_LAST_SEEN_MAJOR_VERSION;

    private static final ModConfigSpec.BooleanValue WINDOW_TITLE_ENABLED;
    private static final ModConfigSpec.EnumValue<WindowTitleFormat> WINDOW_TITLE_FORMAT;
    private static final ModConfigSpec.BooleanValue SHOW_PACK_VERSION_IN_WINDOW_TITLE;
    private static final ModConfigSpec.BooleanValue WINDOW_ICON_ENABLED;
    private static final ModConfigSpec.ConfigValue<String> WINDOW_ICON_PATH_16;
    private static final ModConfigSpec.ConfigValue<String> WINDOW_ICON_PATH_32;

    private static final ModConfigSpec.BooleanValue LOADING_OVERLAY_ENABLED;
    private static final ModConfigSpec.BooleanValue LOADING_SHOW_LOGO;
    private static final ModConfigSpec.BooleanValue LOADING_SHOW_TIPS;
    private static final ModConfigSpec.IntValue LOADING_PARTICLE_DENSITY;
    private static final ModConfigSpec.IntValue LOADING_BACKGROUND_DARKEN;
    private static final ModConfigSpec.BooleanValue LOADING_USE_VOLCANO_BACKGROUND;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> LOADING_TIPS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("ui");
        ENABLE_MENU_REPLACEMENT = BUILDER.comment("Replace the vanilla title screen with Ashwake main menu.")
                .define("enableMenuReplacement", true);
        ANIMATIONS_ENABLED = BUILDER.define("animationsEnabled", true);
        ANIMATION_INTENSITY = BUILDER.defineInRange("animationIntensity", 80, 0, 100);
        PARTICLES_ENABLED = BUILDER.define("particlesEnabled", true);
        PARTICLE_DENSITY = BUILDER.defineInRange("particleDensity", 70, 0, 100);
        HOVER_PARTICLES_ENABLED = BUILDER.define("hoverParticlesEnabled", true);
        HOVER_PARTICLE_DENSITY = BUILDER.defineInRange("hoverParticleDensity", 70, 0, 100);
        BACKGROUND_DARKEN = BUILDER.defineInRange("backgroundDarken", 45, 0, 100);
        FORCE_SHARP_BACKGROUND = BUILDER.define("forceSharpBackground", true);
        ALLOW_BLUR_FOR_DEBUG = BUILDER.comment("Developer-only toggle. Keep false in release builds.")
                .define("allowBlurForDebug", false);
        HIDE_REALMS_BUTTON = BUILDER.define("hideRealmsButton", true);
        SHOW_MODS_BUTTON_DEV_ONLY = BUILDER.define("showModsButtonDevOnly", true);
        REDUCED_MOTION = BUILDER.define("reducedMotion", false);
        PERFORMANCE_PRESET = BUILDER.defineEnum("performancePreset", PerformancePreset.MEDIUM);
        DEBUG_UI = BUILDER.comment("Show developer-facing UI labels such as sharp-mode status.")
                .define("debugUi", false);
        BUILDER.pop();

        BUILDER.push("compat");
        DISABLE_MENU_BLUR_GLOBALLY = BUILDER.define("disableMenuBlurGlobally", true);
        DISABLE_BLUR_ON_ASHWAKE_SCREENS = BUILDER.define("disableBlurOnAshwakeScreens", true);
        BUILDER.pop();

        BUILDER.push("links");
        DISCORD_URL = BUILDER.define("discordUrl", "https://discord.gg/EXAMPLE");
        BUILDER.pop();

        BUILDER.push("changelog");
        CHANGELOG_MODE = BUILDER.defineEnum("mode", ChangelogMode.LOCAL_ONLY);
        CHANGELOG_GITHUB_URL = BUILDER.define("githubUrl", MAINMENU_GITHUB_URL);
        CHANGELOG_REMOTE_URL = BUILDER.define("remoteUrl", "");
        CHANGELOG_CACHE_TTL_HOURS = BUILDER.defineInRange("cacheTtlHours", 24, 1, 168);
        BUILDER.pop();

        BUILDER.push("onboarding");
        ONBOARDING_ENABLED = BUILDER.define("enabled", true);
        ONBOARDING_SHOW_ON_MAJOR_UPDATE = BUILDER.define("showOnMajorUpdate", true);
        ONBOARDING_COMPLETED = BUILDER.define("completed", false);
        ONBOARDING_LAST_SEEN_MAJOR_VERSION = BUILDER.define("lastSeenMajorVersion", "");
        BUILDER.pop();

        BUILDER.push("branding");
        WINDOW_TITLE_ENABLED = BUILDER.define("windowTitleEnabled", true);
        WINDOW_TITLE_FORMAT = BUILDER.defineEnum("windowTitleFormat", WindowTitleFormat.ASHWAKE_PREFIX);
        SHOW_PACK_VERSION_IN_WINDOW_TITLE = BUILDER.define("showPackVersion", true);
        WINDOW_ICON_ENABLED = BUILDER.define("windowIconEnabled", true);
        WINDOW_ICON_PATH_16 = BUILDER.define("windowIconPath16", "ashwake_mainmenu:icons/icon_16x16.png");
        WINDOW_ICON_PATH_32 = BUILDER.define("windowIconPath32", "ashwake_mainmenu:icons/icon_32x32.png");
        BUILDER.pop();

        BUILDER.push("loading");
        LOADING_OVERLAY_ENABLED = BUILDER.comment("Replace vanilla loading visuals while preserving vanilla loading flow.")
                .define("enabled", true);
        LOADING_SHOW_LOGO = BUILDER.define("showLogo", true);
        LOADING_SHOW_TIPS = BUILDER.define("showTips", true);
        LOADING_PARTICLE_DENSITY = BUILDER.defineInRange("particleDensity", 30, 0, 100);
        LOADING_BACKGROUND_DARKEN = BUILDER.defineInRange("backgroundDarken", 65, 0, 100);
        LOADING_USE_VOLCANO_BACKGROUND = BUILDER.define("useVolcanoBackground", true);
        LOADING_TIPS = BUILDER.comment("Tips rotated in the custom loading overlay.")
                .defineListAllowEmpty(
                        "tips",
                        List.of(
                                "Ash settles... but danger never does.",
                                "Performance tip: reduce particles if you feel stutter.",
                                "Need help? Open Guidance from the main menu."),
                        () -> "",
                        AshwakeClientConfig::isStringValue);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private AshwakeClientConfig() {
    }

    private static boolean isStringValue(Object value) {
        return value instanceof String;
    }

    public static boolean enableMenuReplacement() {
        return ENABLE_MENU_REPLACEMENT.get();
    }

    public static boolean animationsEnabled() {
        return ANIMATIONS_ENABLED.get();
    }

    public static int animationIntensity() {
        return ANIMATION_INTENSITY.get();
    }

    public static boolean particlesEnabled() {
        return PARTICLES_ENABLED.get();
    }

    public static int particleDensity() {
        return PARTICLE_DENSITY.get();
    }

    public static boolean hoverParticlesEnabled() {
        return HOVER_PARTICLES_ENABLED.get();
    }

    public static int hoverParticleDensity() {
        return HOVER_PARTICLE_DENSITY.get();
    }

    public static int backgroundDarken() {
        return BACKGROUND_DARKEN.get();
    }

    public static boolean forceSharpBackground() {
        if (!allowBlurForDebug()) {
            return true;
        }
        return FORCE_SHARP_BACKGROUND.get();
    }

    public static boolean allowBlurForDebug() {
        return ALLOW_BLUR_FOR_DEBUG.get();
    }

    public static boolean hideRealmsButton() {
        return HIDE_REALMS_BUTTON.get();
    }

    public static boolean showModsButtonDevOnly() {
        return SHOW_MODS_BUTTON_DEV_ONLY.get();
    }

    public static boolean reducedMotion() {
        return REDUCED_MOTION.get();
    }

    public static boolean disableMenuBlurGlobally() {
        return DISABLE_MENU_BLUR_GLOBALLY.get();
    }

    public static boolean disableBlurOnAshwakeScreens() {
        return DISABLE_BLUR_ON_ASHWAKE_SCREENS.get();
    }

    public static PerformancePreset performancePreset() {
        return PERFORMANCE_PRESET.get();
    }

    public static boolean debugUi() {
        return DEBUG_UI.get();
    }

    public static String discordUrl() {
        return DISCORD_URL.get().trim();
    }

    public static ChangelogMode changelogMode() {
        return CHANGELOG_MODE.get();
    }

    public static String changelogRemoteUrl() {
        return CHANGELOG_REMOTE_URL.get().trim();
    }

    public static String changelogGithubUrl() {
        String configured = CHANGELOG_GITHUB_URL.get().trim();
        if (configured.isBlank() || configured.contains("ORG/REPO")) {
            return MAINMENU_GITHUB_URL;
        }
        return MAINMENU_GITHUB_URL;
    }

    public static int changelogCacheTtlHours() {
        return CHANGELOG_CACHE_TTL_HOURS.get();
    }

    public static boolean onboardingEnabled() {
        return ONBOARDING_ENABLED.get();
    }

    public static boolean showOnMajorUpdate() {
        return ONBOARDING_SHOW_ON_MAJOR_UPDATE.get();
    }

    public static boolean onboardingCompleted() {
        return ONBOARDING_COMPLETED.get();
    }

    public static String lastSeenMajorVersion() {
        return ONBOARDING_LAST_SEEN_MAJOR_VERSION.get().trim();
    }

    public static boolean windowTitleEnabled() {
        return WINDOW_TITLE_ENABLED.get();
    }

    public static WindowTitleFormat windowTitleFormat() {
        return WINDOW_TITLE_FORMAT.get();
    }

    public static boolean showPackVersionInWindowTitle() {
        return SHOW_PACK_VERSION_IN_WINDOW_TITLE.get();
    }

    public static boolean windowIconEnabled() {
        return WINDOW_ICON_ENABLED.get();
    }

    public static String windowIconPath16() {
        return WINDOW_ICON_PATH_16.get().trim();
    }

    public static String windowIconPath32() {
        return WINDOW_ICON_PATH_32.get().trim();
    }

    public static boolean loadingOverlayEnabled() {
        return LOADING_OVERLAY_ENABLED.get();
    }

    public static boolean loadingShowLogo() {
        return LOADING_SHOW_LOGO.get();
    }

    public static boolean loadingShowTips() {
        return LOADING_SHOW_TIPS.get();
    }

    public static int loadingParticleDensity() {
        return LOADING_PARTICLE_DENSITY.get();
    }

    public static int loadingBackgroundDarken() {
        return LOADING_BACKGROUND_DARKEN.get();
    }

    public static boolean loadingUseVolcanoBackground() {
        return LOADING_USE_VOLCANO_BACKGROUND.get();
    }

    public static List<? extends String> loadingTips() {
        return LOADING_TIPS.get();
    }

    public static void setAnimationsEnabled(boolean value) {
        ANIMATIONS_ENABLED.set(value);
    }

    public static void setAnimationIntensity(int value) {
        ANIMATION_INTENSITY.set(value);
    }

    public static void setParticlesEnabled(boolean value) {
        PARTICLES_ENABLED.set(value);
    }

    public static void setParticleDensity(int value) {
        PARTICLE_DENSITY.set(value);
    }

    public static void setHoverParticlesEnabled(boolean value) {
        HOVER_PARTICLES_ENABLED.set(value);
    }

    public static void setHoverParticleDensity(int value) {
        HOVER_PARTICLE_DENSITY.set(value);
    }

    public static void setBackgroundDarken(int value) {
        BACKGROUND_DARKEN.set(value);
    }

    public static void setForceSharpBackground(boolean value) {
        FORCE_SHARP_BACKGROUND.set(allowBlurForDebug() && value);
    }

    public static void setReducedMotion(boolean value) {
        REDUCED_MOTION.set(value);
    }

    public static void setAllowBlurForDebug(boolean value) {
        ALLOW_BLUR_FOR_DEBUG.set(value);
    }

    public static void setDisableMenuBlurGlobally(boolean value) {
        DISABLE_MENU_BLUR_GLOBALLY.set(value);
    }

    public static void setDisableBlurOnAshwakeScreens(boolean value) {
        DISABLE_BLUR_ON_ASHWAKE_SCREENS.set(value);
    }

    public static void setPerformancePreset(PerformancePreset preset) {
        PERFORMANCE_PRESET.set(preset);
    }

    public static void setDebugUi(boolean value) {
        DEBUG_UI.set(value);
    }

    public static void setChangelogMode(ChangelogMode mode) {
        CHANGELOG_MODE.set(mode);
    }

    public static void setDiscordUrl(String url) {
        DISCORD_URL.set(url == null ? "" : url.trim());
    }

    public static void setChangelogRemoteUrl(String url) {
        CHANGELOG_REMOTE_URL.set(url == null ? "" : url.trim());
    }

    public static void setChangelogGithubUrl(String url) {
        CHANGELOG_GITHUB_URL.set(url == null ? "" : url.trim());
    }

    public static void setOnboardingCompleted(boolean completed) {
        ONBOARDING_COMPLETED.set(completed);
    }

    public static void setLastSeenMajorVersion(String majorVersion) {
        ONBOARDING_LAST_SEEN_MAJOR_VERSION.set(majorVersion == null ? "" : majorVersion.trim());
    }

    public static void applyPerformancePreset(PerformancePreset preset) {
        setPerformancePreset(preset);
        switch (preset) {
            case LOW -> {
                setAnimationsEnabled(true);
                setAnimationIntensity(35);
                setParticlesEnabled(false);
                setParticleDensity(15);
                setHoverParticlesEnabled(true);
                setHoverParticleDensity(35);
                setBackgroundDarken(52);
                setReducedMotion(true);
            }
            case MEDIUM -> {
                setAnimationsEnabled(true);
                setAnimationIntensity(70);
                setParticlesEnabled(true);
                setParticleDensity(55);
                setHoverParticlesEnabled(true);
                setHoverParticleDensity(70);
                setBackgroundDarken(45);
                setReducedMotion(false);
            }
            case HIGH -> {
                setAnimationsEnabled(true);
                setAnimationIntensity(100);
                setParticlesEnabled(true);
                setParticleDensity(90);
                setHoverParticlesEnabled(true);
                setHoverParticleDensity(90);
                setBackgroundDarken(40);
                setReducedMotion(false);
            }
        }
    }

    public static void save() {
        SPEC.save();
    }
}
