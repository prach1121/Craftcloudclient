package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Box;

/**
 * Counts hostile mobs within a fixed radius of the player - a quick
 * "is it safe to stand here" number, the same spirit as the Light Level
 * module but for mobs that already exist rather than ones that could
 * still spawn. Purely a read of already-loaded/rendered client entities;
 * it doesn't reveal anything through walls that render distance and
 * vanilla's own mob-tracking wouldn't already have sent the client.
 */
public class HostileMobModule implements HudModule {

    /** Half-extents of the search box, in blocks - 32 blocks each way covers most render-distance-limited encounters without being a giant always-red counter in busy areas. */
    private static final double RADIUS = 32.0;

    private int count = 0;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            count = 0;
            return;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        Box searchBox = new Box(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS);
        count = client.world.getOtherEntities(client.player, searchBox, entity -> entity instanceof HostileEntity && entity.isAlive()).size();
    }

    @Override
    public String getId() {
        return "hostile_mobs";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showHostileMobs;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.hostileMobsPosition;
    }

    @Override
    public String getText() {
        return "Hostiles: " + count;
    }

    @Override
    public int getAccentColor() {
        if (count == 0) return 0xFF55FF7A;
        if (count <= 3) return 0xFFE8FF55;
        return 0xFFFF5555;
    }
}
