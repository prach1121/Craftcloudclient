package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;

/**
 * Simple "X, Y, Z (facing)" readout - the handful of numbers people
 * otherwise have to open the vanilla F3 debug screen for, always visible
 * in a corner panel like every other HUD module instead.
 */
public class CoordsModule implements HudModule {

    private String text = "X: 0, Y: 0, Z: 0";

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        int x = (int) Math.floor(client.player.getX());
        int y = (int) Math.floor(client.player.getY());
        int z = (int) Math.floor(client.player.getZ());
        Direction facing = client.player.getHorizontalFacing();
        text = x + ", " + y + ", " + z + " (" + facingLabel(facing) + ")";
    }

    private String facingLabel(Direction facing) {
        return switch (facing) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            default -> "?";
        };
    }

    @Override
    public String getId() {
        return "coords";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showCoords;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.coordsPosition;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getAccentColor() {
        return 0xFF7AC6FF;
    }
}
