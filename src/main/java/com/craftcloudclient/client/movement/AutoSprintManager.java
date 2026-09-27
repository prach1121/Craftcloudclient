package com.craftcloudclient.client.movement;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Auto Sprint: while enabled, holds the player's sprint state on
 * automatically whenever they're walking forward - the same convenience
 * toggle plenty of clients (and some vanilla-adjacent accessibility
 * options) offer instead of requiring the sprint key to be held the
 * whole time.
 *
 * Deliberately mirrors vanilla's own conditions for *starting* a sprint
 * (food level, not sneaking, not riding, not mid-use-item) rather than
 * just forcing the flag on outright, so this never lets a player sprint
 * anywhere they couldn't have by holding the sprint key themselves - it
 * only saves them from holding it.
 *
 * Ticked every client tick from {@code ClientTickEvents.START_CLIENT_TICK},
 * the same "poll and apply" shape {@link com.craftcloudclient.client.visual.ZoomManager} uses.
 */
public final class AutoSprintManager {

    private AutoSprintManager() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (!ModConfig.INSTANCE.autoSprintEnabled) return;

        // If the player is already holding the sprint key themselves,
        // stay out of the way entirely - vanilla's own input handling
        // has that covered every tick regardless of this manager.
        if (client.options.sprintKey.isPressed()) return;

        boolean movingForward = client.options.forwardKey.isPressed() && !client.options.backKey.isPressed();
        boolean blocked = player.isSneaking() || player.hasVehicle() || player.isUsingItem();
        // Vanilla itself won't let a fresh sprint start at 6 hunger or
        // below - matching that here keeps this a pure convenience,
        // never a gameplay change.
        boolean hasFood = player.getHungerManager().getFoodLevel() > 6;

        if (movingForward && !blocked && hasFood) {
            player.setSprinting(true);
        }
    }
}
