package com.ashwake.mainmenu.internal;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.api.AggregatedChangelog;
import com.ashwake.mainmenu.api.AshwakeMenuApi;
import com.ashwake.mainmenu.api.AshwakeMenuApiProvider;
import com.ashwake.mainmenu.api.ChangelogCollector;
import com.ashwake.mainmenu.api.ChangelogEntry;
import com.ashwake.mainmenu.api.ChangelogProvider;
import com.ashwake.mainmenu.api.GuidanceCard;
import com.ashwake.mainmenu.api.MenuAction;
import com.ashwake.mainmenu.api.MenuContext;
import com.ashwake.mainmenu.api.MenuState;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.ModList;

public final class AshwakeMenuApiImpl implements AshwakeMenuApi {
    private static final int PROVIDER_WARN_THRESHOLD_MS = 50;
    private static final String MOD_CHANGELOG_PATH = "ashwake/changes.json";

    private static final Comparator<MenuAction> ACTION_ORDER = Comparator.comparingInt(MenuAction::sortOrder)
            .thenComparing(MenuAction::id);
    private static final Comparator<GuidanceCard> CARD_ORDER = Comparator.comparingInt(GuidanceCard::sortOrder)
            .thenComparing(GuidanceCard::id);
    private static final Comparator<ChangelogEntry> CHANGELOG_ORDER = Comparator.comparingInt(AshwakeMenuApiImpl::categoryRank)
            .thenComparing(Comparator.comparingLong(ChangelogEntry::timestampEpochMs).reversed())
            .thenComparingInt(ChangelogEntry::sortOrder)
            .thenComparing(ChangelogEntry::sourceModId)
            .thenComparing(ChangelogEntry::title);

    private static final AshwakeMenuApiImpl INSTANCE = new AshwakeMenuApiImpl();

    private final CopyOnWriteArrayList<MenuAction> menuActions = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuidanceCard> guidanceCards = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ChangelogEntry> legacyChangelogEntries = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ChangelogEntry> autoDiscoveredChangelogEntries = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ChangelogProvider> changelogProviders = new CopyOnWriteArrayList<>();

    private final Object changelogCacheLock = new Object();
    private final Set<String> loggedProviderFailures = ConcurrentHashMap.newKeySet();
    private final Set<String> loggedAutoDiscoveryFailures = ConcurrentHashMap.newKeySet();

    private volatile MenuContext menuContext = MenuContext.EMPTY;
    private volatile String badgeText = "";
    private volatile AggregatedChangelog cachedChangelog = AggregatedChangelog.empty();
    private volatile boolean changelogDirty = true;
    private volatile boolean autoDiscoveryScanned;
    private volatile String autoDiscoverySignature = "";
    private volatile String changelogModeSnapshot = "";

    private AshwakeMenuApiImpl() {
    }

    public static void bootstrap() {
        AshwakeMenuApiProvider.set(INSTANCE);
    }

    public static AshwakeMenuApiImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public void registerMainMenuAction(MenuAction action) {
        MenuAction safeAction = Objects.requireNonNull(action, "action");
        menuActions.removeIf(existing -> existing.id().equals(safeAction.id()));
        menuActions.add(safeAction);
    }

    @Override
    public void registerGuidanceCard(GuidanceCard card) {
        GuidanceCard safeCard = Objects.requireNonNull(card, "card");
        guidanceCards.removeIf(existing -> existing.id().equals(safeCard.id()));
        guidanceCards.add(safeCard);
    }

    @Override
    public void registerChangelogProvider(ChangelogProvider provider) {
        ChangelogProvider safeProvider = Objects.requireNonNull(provider, "provider");
        changelogProviders.addIfAbsent(safeProvider);
        markChangelogDirty();
    }

    @Override
    public void publishChangelogEntry(ChangelogEntry entry) {
        legacyChangelogEntries.add(Objects.requireNonNull(entry, "entry"));
        markChangelogDirty();
    }

    @Override
    public AggregatedChangelog getAggregatedChangelog() {
        syncConfigSnapshot();

        AggregatedChangelog cached = cachedChangelog;
        if (!changelogDirty) {
            return cached;
        }

        synchronized (changelogCacheLock) {
            if (!changelogDirty) {
                return cachedChangelog;
            }
            cachedChangelog = buildAggregatedChangelog();
            changelogDirty = false;
            return cachedChangelog;
        }
    }

    @Override
    public MenuContext getMenuContext() {
        return menuContext;
    }

    @Override
    public void setBadgeText(String text) {
        badgeText = text == null ? "" : text.trim();
        menuContext = new MenuContext(menuContext.currentScreenId(), menuContext.onboardingCompleted(), badgeText);
    }

    @Override
    public MenuState getMenuState() {
        MenuContext context = menuContext;
        return new MenuState(context.currentScreenId(), context.onboardingCompleted(), context.badgeText());
    }

