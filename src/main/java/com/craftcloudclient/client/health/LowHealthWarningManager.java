package com.craftcloudclient.client.health;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Warns the player - one action-bar line plus a sharper "ding" than
 * {@link com.craftcloudclient.client.items.DurabilityWarningManager}'s,
 * so the two are never confused for each other - the moment their health
 * drops to the low-health threshold. Same "one clean signal, not a
 * spam alarm" shape as the durability warning: it fires once on the way
 * down, then re-arms only once health climbs back above a slightly
 * higher recovery threshold, so regenerating half a heart at a time
 * near the line can't trigger it every tick.
 *
 * Deliberately not configurable beyond a single on/off toggle in
 * settings, matching DurabilityWarningManager's reasoning - a threshold
 * slider would be a lot of new UI for what's meant to just work as a
 * heads-up safety net.
 */
public final class LowHealthWarningManager {

    /** Warn once health drops to this fraction of max health (or below). */
    private static final float WARN_THRESHOLD = 0.25f;

    /** Re-arm only once health climbs back above this fraction, so hovering right at the line can't refire every tick. */
    private static final float REARM_THRESHOLD = 0.40f;

    private static boolean armed = true;

    private LowHealthWarningManager() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            armed = true;
            return;
        }
        if (!ModConfig.INSTANCE.lowHealthWarningEnabled) return;

        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 0) return;
        float fraction = player.getHealth() / maxHealth;

        if (fraction > REARM_THRESHOLD) {
            armed = true;
            return;
        }

        if (armed && fraction <= WARN_THRESHOLD && player.getHealth() > 0) {
            armed = false;
            player.sendMessage(Text.literal("Low health!"), true);
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_PLAYER_HURT, 1.2f));
        }
    }
}
