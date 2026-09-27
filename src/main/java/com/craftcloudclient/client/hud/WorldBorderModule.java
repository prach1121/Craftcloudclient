package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.border.WorldBorder;

/**
 * Distance from the player to the nearest edge of the world border - the
 * same number vanilla's own F3 debug screen shows ("Nearest border"),
 * just available without opening it. Mostly useful on servers running a
 * shrinking border (SMPs with an event border, minigame arenas, etc.);
 * off by default since most worlds never have a border close enough to
 * matter, same reasoning as the ping/status graphs being opt-in.
 */
public class WorldBorderModule implements HudModule {

    private double distance = Double.MAX_VALUE;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            distance = Double.MAX_VALUE;
            return;
        }
        WorldBorder border = client.world.getWorldBorder();
        distance = border.getDistanceInsideBorder(client.player);
    }

    @Override
    public String getId() {
        return "world_border";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showWorldBorder;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.worldBorderPosition;
    }

    @Override
    public String getText() {
        if (distance == Double.MAX_VALUE) return "Border: --";
        long rounded = Math.round(Math.max(0, distance));
        return "Border: " + rounded + "m";
    }

    @Override
    public int getAccentColor() {
        if (distance <= 100) return 0xFFFF5555;
        if (distance <= 500) return 0xFFE8FF55;
        return 0xFF55FF7A;
    }
}
