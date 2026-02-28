package com.ashwake.mainmenu.api;

import java.util.Objects;

public record MenuContext(
        String currentScreenId,
        boolean onboardingCompleted,
        String badgeText) {

    public static final MenuContext EMPTY = new MenuContext("", false, "");

    public MenuContext {
        currentScreenId = Objects.requireNonNullElse(currentScreenId, "");
        badgeText = Objects.requireNonNullElse(badgeText, "");
    }
}