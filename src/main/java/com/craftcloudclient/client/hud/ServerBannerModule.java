package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.gui.GlassTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;

/**
 * Small pill-shaped banner centered near the very top of the screen,
 * showing the connected server's own icon (pulled from the multiplayer
 * server-list entry, the same favicon shown on the server-selection
 * screen) plus the address/name of the server (e.g. "donutsmp.net").
 * Purely cosmetic - it carries no gameplay information - and exists so
 * streamers can put their server tag on screen for viewers without
 * saying it out loud every stream. Hidden outright when Streamer Mode
 * (Security tab) is on, regardless of this toggle's own state - see
 * {@link com.craftcloudclient.client.config.ModConfig#streamerMode}.
 *
 * Deliberately not a {@link HudModule}: those are text rows the player
 * groups into a corner panel, while this is a single pill, so - like
 * {@link WaypointHudModule} - it's driven directly from
 * {@link HudManager#render(DrawContext)} instead. Its default position
 * (top-center) is set via the quick-pick in the settings screen (see
 * {@link com.craftcloudclient.client.config.ModConfig#serverBannerPosition}),
 * and it can also be dragged to any exact pixel spot via the HUD layout
 * editor - see {@link #currentBounds()} and {@link #savePosition(int, int)},
 * which {@link HudManager#collectDraggables()} hooks it up through.
 */
public class ServerBannerModule {

    private static final int TOP_MARGIN = 8;
    private static final int PADDING_X = 8;
    private static final int LOGO_SIZE = 14;
    private static final int LOGO_TEXT_GAP = 6;
    private static final int HEIGHT = 22;

    public void render(DrawContext context) {
        if (!ModConfig.INSTANCE.showServerBanner || ModConfig.INSTANCE.streamerMode) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.getWindow() == null) return;

        String label = serverLabel(client);
        if (label == null || label.isEmpty()) return;

        HudRect bounds = boundsFor(client, label);
        int x = bounds.x();
        int y = bounds.y();
        int width = bounds.width();

        if (ModConfig.INSTANCE.glassBackground) {
            GlassTheme.panel(context, x, y, width, HEIGHT);
        } else {
            // Non-glass fallback: same flat fill style HudManager falls
            // back to for its own panels when "Glass Background" is off.
            context.fill(x, y, x + width, y + HEIGHT, GlassTheme.PANEL_FILL);
        }

        int logoX = x + PADDING_X;
        int logoY = y + (HEIGHT - LOGO_SIZE) / 2;
        drawLogo(context, client, logoX, logoY);

        int textX = logoX + LOGO_SIZE + LOGO_TEXT_GAP;
        int textY = y + (HEIGHT - client.textRenderer.fontHeight) / 2 + 1;
        context.drawTextWithShadow(client.textRenderer, label, textX, textY, GlassTheme.TEXT_MAIN);
    }

    /**
     * Current on-screen rectangle for the HUD layout editor to hit-test
     * and drag, or {@code null} if there's nothing showing right now to
     * grab (banner disabled, not connected to a server, singleplayer/LAN
     * with no server-list entry, etc).
     */
    public HudRect currentBounds() {
        if (!ModConfig.INSTANCE.showServerBanner || ModConfig.INSTANCE.streamerMode) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getWindow() == null) return null;

        String label = serverLabel(client);
        if (label == null || label.isEmpty()) return null;

        return boundsFor(client, label);
    }

    /** Called by the HUD layout editor once a drag of this panel ends. */
    public void savePosition(int x, int y) {
        ModConfig cfg = ModConfig.INSTANCE;
        cfg.serverBannerX = x;
        cfg.serverBannerY = y;
        cfg.save();
    }

    private HudRect boundsFor(MinecraftClient client, String label) {
        int textWidth = client.textRenderer.getWidth(label);
        int width = PADDING_X * 2 + LOGO_SIZE + LOGO_TEXT_GAP + textWidth;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        ModConfig cfg = ModConfig.INSTANCE;

        // Default position comes from the quick-pick in the settings
        // screen (see GlassBannerPositionPicker on the Server Banner row).
        // Defaults to TOP_CENTER for configs saved before this field
        // existed (Gson leaves new fields at their declared default).
        int defaultX = switch (cfg.serverBannerPosition) {
            case TOP_LEFT, BOTTOM_LEFT -> TOP_MARGIN;
            case TOP_CENTER, BOTTOM_CENTER -> (screenWidth - width) / 2;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - width - TOP_MARGIN;
        };
        int defaultY = switch (cfg.serverBannerPosition) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> TOP_MARGIN;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - HEIGHT - TOP_MARGIN;
        };

        // Manually dragging the banner in the HUD layout editor overrides
        // the corner default until the player picks a corner again.
        int x = cfg.serverBannerX >= 0 ? cfg.serverBannerX : defaultX;
        int y = cfg.serverBannerY >= 0 ? cfg.serverBannerY : defaultY;
        return new HudRect(x, y, width, HEIGHT);
    }

    /**
     * Draws the server's real list icon if the current server-list entry
     * has one cached ({@link ServerIconTexture} handles the decode +
     * upload), otherwise falls back to the plain accent-colored dot used
     * before (LAN worlds, direct-connect-with-no-saved-entry, or a
     * server that just doesn't set a favicon).
     */
    private void drawLogo(DrawContext context, MinecraftClient client, int logoX, int logoY) {
        Identifier icon = ServerIconTexture.get(client);
        if (icon == null) {
            GlassTheme.chip(context, logoX, logoY, LOGO_SIZE, GlassTheme.ACCENT);
            return;
        }

        // NOTE: DrawContext#drawTexture's exact overload has moved around
        // across recent Minecraft versions (a RenderLayer-supplier first
        // argument was added around the 1.21.2 "GuiTextured" rework). If
        // this doesn't match your mappings, the pre-rework signature is:
        //   context.drawTexture(icon, logoX, logoY, 0, 0, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, logoX, logoY, 0, 0,
                LOGO_SIZE, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE);
    }

    /**
     * Prefers the address of the server-list entry the player connected
     * through (what shows up on the multiplayer screen, e.g.
     * "donutsmp.net"), falling back to its display name. Returns null on
     * singleplayer/direct-connect-with-no-entry/LAN, where there's no
     * server identity worth showing.
     */
    private String serverLabel(MinecraftClient client) {
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry == null) return null;
        if (entry.address != null && !entry.address.isBlank()) return entry.address;
        return entry.name;
    }
}