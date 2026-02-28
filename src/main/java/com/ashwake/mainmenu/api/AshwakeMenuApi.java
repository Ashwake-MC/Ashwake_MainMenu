package com.ashwake.mainmenu.api;

import java.util.Objects;

public interface AshwakeMenuApi {
    String API_VERSION = "1.0.0";

    static AshwakeMenuApi get() {
        return AshwakeMenuApiProvider.get();
    }

    void registerMainMenuAction(MenuAction action);

    void registerGuidanceCard(GuidanceCard card);

    default void registerChangelogProvider(ChangelogProvider provider) {
        Objects.requireNonNull(provider, "provider");
        try {
            provider.contribute(this::publishChangelogEntry);
        } catch (Throwable ignored) {
        }
    }

    void publishChangelogEntry(ChangelogEntry entry);

    default AggregatedChangelog getAggregatedChangelog() {
        return AggregatedChangelog.empty();
    }

    default MenuContext getMenuContext() {
        MenuState state = getMenuState();
        return new MenuContext(state.currentScreenId(), state.onboardingCompleted(), state.badgeText());
    }

    void setBadgeText(String text);

    MenuState getMenuState();
}
