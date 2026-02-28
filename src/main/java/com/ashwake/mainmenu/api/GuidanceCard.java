package com.ashwake.mainmenu.api;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;

public record GuidanceCard(
        String id,
        String titleKey,
        String bodyKey,
        String iconTexture,
        int sortOrder,
        CardAction action) {

    public GuidanceCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(titleKey, "titleKey");
        Objects.requireNonNull(bodyKey, "bodyKey");
        Objects.requireNonNull(action, "action");
        if (iconTexture == null) {
            iconTexture = "";
        }
    }

    public sealed interface CardAction permits OpenUrl, OpenScreen, OpenMinecraftScreen, RunTask {
    }

    public record OpenUrl(String url) implements CardAction {
        public OpenUrl {
            Objects.requireNonNull(url, "url");
        }
    }

    public record OpenScreen(String screenId) implements CardAction {
        public OpenScreen {
            Objects.requireNonNull(screenId, "screenId");
        }
    }

    public record OpenMinecraftScreen(Function<Screen, Screen> screenFactory) implements CardAction {
        public OpenMinecraftScreen {
            Objects.requireNonNull(screenFactory, "screenFactory");
        }
    }

    public record RunTask(Runnable task) implements CardAction {
        public RunTask {
            if (task == null) {
                task = () -> {};
            }
        }
    }
}
