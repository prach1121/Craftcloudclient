package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Simple session playtime readout - "how long have I been in this world"
 * rather than the account-wide totals vanilla's own statistics screen
 * tracks. Counts client ticks (20/s) since the player last joined a
 * world and resets back to zero the moment they leave, so reconnecting
 * or switching servers starts a fresh session rather than accumulating
 * forever.
 */
public class PlaytimeModule implements HudModule {

    private int ticks = 0;
    private boolean wasInWorld = false;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inWorld = client.player != null;

        // Rising edge (not in a world -> in a world) marks a new session,
        // whether that's the very first join or a reconnect after leaving.
        if (inWorld && !wasInWorld) {
            ticks = 0;
        }
        wasInWorld = inWorld;

        if (inWorld) {
            ticks++;
        }
    }

    private String formatted() {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public String getId() {
        return "playtime";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showPlaytime;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.playtimePosition;
    }

    @Override
    public String getText() {
        return "Playtime: " + formatted();
    }

    @Override
    public int getAccentColor() {
        // Neutral informational color - this isn't a "good/bad" stat like
        // CPS or ping, so it doesn't shift with any threshold.
        return 0xFF6FD1FF;
    }
}
