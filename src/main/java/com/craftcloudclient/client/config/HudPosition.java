package com.craftcloudclient.client.config;

/**
 * The four corners a HUD element can be anchored to.
 * Kept simple (corner-based) so the config screen can offer a single
 * "cycle" button per element instead of free-form dragging.
 */
public enum HudPosition {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right");

    private final String displayName;

    HudPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public HudPosition next() {
        HudPosition[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
