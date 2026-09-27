package com.craftcloudclient.client.stats;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * The set of things this client keeps a running count of. Each entry
 * carries its own JSON storage key (kept separate from the enum name so
 * renaming/refactoring the enum later can't silently corrupt
 * {@code craftcloudclient_stats.json}), a short HUD/table label, and the
 * item used as its icon.
 */
public enum StatCategory {
    HITS("hits", "Hits", Items.IRON_SWORD),
    END_CRYSTAL("end_crystal", "Crystals", Items.END_CRYSTAL),
    RESPAWN_ANCHOR("respawn_anchor", "Anchors", Items.RESPAWN_ANCHOR),
    OBSIDIAN("obsidian", "Obsidian", Items.OBSIDIAN),
    KILLS("kills", "Kills", Items.PLAYER_HEAD);

    /** Stable key used in the persisted JSON, independent of enum identifier. */
    public final String key;
    public final String label;
    public final Item icon;

    StatCategory(String key, String label, Item icon) {
        this.key = key;
        this.label = label;
        this.icon = icon;
    }
}
