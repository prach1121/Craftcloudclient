package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.util.TickTimeTracker;
import net.minecraft.client.MinecraftClient;

/**
 * Client-side TPS estimate.
 *
 * Vanilla servers don't broadcast their real tick rate to clients, so this
 * measures how smoothly the *client* is able to advance its own ticks
 * (which stalls/slows down when the connection can't keep up), clamped to
 * a 20 TPS ceiling. It is a good "is something lagging" indicator even
 * though it is not a literal reading of the remote server's tick loop.
 */
public class TpsModule implements HudModule {

    private final TickTimeTracker tracker = new TickTimeTracker();
    private double lastTps = 20.0;

    @Override
    public void tick() {
        tracker.tick();
        lastTps = tracker.getRate(20.0);
    }

    @Override
    public String getId() {
        return "tps";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showTps;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.tpsPosition;
    }

    @Override
    public String getText() {
        MinecraftClient client = MinecraftClient.getInstance();
        String suffix = (client.getServer() != null) ? "" : " (est.)";
        return String.format("TPS: %.1f%s", lastTps, suffix);
    }

    @Override
    public int getAccentColor() {
        if (lastTps >= 19.0) return 0xFF55FF7A;
        if (lastTps >= 15.0) return 0xFFE8FF55;
        return 0xFFFF5555;
    }
}
