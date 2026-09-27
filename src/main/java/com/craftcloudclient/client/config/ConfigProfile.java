package com.craftcloudclient.client.config;

/**
 * A single named on-disk snapshot of every {@link ModConfig} field, used
 * by the config screen's Profiles tab (Save/Load/Rename).
 *
 * The player-facing {@link #name} is kept as data inside the file rather
 * than derived from the filename, so {@link ProfileManager#rename} only
 * ever rewrites this one field - it never has to rename (and can't
 * collide on) the file itself.
 */
public class ConfigProfile {
    public String name;
    public ModConfig config;

    /** No-arg constructor required for Gson deserialization. */
    public ConfigProfile() {
    }

    public ConfigProfile(String name, ModConfig config) {
        this.name = name;
        this.config = config;
    }
}