    public List<MenuAction> getMainMenuActions() {
        List<MenuAction> ordered = new ArrayList<>(menuActions);
        ordered.sort(ACTION_ORDER);
        return ordered;
    }

    public List<GuidanceCard> getGuidanceCards() {
        List<GuidanceCard> ordered = new ArrayList<>(guidanceCards);
        ordered.sort(CARD_ORDER);
        return ordered;
    }

    public List<ChangelogEntry> getChangelogEntries() {
        return getAggregatedChangelog().entries();
    }

    public void updateMenuState(String currentScreenId) {
        menuContext = new MenuContext(
                currentScreenId == null ? "" : currentScreenId,
                AshwakeClientConfig.onboardingCompleted(),
                badgeText);
    }

    public void invokeExternalCallback(String callbackId, Runnable callback) {
        try {
            callback.run();
        } catch (Throwable throwable) {
            AshwakeMainMenuMod.LOGGER.error("Ashwake callback [{}] failed", callbackId, throwable);
        }
    }

    private AggregatedChangelog buildAggregatedChangelog() {
        ensureAutoDiscoveredEntries();

        LinkedHashMap<String, ChangelogEntry> deduped = new LinkedHashMap<>();
        ChangelogCollector collector = entry -> mergeEntry(deduped, sanitizeEntry(entry));

        for (ChangelogEntry entry : autoDiscoveredChangelogEntries) {
            collector.add(entry);
        }

        for (ChangelogEntry entry : legacyChangelogEntries) {
            collector.add(entry);
        }

        for (ChangelogProvider provider : changelogProviders) {
            runProviderSafely(provider, collector);
        }

        List<ChangelogEntry> sortedEntries = new ArrayList<>(deduped.values());
        sortedEntries.sort(CHANGELOG_ORDER);

        Map<String, List<ChangelogEntry>> byMod = groupByMod(sortedEntries);
        return new AggregatedChangelog(resolvePackVersion(), sortedEntries, byMod);
    }

