package com.ashwake.mainmenu.api;

@FunctionalInterface
public interface ChangelogCollector {
    void add(ChangelogEntry entry);
}