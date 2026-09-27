package com.craftcloudclient.client.visual;

import com.craftcloudclient.client.KeyBindings;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Hold-to-zoom, like using a spyglass without needing one in hand. Purely a
 * camera effect: while the zoom key is held, this temporarily overrides the
 * vanilla FOV option down to {@link ModConfig#zoomFov}, then restores
 * whatever the player had it set to the instant the key is released.
 */
public final class ZoomManager {

    private static boolean zooming = false;
    private static int savedFov = -1;

    private ZoomManager() {
    }

    /** Call once per client tick. */
    public static void tick(MinecraftClient client) {
        ModConfig cfg = ModConfig.INSTANCE;
        boolean holding = cfg.zoomEnabled && client.player != null && KeyBindings.ZOOM.isPressed();

        SimpleOption<Integer> fov = client.options.getFov();

        if (holding && !zooming) {
            savedFov = fov.getValue();
            fov.setValue(cfg.zoomFov);
            zooming = true;
        } else if (!holding && zooming) {
            restore(fov);
        }
    }

    /** Safety net: called on disconnect/screen changes so a stuck zoom can never persist. */
    public static void reset(MinecraftClient client) {
        if (zooming) {
            restore(client.options.getFov());
        }
    }

    private static void restore(SimpleOption<Integer> fov) {
        if (savedFov >= 0) {
            fov.setValue(savedFov);
        }
        zooming = false;
        savedFov = -1;
    }

    public static boolean isZooming() {
        return zooming;
    }
}
