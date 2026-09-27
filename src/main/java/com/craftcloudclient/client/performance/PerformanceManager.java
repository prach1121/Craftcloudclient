package com.craftcloudclient.client.performance;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Applies purely client-side, vanilla-supported settings changes to reduce
 * visual load and smooth out frame pacing. Nothing here touches game logic,
 * packets, or gives any gameplay advantage - it only adjusts the same
 * options exposed in vanilla's Video Settings menu, driven by our config.
 */
public final class PerformanceManager {

    private PerformanceManager() {}

    // Remembers whatever the player had set before "FPS Boost" was turned
    // on, so it can be restored exactly when the player turns it back off.
    // Null means boost is not currently active.
    private static Integer preBoostViewDistance = null;
    private static Double preBoostEntityDistanceScale = null;
    private static GraphicsMode preBoostGraphicsMode = null;
    private static Boolean preBoostEntityShadows = null;
    private static Boolean preBoostAo = null;

    // Tracks whether the heavy part of the boost (graphics mode + shadows +
    // AO) has already been pushed to the client this session, so repeat
    // calls to apply() - e.g. from clicking a completely unrelated toggle
    // like "Reduce Particles" - don't redo it. See note in applyFpsBoost().
    private static boolean boostCurrentlyApplied = false;

    public static void apply() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        GameOptions options = client.options;
        ModConfig cfg = ModConfig.INSTANCE;

        if (cfg.reduceParticles) {
            options.getParticles().setValue(ParticlesMode.MINIMAL);
        }

        if (cfg.disableClouds) {
            options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        }

        if (cfg.smoothFpsLimiter) {
            options.getMaxFps().setValue(Math.max(30, Math.min(260, cfg.fpsLimit)));
        }

        applyFpsBoost(options, cfg);
    }

    private static void applyFpsBoost(GameOptions options, ModConfig cfg) {
        if (cfg.fpsBoostEnabled) {
            if (preBoostViewDistance == null) {
                // First activation this session - snapshot the player's
                // current settings so we know what to restore later.
                preBoostViewDistance = options.getViewDistance().getValue();
                preBoostEntityDistanceScale = options.getEntityDistanceScaling().getValue();
                preBoostGraphicsMode = options.getPreset().getValue();
                preBoostEntityShadows = options.getEntityShadows().getValue();
                preBoostAo = options.getAo().getValue();
            }

            // View/entity distance are cheap to keep in sync every call (in
            // case the user edits boostRenderDistance while boost is on).
            int cappedRenderDistance = Math.max(2, Math.min(preBoostViewDistance, cfg.boostRenderDistance));
            options.getViewDistance().setValue(cappedRenderDistance);
            options.getEntityDistanceScaling().setValue(
                    Math.max(0.5d, Math.min(1.0d, cfg.boostEntityDistanceScale)));

            // options.applyGraphicsMode(...) forces Minecraft to
            // synchronously rebuild every loaded chunk's render mesh on the
            // main thread - the same hitch vanilla has when you flip
            // Fancy/Fast in Video Settings, except here it used to fire on
            // EVERY call to apply(), not just when the mode actually
            // changed. That meant clicking any other Performance toggle
            // (Reduce Particles, Disable Clouds, ...) while FPS Boost was
            // already on would silently re-trigger a full chunk rebuild -
            // on a busy multiplayer world with a lot of loaded chunks that's
            // exactly what makes the window freeze / show "Not Responding".
            // Guarding on boostCurrentlyApplied makes this run only on the
            // actual off->on transition.
            if (!boostCurrentlyApplied) {
                options.applyGraphicsMode(GraphicsMode.FAST);
                options.getEntityShadows().setValue(false);
                options.getAo().setValue(false);
                boostCurrentlyApplied = true;
            }
        } else if (preBoostViewDistance != null) {
            // Boost was just turned off - hand the player's original
            // settings back exactly as they were. Same idempotency guard
            // here so this branch (which also calls applyGraphicsMode) only
            // runs once per actual on->off transition too.
            if (boostCurrentlyApplied) {
                options.getViewDistance().setValue(preBoostViewDistance);
                options.getEntityDistanceScaling().setValue(preBoostEntityDistanceScale);
                options.applyGraphicsMode(preBoostGraphicsMode);
                options.getEntityShadows().setValue(preBoostEntityShadows);
                options.getAo().setValue(preBoostAo);
                boostCurrentlyApplied = false;
            }

            preBoostViewDistance = null;
            preBoostEntityDistanceScale = null;
            preBoostGraphicsMode = null;
            preBoostEntityShadows = null;
            preBoostAo = null;
        }
    }
}
