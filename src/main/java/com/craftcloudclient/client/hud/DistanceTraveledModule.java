package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Running total of blocks traveled this session (any direction - walking,
 * swimming, flying, riding a boat/minecart all count), the same
 * "session, not lifetime" scope as {@link PlaytimeModule} and reset the
 * same way: back to zero on join/rejoin. Large elytra flights or long
 * cart rides can rack this up fast, so it's shown in km once it passes
 * 1000 blocks rather than an ever-growing block count.
 */
public class DistanceTraveledModule implements HudModule {

    private double blocksTraveled = 0;
    private double lastX, lastY, lastZ;
    private boolean hasLastPos = false;
    private boolean wasInWorld = false;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inWorld = client.player != null;

        if (inWorld && !wasInWorld) {
            // Rising edge: fresh session, same as PlaytimeModule.
            blocksTraveled = 0;
            hasLastPos = false;
        }
        wasInWorld = inWorld;

        if (!inWorld) {
            hasLastPos = false;
            return;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        if (hasLastPos) {
            double dx = x - lastX;
            double dy = y - lastY;
            double dz = z - lastZ;
            double delta = Math.sqrt(dx * dx + dy * dy + dz * dz);
            // Ignore teleports (elytra firework bursts and normal movement
            // never cover more than a handful of blocks in a single tick;
            // a jump this large in one tick is a teleport/respawn, not
            // travel, and shouldn't be added to the odometer).
            if (delta < 40.0) {
                blocksTraveled += delta;
            }
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        hasLastPos = true;
    }

    private String formatted() {
        if (blocksTraveled >= 1000) {
            return String.format("%.1fkm", blocksTraveled / 1000.0);
        }
        return Math.round(blocksTraveled) + "m";
    }

    @Override
    public String getId() {
        return "distance_traveled";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showDistanceTraveled;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.distanceTraveledPosition;
    }

    @Override
    public String getText() {
        return "Distance: " + formatted();
    }

    @Override
    public int getAccentColor() {
        return 0xFF7AC6FF;
    }
}
