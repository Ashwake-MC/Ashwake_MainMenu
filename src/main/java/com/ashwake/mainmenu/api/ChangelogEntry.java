package com.ashwake.mainmenu.api;

import java.util.Locale;
import java.util.Objects;

public record ChangelogEntry(
        String sourceModId,
        String sourceModName,
        String version,
        String category,
        String title,
        String bodyMarkdown,
        long timestampEpochMs,
        int sortOrder) {

    public enum Category {
        HIGHLIGHTS,
        FIXES,
        KNOWN_ISSUES,
        PACK_CHANGES,
        MOD_UPDATES,
        OTHER;

        public static Category from(String raw) {
            if (raw == null || raw.isBlank()) {
                return OTHER;
            }

            String normalized = raw.trim()
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return switch (normalized) {
                case "HIGHLIGHTS" -> HIGHLIGHTS;
                case "FIXES" -> FIXES;
                case "KNOWN_ISSUES", "KNOWNISSUES" -> KNOWN_ISSUES;
                case "PACK_CHANGES", "PACKCHANGES" -> PACK_CHANGES;
                case "MOD_UPDATES", "MODUPDATES" -> MOD_UPDATES;
                default -> OTHER;
            };
        }

        public String displayName() {
            return switch (this) {
                case HIGHLIGHTS -> "Highlights";
                case FIXES -> "Fixes";
                case KNOWN_ISSUES -> "Known Issues";
                case PACK_CHANGES -> "Pack Changes";
                case MOD_UPDATES -> "Mod Updates";
                case OTHER -> "Other";
            };
        }
    }

    public ChangelogEntry {
        sourceModId = normalizeSourceModId(sourceModId);
        sourceModName = normalizeSourceModName(sourceModName, sourceModId);
        version = normalizeVersion(version);
        category = Category.from(category).name();
        title = normalizeTitle(title);
        bodyMarkdown = Objects.requireNonNullElse(bodyMarkdown, "").trim();
        timestampEpochMs = Math.max(0L, timestampEpochMs);
    }

    public ChangelogEntry(
            String sourceModId,
            String sourceModName,
            String version,
            Category category,
            String title,
            String bodyMarkdown,
            long timestampEpochMs,
            int sortOrder) {
        this(
                sourceModId,
                sourceModName,
                version,
                category == null ? null : category.name(),
                title,
                bodyMarkdown,
                timestampEpochMs,
                sortOrder);
    }

    public ChangelogEntry(String version, String category, String title, String bodyMarkdown, int sortOrder) {
        this("unknown", "Unknown", version, category, title, bodyMarkdown, 0L, sortOrder);
    }

    public ChangelogEntry(String version, Category category, String title, String bodyMarkdown, int sortOrder) {
        this("unknown", "Unknown", version, category, title, bodyMarkdown, 0L, sortOrder);
    }

    public Category categoryEnum() {
        return Category.from(category);
    }

    private static String normalizeSourceModId(String sourceModId) {
        String clean = Objects.requireNonNullElse(sourceModId, "").trim().toLowerCase(Locale.ROOT);
        return clean.isEmpty() ? "unknown" : clean;
    }

    private static String normalizeSourceModName(String sourceModName, String sourceModId) {
        String clean = Objects.requireNonNullElse(sourceModName, "").trim();
        if (!clean.isEmpty()) {
            return clean;
        }
        return sourceModId == null || sourceModId.isBlank() ? "Unknown" : sourceModId;
    }

    private static String normalizeVersion(String version) {
        String clean = Objects.requireNonNullElse(version, "").trim();
        return clean.isEmpty() ? "unknown" : clean;
    }

    private static String normalizeTitle(String title) {
        String clean = Objects.requireNonNullElse(title, "").trim();
        return clean.isEmpty() ? "Untitled update" : clean;
    }
}
