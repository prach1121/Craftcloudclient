package com.craftcloudclient.client.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tracks the player's current kill streak (consecutive kills without
 * dying) and the best streak ever reached. Fed by the same two signals
 * already wired up elsewhere - {@link StatsListener}'s kill detection
 * calls {@link #onKill()}, and {@link com.craftcloudclient.client.waypoint.DeathTracker}'s
 * death detection calls {@link #onDeath()} - so this file doesn't need
 * any detection logic of its own, just the counting.
 *
 * Stored at {@code .minecraft/config/craftcloudclient_streak.json}.
 */
public final class KillStreakManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("craftcloudclient_streak.json");

    public static KillStreakManager INSTANCE = load();

    private int currentStreak;
    private int bestStreak;

    private KillStreakManager() {}

    private static KillStreakManager load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                KillStreakManager loaded = GSON.fromJson(reader, KillStreakManager.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new KillStreakManager();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, KillStreakManager.class, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void onKill() {
        currentStreak++;
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }
        save();
    }

    public synchronized void onDeath() {
        if (currentStreak == 0) return; // nothing changed, skip the write
        currentStreak = 0;
        save();
    }

    public synchronized int getCurrentStreak() {
        return currentStreak;
    }

    public synchronized int getBestStreak() {
        return bestStreak;
    }

    /** Only resets the all-time best - the current streak keeps going, same as it would if this button didn't exist. */
    public synchronized void resetBest() {
        bestStreak = currentStreak;
        save();
    }
}
