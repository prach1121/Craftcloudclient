package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/**
 * Small rolling bar-graph of ping over roughly the last 20 seconds, so a
 * lag spike is visible as a shape instead of only a number that's easy to
 * miss mid-fight. Off by default - a graph takes noticeably more screen
 * space than a single text line, so it only shows up once explicitly
 * enabled from the HUD tab (see {@link ModConfig#showPingGraph}).
 */
public class PingGraphModule implements HudModule {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 28;
    private static final int BAR_WIDTH = 2;
    private static final int SAMPLE_EVERY_TICKS = 10; // ~0.5s per bar at 20 tps -> 20s of history
    private static final double SCALE_MAX_MS = 300.0; // bars clip (don't grow further) above this

    private final GraphHistory history = new GraphHistory(WIDTH / BAR_WIDTH);
    private int tickCounter = 0;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        int ping = 0;
        if (client.player != null) {
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            PlayerListEntry entry = handler != null ? handler.getPlayerListEntry(client.player.getUuid()) : null;
            ping = (entry != null) ? entry.getLatency() : 0;
        }

        tickCounter++;
        if (tickCounter >= SAMPLE_EVERY_TICKS) {
            tickCounter = 0;
            history.add(ping);
        }
    }

    @Override
    public String getId() {
        return "ping_graph";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showPingGraph;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.pingGraphPosition;
    }

    @Override
    public String getText() {
        return "Ping Graph";
    }

    @Override
    public int getAccentColor() {
        return 0xFFF2F5FA;
    }

    @Override
    public int getGraphWidth() {
        return WIDTH;
    }

    @Override
    public int getGraphHeight() {
        return HEIGHT;
    }

    @Override
    public void drawGraph(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0x552A2E38);

        int i = 0;
        for (double sample : history.samples()) {
            int barX = x + i * BAR_WIDTH;
            double clamped = Math.max(0, Math.min(sample, SCALE_MAX_MS));
            int barHeight = (int) Math.max(1, (clamped / SCALE_MAX_MS) * (h - 2));
            int barY = y + h - barHeight;
            context.fill(barX, barY, barX + BAR_WIDTH - 1, y + h, colorFor(sample));
            i++;
        }
    }

    private int colorFor(double ms) {
        if (ms <= 60) return 0xFF55FF7A;
        if (ms <= 150) return 0xFFE8FF55;
        return 0xFFFF5555;
    }
}
