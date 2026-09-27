package com.craftcloudclient.client.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persistent hit / placement / kill counters, bucketed by calendar day so
 * "today / this week / this month / this year / all-time" totals can be
 * re-aggregated on demand from a single source of truth, instead of
 * keeping five separate running counters per category that could drift
 * out of sync with each other (e.g. after the game is left open across
 * midnight).
 *
 * Stored at {@code .minecraft/config/craftcloudclient_stats.json} as:
 * <pre>
 * { "2026-07-16": { "hits": 42, "kills": 3, ... }, "2026-07-15": {...} }
 * </pre>
 */
public final class StatsManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("craftcloudclient_stats.json");
    private static final Type DATA_TYPE = new TypeToken<TreeMap<String, Map<String, Integer>>>() {}.getType();

    /** How long to let changes sit in memory before writing them to disk. */
    private static final long SAVE_INTERVAL_MS = 5000L;

    public static StatsManager INSTANCE = load();

    // day ("yyyy-MM-dd") -> category key -> count
    private final Map<String, Map<String, Integer>> byDay;

    private boolean dirty = false;
    private long lastSaveMs = System.currentTimeMillis();

    private StatsManager(Map<String, Map<String, Integer>> byDay) {
        this.byDay = byDay;
    }

    public static StatsManager load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                Map<String, Map<String, Integer>> data = GSON.fromJson(reader, DATA_TYPE);
                if (data != null) {
                    return new StatsManager(new TreeMap<>(data));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new StatsManager(new TreeMap<>());
    }

    public synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(byDay, DATA_TYPE, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        dirty = false;
        lastSaveMs = System.currentTimeMillis();
    }

    /** Called once per client tick; flushes to disk periodically rather than on every single record(). */
    public synchronized void tick() {
        if (dirty && System.currentTimeMillis() - lastSaveMs > SAVE_INTERVAL_MS) {
            save();
        }
    }

    public void record(StatCategory category) {
        record(category, 1);
    }

    public synchronized void record(StatCategory category, int amount) {
        String today = LocalDate.now().toString();
        Map<String, Integer> day = byDay.computeIfAbsent(today, k -> new LinkedHashMap<>());
        day.merge(category.key, amount, Integer::sum);
        dirty = true;
    }

    public synchronized int getCount(StatCategory category, StatPeriod period) {
        LocalDate today = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
        int targetWeek = today.get(wf.weekOfWeekBasedYear());
        int targetWeekYear = today.get(wf.weekBasedYear());

        int total = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : byDay.entrySet()) {
            LocalDate date;
            try {
                date = LocalDate.parse(entry.getKey());
            } catch (Exception e) {
                continue; // ignore any malformed/foreign key rather than crash the menu
            }

            boolean matches = switch (period) {
                case TODAY -> date.equals(today);
                case WEEK -> date.get(wf.weekOfWeekBasedYear()) == targetWeek
                        && date.get(wf.weekBasedYear()) == targetWeekYear;
                case MONTH -> date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
                case YEAR -> date.getYear() == today.getYear();
                case ALL_TIME -> true;
            };

            if (matches) {
                total += entry.getValue().getOrDefault(category.key, 0);
            }
        }
        return total;
    }

    /** Wipes every recorded stat and saves immediately. Used by the "Reset Stats" button. */
    public synchronized void resetAll() {
        byDay.clear();
        save();
    }
}
