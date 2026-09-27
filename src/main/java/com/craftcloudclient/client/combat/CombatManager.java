package com.craftcloudclient.client.combat;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

/**
 * Tracks a simple client-side "in combat" window: dealing or taking
 * damage refreshes a countdown, and the player is considered "in combat"
 * for as long as that countdown hasn't hit zero. Purely informational -
 * unlike a server-side combat tag, this never blocks any action (item
 * use, teleport commands, logging out, etc.); it only feeds
 * {@link com.craftcloudclient.client.hud.CombatTimerModule}'s display.
 *
 * "Taking damage" is detected the same low-risk way {@link
 * com.craftcloudclient.client.waypoint.DeathTracker} detects death: by
 * polling health once per tick and reacting to a drop, rather than
 * hooking a damage-event mixin - see the comment on
 * {@link com.craftcloudclient.client.hud.CpsModule} for why this client
 * avoids mixins into the interaction/damage pipeline where a simple poll
 * gets the same result.
 */
public final class CombatManager {

    /** How long the "in combat" window stays open after the last hit, in ticks. */
    private static final int COMBAT_DURATION_TICKS = 20 * 8;

    private static int ticksRemaining = 0;
    private static float lastHealth = -1f;

    private CombatManager() {}

    /** Call once during mod init to register the "dealt damage" hook. */
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity != player) {
                markCombat();
            }
            return ActionResult.PASS;
        });
    }

    /** Call once per client tick to poll for "took damage" and count down. */
    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            lastHealth = -1f;
            ticksRemaining = 0;
            return;
        }

        float health = client.player.getHealth();
        if (lastHealth >= 0 && health < lastHealth) {
            markCombat();
        }
        lastHealth = health;

        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    private static void markCombat() {
        ticksRemaining = COMBAT_DURATION_TICKS;
    }

    public static boolean isInCombat() {
        return ticksRemaining > 0;
    }

    public static int getSecondsRemaining() {
        return (ticksRemaining + 19) / 20;
    }
}
