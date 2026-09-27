package com.craftcloudclient.client.config;

/**
 * Quick-pick anchor for the {@code Server Banner} only (see
 * {@link ModConfig#serverBannerPosition}). Kept separate from
 * {@link HudPosition} - which the corner-grouped module panels use -
 * because the banner is a single centered pill, not a corner panel, and
 * needs a "center" option those panels have no use for.
 */
public enum BannerPosition {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right");

    private final String displayName;

    BannerPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BannerPosition next() {
        BannerPosition[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
