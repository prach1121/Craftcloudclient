package com.craftcloudclient.client.waypoint;

import net.minecraft.client.MinecraftClient;

/** Small helper for turning a dimension registry key into something to store/display. */
public final class DimensionUtil {

    private DimensionUtil() {}

    /** e.g. "minecraft:overworld" - "unknown" if not currently in a world. */
    public static String currentDimensionKey(MinecraftClient client) {
        return (client.world != null) ? client.world.getRegistryKey().getValue().toString() : "unknown";
    }

    /** Short human label for a dimension key, e.g. "minecraft:the_nether" -&gt; "Nether". */
    public static String label(String dimensionKey) {
        if (dimensionKey == null) return "Unknown";
        return switch (dimensionKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> {
                int idx = dimensionKey.indexOf(':');
                yield idx >= 0 ? dimensionKey.substring(idx + 1) : dimensionKey;
            }
        };
    }
}
