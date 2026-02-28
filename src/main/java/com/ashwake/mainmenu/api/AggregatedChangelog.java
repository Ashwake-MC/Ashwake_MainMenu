package com.ashwake.mainmenu.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AggregatedChangelog(
        String packVersion,
        List<ChangelogEntry> entries,
        Map<String, List<ChangelogEntry>> byMod) {

    private static final AggregatedChangelog EMPTY = new AggregatedChangelog("latest", List.of(), Map.of());

    public AggregatedChangelog {
        packVersion = Objects.requireNonNullElse(packVersion, "latest");
        entries = List.copyOf(entries == null ? List.of() : entries);
        byMod = copyByMod(byMod);
    }

    public static AggregatedChangelog empty() {
        return EMPTY;
    }

    private static Map<String, List<ChangelogEntry>> copyByMod(Map<String, List<ChangelogEntry>> byMod) {
        if (byMod == null || byMod.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<String, List<ChangelogEntry>> copied = new LinkedHashMap<>();
        byMod.forEach((modId, list) -> copied.put(modId == null ? "" : modId, List.copyOf(list == null ? List.of() : list)));
        return Collections.unmodifiableMap(copied);
    }
}
