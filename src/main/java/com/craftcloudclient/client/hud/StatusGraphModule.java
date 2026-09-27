package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.util.TickTimeTracker;
import net.minecraft.client.gui.DrawContext;

/**
 * Small rolling bar-graph of the same client-side TPS estimate
 * {@link TpsModule} shows as text (own tracker instance - see the class
 * doc on {@link TickTimeTracker} for what "TPS" means here), so a lag
 * spike shows up as a visible dip instead of only a number that's easy to
 * miss mid-fight. Off by default - see {@link ModConfig#showStatusGraph}.
 */
public class StatusGraphModule implements HudModule {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 28;
    private static final int BAR_WIDTH = 2;
    private static final int SAMPLE_EVERY_TICKS = 10; // ~0.5s per bar at 20 tps -> 20s of history
    private static final double SCALE_MAX_TPS = 20.0;

    private final TickTimeTracker tracker = new TickTimeTracker();
    private final GraphHistory history = new GraphHistory(WIDTH / BAR_WIDTH);
    private int tickCounter = 0;

    @Override
    public void tick() {
        tracker.tick();
        double tps = tracker.getRate(SCALE_MAX_TPS);

        tickCounter++;
        if (tickCounter >= SAMPLE_EVERY_TICKS) {
            tickCounter = 0;
            history.add(tps);
        }
    }

    @Override
    public String getId() {
        return "status_graph";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showStatusGraph;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.statusGraphPosition;
    }

    @Override
    public String getText() {
        return "Status Graph";
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
            double clamped = Math.max(0, Math.min(sample, SCALE_MAX_TPS));
            int barHeight = (int) Math.max(1, (clamped / SCALE_MAX_TPS) * (h - 2));
            int barY = y + h - barHeight;
            context.fill(barX, barY, barX + BAR_WIDTH - 1, y + h, colorFor(sample));
            i++;
        }
    }

    private int colorFor(double tps) {
        if (tps >= 19.0) return 0xFF55FF7A;
        if (tps >= 15.0) return 0xFFE8FF55;
        return 0xFFFF5555;
    }
}
