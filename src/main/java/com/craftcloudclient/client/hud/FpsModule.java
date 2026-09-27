package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.util.TickTimeTracker;

public class FpsModule implements HudModule {

    private final TickTimeTracker tracker = new TickTimeTracker();

    /** Call this once per rendered frame (from the HUD render callback itself). */
    public void onFrame() {
        tracker.tick();
    }

    @Override
    public String getId() {
        return "fps";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showFps;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.fpsPosition;
    }

    @Override
    public String getText() {
        return "FPS: " + tracker.getCountedRate();
    }

    @Override
    public int getAccentColor() {
        int fps = tracker.getCountedRate();
        if (fps >= 120) return 0xFF55FF7A;
        if (fps >= 60) return 0xFFE8FF55;
        return 0xFFFF5555;
    }
}
