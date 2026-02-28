package com.ashwake.mainmenu.api;

import java.util.Objects;

public record MenuState(
        String currentScreenId,
        boolean onboardingCompleted,
        String badgeText) {

    public static final MenuState EMPTY = new MenuState("", false, "");

    public MenuState {
        currentScreenId = Objects.requireNonNullElse(currentScreenId, "");
        badgeText = Objects.requireNonNullElse(badgeText, "");
    }
}
