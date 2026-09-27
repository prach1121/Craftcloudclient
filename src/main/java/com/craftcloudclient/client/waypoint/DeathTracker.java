package com.craftcloudclient.client.waypoint;

import com.craftcloudclient.client.stats.KillStreakManager;
import net.minecraft.client.MinecraftClient;

/**
 * Watches the local player's health once per client tick and records an
 * automatic death waypoint the moment it crosses from "alive" to "dead"
 * (health drops to 0 or below) - the same moment vanilla itself would pop
 * open the death screen, so the recorded coordinates are exactly where
 * the player was standing when they died, not wherever they respawn.
 *
 * Polled from the main tick loop rather than hooked into a death
 * event/mixin, matching the rest of this client's "avoid guessing at
 * fragile private APIs" approach - see the comment on
 * {@link com.craftcloudclient.client.hud.CpsModule}.
 */
public final class DeathTracker {

    private static boolean wasAlive = true;

    private DeathTracker() {}

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            // Reset so leaving and rejoining a world isn't itself read as
            // an alive -> dead transition the next time a player exists.
            wasAlive = true;
            return;
        }

        boolean aliveNow = client.player.getHealth() > 0.0f;
        if (wasAlive && !aliveNow) {
            WaypointManager.INSTANCE.recordDeath(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    DimensionUtil.currentDimensionKey(client)
            );
            KillStreakManager.INSTANCE.onDeath();
        }
        wasAlive = aliveNow;
    }
}
