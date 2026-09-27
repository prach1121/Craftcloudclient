package com.craftcloudclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Save/Load/Rename for named config profiles - full snapshots of every
 * {@link ModConfig} field, stored as individual JSON files under
 * .minecraft/config/craftcloudclient_profiles/.
 *
 * Each file's on-disk name is a one-time, collision-safe slug generated
 * at Save time; the player-facing name lives inside the file itself (see
 * {@link ConfigProfile#name}), so Rename only ever rewrites that one
 * field and never touches - or can collide on - the file path.
 *
 * Save/Load both operate on the single live {@link ModConfig#INSTANCE}:
 * Save snapshots whatever is currently active (including any unsaved
 * edits made this session), and Load replaces the live instance outright.
 * Every HUD module/manager in the mod reads {@code ModConfig.INSTANCE}
 * fresh each frame/tick rather than caching it, so a loaded profile takes
 * effect immediately with no restart required.
 */
public final class ProfileManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("craftcloudclient_profiles");

    private ProfileManager() {
    }

    /** One entry in the profile list: the file on disk plus the display name read from inside it. */
    public record ProfileEntry(Path file, String name) {
    }

    /** Every saved profile, sorted alphabetically (case-insensitive) by display name for a stable UI list. */
    public static List<ProfileEntry> list() {
        List<ProfileEntry> entries = new ArrayList<>();
        if (!Files.isDirectory(DIR)) {
            return entries;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIR, "*.json")) {
            for (Path path : stream) {
                readProfile(path).ifPresent(profile -> entries.add(new ProfileEntry(path, profile.name)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        entries.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
        return entries;
    }

    /**
     * Saves a snapshot of the current live settings under the given
     * display name. If a profile with that exact display name (case
     * insensitive) already exists, its file is overwritten in place
     * instead of creating a duplicate, so re-saving over an existing
     * profile doesn't orphan it under a second file.
     */
    public static void save(String name) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            return;
        }
        Optional<ProfileEntry> existing = findByName(trimmedName);
        Path target = existing.map(ProfileEntry::file).orElseGet(() -> DIR.resolve(uniqueFileName(trimmedName)));
        writeProfile(target, new ConfigProfile(trimmedName, ModConfig.INSTANCE));
    }

    /**
     * Replaces the live config outright with the given profile's
     * snapshot and returns true, or returns false (leaving the live
     * config untouched) if the file is missing or unreadable.
     *
     * The caller is still responsible for saving the main config file
     * afterward so the loaded settings persist across a restart, for
     * re-running any live-apply side effects the individual toggle rows
     * would normally trigger, and for rebuilding any open config screen
     * so its widgets reflect the newly-loaded values.
     */
    public static boolean load(ProfileEntry entry) {
        Optional<ConfigProfile> profile = readProfile(entry.file());
        if (profile.isEmpty() || profile.get().config == null) {
            return false;
        }
        ModConfig.INSTANCE = profile.get().config;
        return true;
    }

    /**
     * Renames a profile in place - only the display name field inside
     * its JSON file changes, the file path never does. Returns false
     * (no-op) if the new name is blank, or if it's already used by a
     * *different* profile; renaming a profile to the name it already has
     * is treated as a harmless success.
     */
    public static boolean rename(ProfileEntry entry, String newName) {
        newName = newName.trim();
        if (newName.isEmpty()) {
            return false;
        }
        Optional<ProfileEntry> collision = findByName(newName);
        if (collision.isPresent() && !collision.get().file().equals(entry.file())) {
            return false;
        }
        Optional<ConfigProfile> profile = readProfile(entry.file());
        if (profile.isEmpty()) {
            return false;
        }
        profile.get().name = newName;
        writeProfile(entry.file(), profile.get());
        return true;
    }

    /**
     * Permanently removes a saved profile's file from disk. Returns true
     * if the file was deleted, or false if it was already gone / couldn't
     * be removed - either way the caller should refresh its list.
     */
    public static boolean delete(ProfileEntry entry) {
        try {
            return Files.deleteIfExists(entry.file());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Optional<ProfileEntry> findByName(String name) {
        for (ProfileEntry entry : list()) {
            if (entry.name().equalsIgnoreCase(name)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /** A filesystem-safe, collision-free slug for a brand-new profile's file. The display name lives inside the file, so this is never touched again after creation - renaming never needs to regenerate it. */
    private static String uniqueFileName(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        base = base.replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "profile";
        }
        String candidate = base;
        int suffix = 1;
        while (Files.exists(DIR.resolve(candidate + ".json"))) {
            candidate = base + "-" + (++suffix);
        }
        return candidate + ".json";
    }

    private static Optional<ConfigProfile> readProfile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ConfigProfile profile = GSON.fromJson(reader, ConfigProfile.class);
            if (profile != null && profile.name != null && profile.config != null) {
                return Optional.of(profile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private static void writeProfile(Path path, ConfigProfile profile) {
        try {
            Files.createDirectories(DIR);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(profile, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
