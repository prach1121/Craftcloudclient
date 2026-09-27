package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.gui.GlassTheme;
import com.craftcloudclient.client.waypoint.DimensionUtil;
import com.craftcloudclient.client.waypoint.Waypoint;
import com.craftcloudclient.client.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Small compass-style strip showing which way to turn to face the
 * player's nearest (non-hidden) waypoint, plus its live straight-line
 * distance - a directional companion to {@link WaypointHudModule}'s
 * name/distance card. Toggled separately from that card (see
 * {@link ModConfig#showWaypointNavigator}) since some players want the
 * turn-by-turn strip without the card, or vice versa; running both,
 * either, or neither is fine.
 *
 * The strip never rotates any graphics - DrawContext doesn't give this
 * project a cheap rotated-fill primitive to lean on (see the rest of the
 * HUD, which only ever uses axis-aligned {@code context.fill} calls), so
 * instead the bearing to the waypoint relative to the player's facing,
 * from -180 (directly behind) to +180, is mapped linearly onto a fixed
 * horizontal bar and drawn as a moving marker - the same trick a lot of
 * shooters use for their compass HUD.
 */
public class WaypointNavigatorModule {

    private static final int BAR_WIDTH = 110;
    private static final int BAR_HEIGHT = 2;
    private static final int MARKER_WIDTH = 8;
    private static final int MARKER_HEIGHT = 4;
    private static final int MODULE_HEIGHT = 26;
    // Sits below WaypointHudModule's default centered card (y=34,
    // height 40) so the two don't overlap out of the box - both remain
    // independently draggable via the HUD layout editor regardless.
    private static final int TOP_MARGIN = 80;
    /** Bearings within this many degrees of dead-ahead show a "straight on" hint instead of a turn direction. */
    private static final double STRAIGHT_AHEAD_THRESHOLD = 8.0;

    public void render(DrawContext context) {
        if (!ModConfig.INSTANCE.showWaypointNavigator) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.getWindow() == null) return;

        Waypoint target = nearestTarget(client);
        if (target == null) return;

        HudRect bounds = boundsFor(client);
        drawNavigator(context, client, bounds.x(), bounds.y(), target);
    }

    /**
     * Current on-screen rectangle for the HUD layout editor to hit-test
     * and drag, or {@code null} if there's nothing to navigate to right
     * now (navigator disabled, or no waypoints in this dimension).
     */
    public HudRect currentBounds() {
        if (!ModConfig.INSTANCE.showWaypointNavigator) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getWindow() == null) return null;
        if (nearestTarget(client) == null) return null;

        return boundsFor(client);
    }

    /** Called by the HUD layout editor once a drag of this panel ends. */
    public void savePosition(int x, int y) {
        ModConfig cfg = ModConfig.INSTANCE;
        cfg.waypointNavigatorX = x;
        cfg.waypointNavigatorY = y;
        cfg.save();
    }

    private Waypoint nearestTarget(MinecraftClient client) {
        String dimension = DimensionUtil.currentDimensionKey(client);
        return WaypointManager.INSTANCE.getNearestUserWaypoint(
                client.player.getX(), client.player.getY(), client.player.getZ(), dimension);
    }

    private HudRect boundsFor(MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int defaultX = (screenWidth - BAR_WIDTH) / 2;

        ModConfig cfg = ModConfig.INSTANCE;
        int x = cfg.waypointNavigatorX >= 0 ? cfg.waypointNavigatorX : defaultX;
        int y = cfg.waypointNavigatorY >= 0 ? cfg.waypointNavigatorY : TOP_MARGIN;
        return new HudRect(x, y, BAR_WIDTH, MODULE_HEIGHT);
    }

    private void drawNavigator(DrawContext context, MinecraftClient client, int x, int y, Waypoint target) {
        double relative = relativeBearing(client, target);

        int centerX = x + BAR_WIDTH / 2;

        if (ModConfig.INSTANCE.glassBackground) {
            GlassTheme.card(context, x, y, BAR_WIDTH, MODULE_HEIGHT, false);
        }

        // The track plus a brighter center tick marking dead-ahead - the
        // marker's position relative to that tick tells the whole story,
        // no numeric heading needed to read "turn right a bit".
        context.fill(x + 4, y + 4, x + BAR_WIDTH - 4, y + 4 + BAR_HEIGHT, 0x55FFFFFF);
        context.fill(centerX - 1, y + 2, centerX + 1, y + 8, GlassTheme.TEXT_DIM);

        int halfTravel = BAR_WIDTH / 2 - 6;
        int markerX = centerX + (int) Math.round((relative / 180.0) * halfTravel);
        boolean aligned = Math.abs(relative) <= STRAIGHT_AHEAD_THRESHOLD;
        int markerColor = aligned ? GlassTheme.ACCENT : 0xFFE6C15A;
        context.fill(markerX - MARKER_WIDTH / 2, y, markerX + MARKER_WIDTH / 2, y + MARKER_HEIGHT, markerColor);

        double dist = target.distanceTo(client.player.getX(), client.player.getY(), client.player.getZ());
        String turnHint = aligned ? "^" : (relative > 0 ? ">" : "<");
        String line = turnHint + " " + target.name + "  " + String.format("%.0fm", dist);
        int tw = client.textRenderer.getWidth(line);
        context.drawTextWithShadow(client.textRenderer, line, centerX - tw / 2, y + 12, GlassTheme.TEXT_MAIN);
    }

    /**
     * Degrees the player needs to turn to face {@code target}, in
     * (-180, 180]: 0 is dead-ahead, positive is "turn right" (clockwise,
     * matching Minecraft's own yaw convention), negative is "turn left".
     */
    private double relativeBearing(MinecraftClient client, Waypoint target) {
        double dx = target.x - client.player.getX();
        double dz = target.z - client.player.getZ();
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        return normalizeAngle(bearing - client.player.getYaw());
    }

    private static double normalizeAngle(double angle) {
        double a = angle % 360;
        if (a >= 180) a -= 360;
        if (a < -180) a += 360;
        return a;
    }
}
