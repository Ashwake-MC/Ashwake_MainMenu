package com.ashwake.mainmenu.api;

import java.util.Objects;

public record MenuAction(
        String id,
        String labelKey,
        String iconTexture,
        int sortOrder,
        Runnable onClick) {

    public MenuAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(labelKey, "labelKey");
        if (iconTexture == null) {
            iconTexture = "";
        }
        if (onClick == null) {
            onClick = () -> {};
        }
    }
}
