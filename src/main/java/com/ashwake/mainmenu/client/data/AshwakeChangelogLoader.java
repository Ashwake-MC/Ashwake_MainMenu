package com.ashwake.mainmenu.client.data;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.api.AggregatedChangelog;
import com.ashwake.mainmenu.api.AshwakeMenuApi;
import com.ashwake.mainmenu.api.ChangelogEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class AshwakeChangelogLoader {
    private static final ResourceLocation LOCAL_CHANGELOG = ResourceLocation.fromNamespaceAndPath(
            AshwakeMainMenuMod.MOD_ID, "changelog/latest.md");
    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Highlights",
            "Fixes",
            "Known Issues",
            "Pack Changes",
            "Mod Updates",
            "Other");

    public LoadedChangelog load() {
        String markdown = readLocalMarkdown().orElse("");
        Map<String, List<String>> sections = parseLocalSections(markdown);
        AggregatedChangelog aggregated = AshwakeMenuApi.get().getAggregatedChangelog();

        for (ChangelogEntry entry : aggregated.entries()) {
            String category = normalizeCategory(entry.categoryEnum().displayName());
            sections.computeIfAbsent(category, key -> new ArrayList<>()).add(formatEntryLine(entry));
        }

        String version = aggregated.packVersion().isBlank() ? "latest" : aggregated.packVersion();
        boolean hasConfiguredContent = !markdown.isBlank() || !aggregated.entries().isEmpty();
        return new LoadedChangelog(version, sections, hasConfiguredContent);
    }

    private Optional<String> readLocalMarkdown() {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(LOCAL_CHANGELOG);
        if (resource.isEmpty()) {
            return Optional.empty();
        }

        try (InputStream stream = resource.get().open()) {
            return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            AshwakeMainMenuMod.LOGGER.warn("Failed reading local changelog resource {}", LOCAL_CHANGELOG, exception);
            return Optional.empty();
        }
    }

    private Map<String, List<String>> parseLocalSections(String markdown) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        for (String category : DEFAULT_CATEGORIES) {
            sections.put(category, new ArrayList<>());
        }

        String currentCategory = DEFAULT_CATEGORIES.getFirst();
        if (markdown.isBlank()) {
            return sections;
        }

        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("## ")) {
                currentCategory = normalizeCategory(line.substring(3).trim());
                sections.computeIfAbsent(currentCategory, key -> new ArrayList<>());
                continue;
            }

            if (line.startsWith("# ")) {
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("* ")) {
                sections.get(currentCategory).add(line.replaceFirst("^[*-]\\s*", "- "));
                continue;
            }

            if (line.matches("^\\d+\\.\\s+.*")) {
                sections.get(currentCategory).add(line);
                continue;
            }

            sections.get(currentCategory).add("- " + line);
        }

        return sections;
    }

    private static String formatEntryLine(ChangelogEntry entry) {
        String body = stripMarkdown(entry.bodyMarkdown());
        String sourcePrefix = "ashwake_pack".equals(entry.sourceModId())
                || entry.sourceModName().isBlank()
                || entry.sourceModName().equalsIgnoreCase(entry.sourceModId())
                        ? ""
                        : "[" + entry.sourceModName() + "] ";
        return "- " + sourcePrefix + entry.title() + (body.isBlank() ? "" : ": " + body);
    }

    private static String stripMarkdown(String value) {
        return value.replace("`", "")
                .replace("**", "")
                .replace("__", "")
                .replace("*", "")
                .replace("_", "")
                .replace("[", "")
                .replace("]", "")
                .replace("(", "")
                .replace(")", "")
                .trim();
    }

    private static String normalizeCategory(String category) {
        String clean = category == null ? "" : category.trim();
        if (clean.isEmpty()) {
            return DEFAULT_CATEGORIES.getFirst();
        }

        String compact = clean.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
        return switch (compact) {
            case "highlights" -> "Highlights";
            case "fixes" -> "Fixes";
            case "knownissues" -> "Known Issues";
            case "packchanges" -> "Pack Changes";
            case "modupdates" -> "Mod Updates";
            case "other" -> "Other";
            default -> clean;
        };
    }

    public record LoadedChangelog(
            String version,
            Map<String, List<String>> sections,
            boolean hasConfiguredContent) {
    }
}
