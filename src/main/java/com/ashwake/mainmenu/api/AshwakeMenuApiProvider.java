package com.ashwake.mainmenu.api;

public final class AshwakeMenuApiProvider {
    private static final AshwakeMenuApi NOOP = new NoOpAshwakeMenuApi();
    private static volatile AshwakeMenuApi instance = NOOP;

    private AshwakeMenuApiProvider() {
    }

    public static AshwakeMenuApi get() {
        return instance;
    }

    public static void set(AshwakeMenuApi api) {
        instance = api == null ? NOOP : api;
    }

    public static void install(AshwakeMenuApi api) {
        set(api);
    }

    private static final class NoOpAshwakeMenuApi implements AshwakeMenuApi {
        private volatile String badgeText = "";

        @Override
        public void registerMainMenuAction(MenuAction action) {
        }

        @Override
        public void registerGuidanceCard(GuidanceCard card) {
        }

        @Override
        public void registerChangelogProvider(ChangelogProvider provider) {
        }

        @Override
        public void publishChangelogEntry(ChangelogEntry entry) {
        }

        @Override
        public AggregatedChangelog getAggregatedChangelog() {
            return AggregatedChangelog.empty();
        }

        @Override
        public MenuContext getMenuContext() {
            return new MenuContext("", false, badgeText);
        }

        @Override
        public void setBadgeText(String text) {
            badgeText = text == null ? "" : text;
        }

        @Override
        public MenuState getMenuState() {
            MenuContext context = getMenuContext();
            return new MenuState(context.currentScreenId(), context.onboardingCompleted(), context.badgeText());
        }
    }
}
