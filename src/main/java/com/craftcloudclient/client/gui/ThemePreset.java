package com.craftcloudclient.client.gui;

/**
 * The small set of accent-color pairs a player can pick from in the
 * Theme tab. Picking one just overwrites {@link GlassTheme#ACCENT} and
 * {@link GlassTheme#ACCENT_SECONDARY} - since every glass widget reads
 * those two fields at render time (rather than baking a fixed color into
 * itself), the whole menu and HUD retint immediately, no restart needed.
 */
public enum ThemePreset {
    CYAN("Cyan", 0xFF6FD3FF, 0xFFB98BFF),
    VIOLET("Violet", 0xFFB98BFF, 0xFF6FD3FF),
    ROSE("Rose", 0xFFFF6F91, 0xFFFFD166),
    MINT("Mint", 0xFF4CE07A, 0xFF6FD3FF),
    AMBER("Amber", 0xFFFFD166, 0xFFFF6F91);

    public final String label;
    public final int accent;
    public final int secondary;

    ThemePreset(String label, int accent, int secondary) {
        this.label = label;
        this.accent = accent;
        this.secondary = secondary;
    }

    public void apply() {
        GlassTheme.ACCENT = accent;
        GlassTheme.ACCENT_SECONDARY = secondary;
    }

    /** Safe lookup for a config-stored index, falling back to the first preset if out of range. */
    public static ThemePreset fromIndex(int index) {
        ThemePreset[] all = values();
        if (index < 0 || index >= all.length) return all[0];
        return all[index];
    }
}
