package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Total experience points gained since joining the current world/session -
 * same "session, not lifetime" scope as Playtime and Distance Traveled.
 * Reads the player's total (cumulative) XP counter rather than the
 * level/progress bar, so it keeps counting correctly across level-ups
 * and doesn't reset to a small number every time the bar fills.
 */
public class XpGainedModule implements HudModule {

    private int sessionStartXp = 0;
    private int gained = 0;
    private boolean wasInWorld = false;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inWorld = client.player != null;

        if (inWorld && !wasInWorld) {
            sessionStartXp = client.player.totalExperience;
            gained = 0;
        }
        wasInWorld = inWorld;

        if (!inWorld) return;

        int current = client.player.totalExperience;
        // Clamp at 0 instead of going negative if the player's total XP
        // drops below where the session started (death with XP loss on a
        // server that allows it) - a negative "XP gained this session"
        // reads as broken rather than informative.
        gained = Math.max(0, current - sessionStartXp);
    }

    @Override
    public String getId() {
        return "xp_gained";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showXpGained;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.xpGainedPosition;
    }

    @Override
    public String getText() {
        return "XP Gained: +" + gained;
    }

    @Override
    public int getAccentColor() {
        return 0xFF55FF7A;
    }
}
