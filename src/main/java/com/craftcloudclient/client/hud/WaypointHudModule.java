package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.gui.Animator;
import com.craftcloudclient.client.gui.GlassTheme;
import com.craftcloudclient.client.waypoint.DimensionUtil;
import com.craftcloudclient.client.waypoint.Waypoint;
import com.craftcloudclient.client.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Renders up to two small cards centered near the top of the screen:
 * the closest waypoint the player has placed (left) and their most
 * recent death location (right), each showing an icon, a name, and a
 * live straight-line distance like "28.1m".
 *
 * Deliberately not a {@link HudModule} - those are single text rows
 * grouped into a corner panel, while this is a fixed pair of two-line
 * cards that defaults to always-centered, so it's driven directly from
 * {@link HudManager#render(DrawContext)} instead. It can still be
 * dragged elsewhere via the HUD layout editor - see
 * {@link #currentBounds()} and {@link #savePosition(int, int)}, which
 * {@link HudManager#collectDraggables()} hooks it up through.
 */
public class WaypointHudModule {

    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 40;
    private static final int CARD_GAP = 6;
    private static final int TOP_MARGIN = 34;
    private static final int ICON_SIZE = 16;

    // Pop-in animation: whenever the card being shown changes (a new
    // nearest waypoint becomes closer, or a fresh death gets tracked) it
    // slides down into place instead of just snapping on screen. Keyed
    // off createdAtMs rather than object identity so a save/reload that
    // deserializes a fresh Waypoint instance with the same timestamp
    // still counts as "the same card", not a new pop.
    private static final long POP_DURATION_MS = 220;
    private static final int POP_SLIDE = 8;
    private long lastNearestId = Long.MIN_VALUE;
    private long nearestPopStart = Long.MIN_VALUE;
    private long lastDeathId = Long.MIN_VALUE;
    private long deathPopStart = Long.MIN_VALUE;

    /** Which of the (up to two) cards are showing this frame, and how wide the row is. */
    private record Layout(Waypoint nearestWaypoint, Waypoint lastDeath, int totalWidth) {}

    public void render(DrawContext context) {
        if (!ModConfig.INSTANCE.showWaypointHud) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.getWindow() == null) return;

        Layout layout = computeLayout(client);
        if (layout == null) return;

        HudRect bounds = boundsFor(client, layout);
        int x = bounds.x();
        int y = bounds.y();

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        if (layout.nearestWaypoint() != null) {
            long id = layout.nearestWaypoint().createdAtMs;
            if (id != lastNearestId) {
                lastNearestId = id;
                nearestPopStart = System.currentTimeMillis();
            }
            int slide = POP_SLIDE - (int) (POP_SLIDE * Animator.popIn(nearestPopStart, POP_DURATION_MS));
            double dist = layout.nearestWaypoint().distanceTo(px, py, pz);
            drawCard(context, client, x, y - slide, layout.nearestWaypoint().name, formatDistance(dist), new ItemStack(Items.COMPASS));
            x += CARD_WIDTH + CARD_GAP;
        }

        if (layout.lastDeath() != null) {
            long id = layout.lastDeath().createdAtMs;
            if (id != lastDeathId) {
                lastDeathId = id;
                deathPopStart = System.currentTimeMillis();
            }
            int slide = POP_SLIDE - (int) (POP_SLIDE * Animator.popIn(deathPopStart, POP_DURATION_MS));
            double dist = layout.lastDeath().distanceTo(px, py, pz);
            drawCard(context, client, x, y - slide, "Latest Death", formatDistance(dist), new ItemStack(Items.PLAYER_HEAD));
        }
    }

    /**
     * Current on-screen rectangle for the HUD layout editor to hit-test
     * and drag - spanning both cards when both are showing - or
     * {@code null} if there's nothing to grab right now (waypoint HUD
     * disabled, or the player has no waypoints and hasn't died yet in
     * this dimension).
     */
    public HudRect currentBounds() {
        if (!ModConfig.INSTANCE.showWaypointHud) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getWindow() == null) return null;

        Layout layout = computeLayout(client);
        if (layout == null) return null;

        return boundsFor(client, layout);
    }

    /** Called by the HUD layout editor once a drag of this panel ends. */
    public void savePosition(int x, int y) {
        ModConfig cfg = ModConfig.INSTANCE;
        cfg.waypointHudX = x;
        cfg.waypointHudY = y;
        cfg.save();
    }

    private Layout computeLayout(MinecraftClient client) {
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        String dimension = DimensionUtil.currentDimensionKey(client);

        Waypoint nearestWaypoint = WaypointManager.INSTANCE.getNearestUserWaypoint(px, py, pz, dimension);
        Waypoint lastDeath = WaypointManager.INSTANCE.getLastDeathInDimension(dimension);
        if (nearestWaypoint == null && lastDeath == null) return null;

        int cardCount = (nearestWaypoint != null ? 1 : 0) + (lastDeath != null ? 1 : 0);
        int totalWidth = cardCount * CARD_WIDTH + (cardCount - 1) * CARD_GAP;
        return new Layout(nearestWaypoint, lastDeath, totalWidth);
    }

    private HudRect boundsFor(MinecraftClient client, Layout layout) {
        int screenWidth = client.getWindow().getScaledWidth();
        int defaultX = (screenWidth - layout.totalWidth()) / 2;

        ModConfig cfg = ModConfig.INSTANCE;
        int x = cfg.waypointHudX >= 0 ? cfg.waypointHudX : defaultX;
        int y = cfg.waypointHudY >= 0 ? cfg.waypointHudY : TOP_MARGIN;
        return new HudRect(x, y, layout.totalWidth(), CARD_HEIGHT);
    }

    private static String formatDistance(double dist) {
        return String.format("%.1fm", dist);
    }

    private void drawCard(DrawContext context, MinecraftClient client, int x, int y, String title, String subtitle, ItemStack icon) {
        if (ModConfig.INSTANCE.glassBackground) {
            GlassTheme.card(context, x, y, CARD_WIDTH, CARD_HEIGHT, false);
        }

        int centerX = x + CARD_WIDTH / 2;
        int iconX = centerX - ICON_SIZE / 2;
        int iconY = y + 4;
        context.drawItem(icon, iconX, iconY);

        int nameWidth = client.textRenderer.getWidth(title);
        int nameX = centerX - nameWidth / 2;
        int nameY = iconY + ICON_SIZE + 3;
        context.drawTextWithShadow(client.textRenderer, title, nameX, nameY, GlassTheme.TEXT_MAIN);

        int distWidth = client.textRenderer.getWidth(subtitle);
        int distX = centerX - distWidth / 2;
        int distY = nameY + client.textRenderer.fontHeight + 1;
        context.drawTextWithShadow(client.textRenderer, subtitle, distX, distY, GlassTheme.TEXT_DIM);
    }
}
