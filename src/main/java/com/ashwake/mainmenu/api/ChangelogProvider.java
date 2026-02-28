package com.ashwake.mainmenu.api;

@FunctionalInterface
public interface ChangelogProvider {
    void contribute(ChangelogCollector collector);
}