    private static Map<String, List<ChangelogEntry>> groupByMod(List<ChangelogEntry> entries) {
        TreeMap<String, List<ChangelogEntry>> grouped = new TreeMap<>();
        for (ChangelogEntry entry : entries) {
            grouped.computeIfAbsent(entry.sourceModId(), ignored -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    private void runProviderSafely(ChangelogProvider provider, ChangelogCollector collector) {
        String providerId = providerIdentity(provider);
        long startNs = System.nanoTime();
        try {
            provider.contribute(collector);
        } catch (Throwable throwable) {
            if (loggedProviderFailures.add(providerId)) {
                AshwakeMainMenuMod.LOGGER.error("Ashwake changelog provider [{}] failed", providerId, throwable);
            }
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            if (elapsedMs > PROVIDER_WARN_THRESHOLD_MS) {
                AshwakeMainMenuMod.LOGGER.warn(
                        "Ashwake changelog provider [{}] exceeded {}ms budget ({}ms)",
                        providerId,
                        PROVIDER_WARN_THRESHOLD_MS,
                        elapsedMs);
            }
        }
    }

    private static ChangelogEntry sanitizeEntry(ChangelogEntry entry) {
        if (entry == null) {
            return new ChangelogEntry("unknown", "Unknown", "unknown", ChangelogEntry.Category.OTHER, "Untitled update", "", 0L, 0);
        }
        return new ChangelogEntry(
                entry.sourceModId(),
                entry.sourceModName(),
                entry.version(),
                entry.category(),
                entry.title(),
                entry.bodyMarkdown(),
                entry.timestampEpochMs(),
                entry.sortOrder());
    }

    private static void mergeEntry(Map<String, ChangelogEntry> deduped, ChangelogEntry candidate) {
        String key = dedupeKey(candidate);
        ChangelogEntry existing = deduped.get(key);
        if (existing == null
                || candidate.timestampEpochMs() > existing.timestampEpochMs()
                || (candidate.timestampEpochMs() == existing.timestampEpochMs()
                        && candidate.sortOrder() < existing.sortOrder())) {
            deduped.put(key, candidate);
        }
    }

    private static String dedupeKey(ChangelogEntry entry) {
        return (entry.sourceModId() + "|" + entry.version() + "|" + entry.title()).toLowerCase(Locale.ROOT);
    }

    private static String providerIdentity(ChangelogProvider provider) {
        return provider.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(provider));
    }

    private static int categoryRank(ChangelogEntry entry) {
        return switch (entry.categoryEnum()) {
            case HIGHLIGHTS -> 0;
            case FIXES -> 1;
            case KNOWN_ISSUES -> 2;
            case PACK_CHANGES -> 3;
            case MOD_UPDATES -> 4;
            case OTHER -> 5;
        };
    }

    private static String resolvePackVersion() {
        return ModList.get()
                .getModContainerById(AshwakeMainMenuMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("latest");
    }

    public void invalidateChangelogCache(String reason) {
        synchronized (changelogCacheLock) {
            changelogDirty = true;
            autoDiscoveryScanned = false;
            autoDiscoverySignature = "";
            autoDiscoveredChangelogEntries.clear();
        }
        if (reason != null && !reason.isBlank()) {
            AshwakeMainMenuMod.LOGGER.debug("Ashwake changelog cache invalidated: {}", reason);
        }
    }

    private void markChangelogDirty() {
        changelogDirty = true;
    }

    private void syncConfigSnapshot() {
        String currentMode = AshwakeClientConfig.changelogMode().name();
        if (currentMode.equals(changelogModeSnapshot)) {
            return;
        }

        synchronized (changelogCacheLock) {
            if (!currentMode.equals(changelogModeSnapshot)) {
                changelogModeSnapshot = currentMode;
                changelogDirty = true;
            }
        }
    }

    private void ensureAutoDiscoveredEntries() {
        String currentSignature = buildModSignature();
        if (autoDiscoveryScanned && currentSignature.equals(autoDiscoverySignature)) {
            return;
        }

        autoDiscoveredChangelogEntries.clear();

        for (var modInfo : ModList.get().getMods()) {
            String fallbackModId = normalizeModId(modInfo.getModId());
            String fallbackModName = safeString(modInfo.getDisplayName(), fallbackModId);
            String fallbackVersion = safeString(modInfo.getVersion().toString(), "unknown");
            readDiscoveredChangesFile(fallbackModId, fallbackModName, fallbackVersion);
        }

        autoDiscoverySignature = currentSignature;
        autoDiscoveryScanned = true;
    }

    private static String buildModSignature() {
        TreeMap<String, String> mods = new TreeMap<>();
        for (var modInfo : ModList.get().getMods()) {
            mods.put(normalizeModId(modInfo.getModId()), safeString(modInfo.getVersion().toString(), "unknown"));
        }

        StringBuilder signature = new StringBuilder();
        mods.forEach((modId, version) -> signature.append(modId).append('@').append(version).append(';'));
        return signature.toString();
    }

    private void readDiscoveredChangesFile(String fallbackModId, String fallbackModName, String fallbackVersion) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(fallbackModId, MOD_CHANGELOG_PATH);
        var resourceOptional = minecraft.getResourceManager().getResource(location);
        if (resourceOptional.isEmpty()) {
            return;
        }

        Resource resource = resourceOptional.get();
        try (InputStream stream = resource.open();
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("Expected JSON object root");
            }

            JsonObject root = parsed.getAsJsonObject();
            String sourceModId = normalizeModId(readString(root, "modId", fallbackModId));
            String sourceModName = safeString(readString(root, "modName", fallbackModName), fallbackModName);
            String sourceVersion = safeString(readString(root, "version", fallbackVersion), fallbackVersion);
            long defaultTimestamp = readLong(root, "timestamp", 0L);

            JsonArray entries = readArray(root, "entries");
            if (entries == null) {
                return;
            }

            int index = 0;
            for (JsonElement entryElement : entries) {
                if (!entryElement.isJsonObject()) {
                    index++;
                    continue;
                }

                JsonObject entry = entryElement.getAsJsonObject();
                String category = readString(entry, "category", ChangelogEntry.Category.OTHER.name());
                String title = readString(entry, "title", "Untitled update");
                String body = readString(entry, "bodyMarkdown", readString(entry, "body", ""));
                long timestamp = readLong(entry, "timestamp", defaultTimestamp);
                int sortOrder = readInt(entry, "sortOrder", index);
                autoDiscoveredChangelogEntries.add(new ChangelogEntry(
                        sourceModId,
                        sourceModName,
                        sourceVersion,
                        category,
                        title,
                        body,
                        timestamp,
                        sortOrder));
                index++;
            }
        } catch (IOException | RuntimeException exception) {
            if (loggedAutoDiscoveryFailures.add(fallbackModId)) {
                AshwakeMainMenuMod.LOGGER.warn(
                        "Ashwake auto-discovery failed for {}:{}",
                        fallbackModId,
                        MOD_CHANGELOG_PATH,
                        exception);
            }
        }
    }

    private static String readString(JsonObject object, String key, String fallback) {
        if (!object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive()) {
            return fallback;
        }

        return safeString(element.getAsString(), fallback);
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        if (!object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long readLong(JsonObject object, String key, long fallback) {
        if (!object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static JsonArray readArray(JsonObject object, String key) {
        if (!object.has(key)) {
            return null;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            return null;
        }
        return element.getAsJsonArray();
    }

    private static String normalizeModId(String modId) {
        return safeString(modId, "unknown").toLowerCase(Locale.ROOT);
    }

    private static String safeString(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            return fallback == null || fallback.isBlank() ? "unknown" : fallback.trim();
        }
        return cleaned;
    }
}
