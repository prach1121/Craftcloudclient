package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

/**
 * Shows the block-light level at the player's feet - the same 0-15 value
 * that governs whether hostile mobs can spawn there - so base-proofing
 * doesn't require opening the F3 debug screen. Colored the same way a
 * builder would eyeball it: green once it's bright enough that nothing
 * spawns, yellow in the "technically still safe but close" band, red once
 * it's dark enough for mobs.
 */
public class LightLevelModule implements HudModule {

    private int lightLevel = 15;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            lightLevel = 15;
            return;
        }
        BlockPos pos = client.player.getBlockPos();
        lightLevel = client.world.getLightLevel(LightType.BLOCK, pos);
    }

    @Override
    public String getId() {
        return "lightlevel";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showLightLevel;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.lightLevelPosition;
    }

    @Override
    public String getText() {
        return "Light: " + lightLevel;
    }

    @Override
    public int getAccentColor() {
        // Most hostile mobs need block light <= 0 to spawn on a surface
        // during the day, and <= 7 at night - so anything below 8 is
        // "worth paying attention to" even before it hits 0.
        if (lightLevel <= 0) return 0xFFFF5555;
        if (lightLevel < 8) return 0xFFE8FF55;
        return 0xFF55FF7A;
    }
}
