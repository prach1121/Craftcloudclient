package com.craftcloudclient.client.health;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Warns the player - one action-bar line plus a soft "bite" sound - the
 * moment their hunger bar drops low enough that sprinting is about to be
 * disabled (6 food points, the same threshold vanilla itself uses), so it
 * doubles as an early heads-up for {@link com.craftcloudclient.client.movement.AutoSprintManager}
 * users who might otherwise be confused about why auto-sprint just
 * stopped engaging.
 *
 * Same "one clean signal, not a spam alarm" shape as
 * {@link LowHealthWarningManager} and {@link com.craftcloudclient.client.items.DurabilityWarningManager}:
 * it fires once on the way down, then re-arms only once food climbs back
 * up a couple points, so eating a single bite right at the threshold
 * can't refire it every tick.
 */
public final class HungerWarningManager {

    /** Warn once food level drops to this value (or below), out of 20. */
    private static final int WARN_THRESHOLD = 6;

    /** Re-arm only once food climbs back above this value, so hovering right at the line can't refire every tick. */
    private static final int REARM_THRESHOLD = 8;

    private static boolean armed = true;

    private HungerWarningManager() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            armed = true;
            return;
        }
        if (!ModConfig.INSTANCE.hungerWarningEnabled) return;

        int foodLevel = player.getHungerManager().getFoodLevel();

        if (foodLevel > REARM_THRESHOLD) {
            armed = true;
            return;
        }

        if (armed && foodLevel <= WARN_THRESHOLD) {
            armed = false;
            player.sendMessage(Text.literal("Getting hungry!"), true);
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_GENERIC_EAT, 1.0f));
        }
    }
}
