package com.craftcloudclient.client.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Small set of helpers to draw a consistent "glass" look:
 * a translucent dark panel, a soft top highlight, and a thin
 * accent-colored border. No shaders are used (keeps this compatible
 * everywhere) - the "glass" effect is faked with layered translucency.
 */
public final class GlassTheme {

    // True-black glass base. Kept close to pure black (rather than the old
    // navy-tinted fills) so the panel reads as dark smoked glass sitting
    // over the blurred game world, instead of a flat blue-grey card.
    public static final int PANEL_FILL = 0x9A0B0C10;      // translucent near-black
    public static final int PANEL_FILL_HOVER = 0xAA15161C;
    public static final int PANEL_BORDER = 0x3CFFFFFF;     // faint white edge
    public static int ACCENT = 0xFF6FD3FF;                 // cyan accent (mutable: swappable via the Theme tab)
    public static final int TEXT_MAIN = 0xFFF2F5FA;
    public static final int TEXT_DIM = 0xFFAAB4C0;

    public static final int ROW_FILL = 0x8C0A0B10;
    public static final int ROW_FILL_HOVER = 0x9C15171E;
    public static final int ROW_BORDER = 0x2AFFFFFF;

    // Fully-saturated on/off states - used for anything that should read as
    // a clear "green = on / red = off" indicator (enabled pills, toggle
    // switch tracks). The old *_DIM variants are kept only for spots that
    // still want a faint accent bar rather than a solid color block.
    public static final int ON_COLOR = 0xFF4CE07A;
    public static final int ON_COLOR_DIM = 0x554CE07A;
    public static final int OFF_COLOR = 0xFFE05555;
    public static final int OFF_COLOR_DIM = 0x40E05555;

    // --- Sidebar / nav (used by the tabbed menu screen) ---
    public static final int SIDEBAR_FILL = 0xB0060709;
    public static final int SIDEBAR_BORDER = 0x2EFFFFFF;
    public static final int TAB_FILL_HOVER = 0x26FFFFFF;
    public static final int TAB_FILL_ACTIVE = 0x3AFFFFFF;
    public static final int DIVIDER = 0x22FFFFFF;

    // A second accent used to give the menu a subtle two-tone gradient
    // feel (closer to the blue/purple look of other polished clients)
    // without pulling in a texture or shader dependency.
    public static int ACCENT_SECONDARY = 0xFFB98BFF;

    // Global corner/design switch, driven by the "Old Corner Design"
    // toggle in the Theme tab (backed by ModConfig#oldCornerDesign). When
    // true, every fillRounded/outlineRounded/roundedTopSheen call below
    // collapses its radius to 0, so the whole UI - panels, cards, pills,
    // buttons, the close button - falls back to flat square corners
    // instead of the rounded black-glass look, with no per-widget changes
    // needed.
    public static boolean legacyCorners = false;

    // --- Corner radii ---
    // Shared radii so every panel/card/pill in the menu rounds by a
    // consistent amount rather than each widget picking its own number.
    public static final int PANEL_RADIUS = 10;
    public static final int CARD_RADIUS = 8;
    public static final int ROW_RADIUS = 7;
    public static final int TAB_RADIUS = 6;

    private GlassTheme() {}

    // ------------------------------------------------------------------
    // Rounded-rect helpers
    //
    // Minecraft's DrawContext only draws flat rectangles, so "rounded
    // corners" are faked without any shader: each of the top/bottom
    // `radius` rows is drawn as its own horizontal strip, inset from the
    // left/right edges by a per-row amount taken from a quarter-circle,
    // and the corner pixels that are cut off are simply never drawn -
    // whatever sits behind (the blurred game world, or a panel drawn
    // earlier) shows through there instead. A stroked outline is then
    // just two of these silhouettes stacked: a slightly bigger one in the
    // border color, with a 1px-inset smaller one in the fill color drawn
    // on top of it.
    // ------------------------------------------------------------------

    /** How far a single row (0 = outermost) at corner radius {@code r} should be inset, using a quarter-circle. */
    private static int cornerInset(int r, int row) {
        if (r <= 0) return 0;
        double dy = r - row - 0.5;
        double dx = Math.sqrt(Math.max(0, (double) r * r - dy * dy));
        return (int) Math.round(r - dx);
    }

    public static void fillRounded(DrawContext ctx, int x, int y, int width, int height, int color, int radius) {
        fillRounded(ctx, x, y, width, height, color, radius, true, true, true, true);
    }

    /** Same as {@link #fillRounded(DrawContext, int, int, int, int, int, int)}, but each corner can be rounded independently (e.g. a panel that's flush against another element on one side). */
    public static void fillRounded(DrawContext ctx, int x, int y, int width, int height, int color, int radius,
                                    boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        int r = legacyCorners ? 0 : Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + width, y + height, color);
            return;
        }
        for (int row = 0; row < r; row++) {
            int left = roundTL ? cornerInset(r, row) : 0;
            int right = roundTR ? cornerInset(r, row) : 0;
            ctx.fill(x + left, y + row, x + width - right, y + row + 1, color);
        }
        ctx.fill(x, y + r, x + width, y + height - r, color);
        for (int row = 0; row < r; row++) {
            int left = roundBL ? cornerInset(r, row) : 0;
            int right = roundBR ? cornerInset(r, row) : 0;
            int rowY = y + height - 1 - row;
            ctx.fill(x + left, rowY, x + width - right, rowY + 1, color);
        }
    }

    /** A rounded rect with a 1px border: an outer silhouette in {@code borderColor}, with a 1px-inset inner silhouette in {@code fillColor} drawn on top. */
    public static void strokeRounded(DrawContext ctx, int x, int y, int width, int height, int fillColor, int borderColor, int radius,
                                      boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        fillRounded(ctx, x, y, width, height, borderColor, radius, roundTL, roundTR, roundBL, roundBR);
        fillRounded(ctx, x + 1, y + 1, width - 2, height - 2, fillColor, Math.max(0, radius - 1), roundTL, roundTR, roundBL, roundBR);
    }

    public static void strokeRounded(DrawContext ctx, int x, int y, int width, int height, int fillColor, int borderColor, int radius) {
        strokeRounded(ctx, x, y, width, height, fillColor, borderColor, radius, true, true, true, true);
    }

    /**
     * A 1px rounded-rect OUTLINE only - unlike {@link #strokeRounded}, this
     * never touches interior pixels, so it's safe to draw over content that
     * was already painted (a translucent border color here only shows up
     * right at the true edge, instead of washing over everything inside it
     * the way stacking two translucent silhouettes would).
     */
    public static void outlineRounded(DrawContext ctx, int x, int y, int width, int height, int borderColor, int radius,
                                       boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        int r = legacyCorners ? 0 : Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int row = 0; row < height; row++) {
            int left, right;
            if (row < r) {
                left = roundTL ? cornerInset(r, row) : 0;
                right = roundTR ? cornerInset(r, row) : 0;
            } else if (row >= height - r) {
                int fromBottom = height - 1 - row;
                left = roundBL ? cornerInset(r, fromBottom) : 0;
                right = roundBR ? cornerInset(r, fromBottom) : 0;
            } else {
                left = 0;
                right = 0;
            }
            int rowY = y + row;
            ctx.fill(x + left, rowY, x + left + 1, rowY + 1, borderColor);
            ctx.fill(x + width - right - 1, rowY, x + width - right, rowY + 1, borderColor);
        }
    }

    /**
     * A vertical-gradient "sheen" band, {@code sheenHeight} tall, drawn
     * across the top of a rounded rect - like {@code fillGradient} but the
     * rows inside the corner radius are inset to match {@link #fillRounded}
     * instead of being drawn as a plain rectangle (which would repaint
     * over, and square off, the rounded corners underneath).
     */
    public static void roundedTopSheen(DrawContext ctx, int x, int y, int width, int sheenHeight, int radius,
                                        int colorTop, int colorBottom, boolean roundTL, boolean roundTR) {
        int r = legacyCorners ? 0 : Math.max(0, Math.min(radius, Math.min(width, sheenHeight) / 2));
        int cornerRows = Math.min(r, sheenHeight);
        for (int row = 0; row < cornerRows; row++) {
            float t = sheenHeight <= 1 ? 0f : row / (float) (sheenHeight - 1);
            int rowColor = blend(colorTop, colorBottom, t);
            int left = roundTL ? cornerInset(r, row) : 0;
            int right = roundTR ? cornerInset(r, row) : 0;
            ctx.fill(x + left, y + row, x + width - right, y + row + 1, rowColor);
        }
        if (cornerRows < sheenHeight) {
            float t = sheenHeight <= 1 ? 0f : cornerRows / (float) (sheenHeight - 1);
            int fadeStart = blend(colorTop, colorBottom, t);
            ctx.fillGradient(x, y + cornerRows, x + width, y + sheenHeight, fadeStart, colorBottom);
        }
    }


    /** Draws a rounded-feeling glass panel (corners are simulated by insetting the border). */
    public static void panel(DrawContext ctx, int x, int y, int width, int height) {
        panel(ctx, x, y, width, height, PANEL_FILL);
    }

    public static void panel(DrawContext ctx, int x, int y, int width, int height, int fillColor) {
        // Border - a slow breathing accent tint mixed over the plain
        // white edge, so panels read as subtly "alive" rather than a
        // static screenshot. Collapses to the flat PANEL_BORDER color
        // when animations are off.
        int border = Animator.enabled()
                ? blend(PANEL_BORDER, (ACCENT & 0x00FFFFFF) | 0x50000000, Animator.pulse01(3200))
                : PANEL_BORDER;

        // Rounded fill + 1px rounded border in one pass, then a soft top
        // highlight strip (glass sheen) that respects the same rounded
        // top corners instead of squaring them back off.
        strokeRounded(ctx, x, y, width, height, fillColor, border, PANEL_RADIUS);
        int highlightHeight = Math.max(1, height / 3);
        roundedTopSheen(ctx, x, y, width, highlightHeight, PANEL_RADIUS, 0x22FFFFFF, 0x00FFFFFF, true, true);
    }

    /** Thin accent underline, used to separate a header from content. */
    public static void accentLine(DrawContext ctx, int x, int y, int width) {
        // Fade end uses the CURRENT accent's own RGB at zero alpha, rather
        // than the old hardcoded cyan - so a custom Theme-tab accent color
        // fades out to itself instead of always fading to cyan. Breathes
        // slightly brighter/dimmer when animations are on.
        int fadeEnd = ACCENT & 0x00FFFFFF;
        int endColor = Animator.enabled() ? Animator.pulseAlpha(fadeEnd, 0x00, 0x40, 2600) : fadeEnd;
        ctx.fillGradient(x, y, x + width, y + 2, ACCENT, endColor);
    }

    /** A small glass "card" used as the background of interactive rows. */
    public static void card(DrawContext ctx, int x, int y, int width, int height, boolean hovered) {
        int radius = Math.min(CARD_RADIUS, Math.max(2, height / 3));
        strokeRounded(ctx, x, y, width, height, hovered ? ROW_FILL_HOVER : ROW_FILL, ROW_BORDER, radius);
        int sheen = Math.max(1, height / 2);
        roundedTopSheen(ctx, x, y, width, sheen, radius, hovered ? 0x1EFFFFFF : 0x14FFFFFF, 0x00FFFFFF, true, true);
    }

    /** Small colored dot used before section header labels. */
    public static void dot(DrawContext ctx, int x, int y, int size, int color) {
        ctx.fill(x, y, x + size, y + size, color);
    }

    /**
     * Small filled square with a soft inner highlight and a faint border,
     * used as a lightweight "icon" next to the wordmark and the content
     * header title - a bit more polished than a flat {@link #dot} without
     * needing an actual texture.
     */
    public static void chip(DrawContext ctx, int x, int y, int size, int color) {
        ctx.fill(x, y, x + size, y + size, color);
        int highlight = Math.max(1, size / 2);
        // Breathing glint instead of a flat highlight strip - same trick
        // as the panel border above, just faster and brighter since this
        // is a small focal element (the logo dot / header chip) rather
        // than a big background surface.
        int highlightColor = Animator.enabled()
                ? Animator.pulseAlpha(0x00FFFFFF, 0x30, 0x70, 1800)
                : 0x55FFFFFF;
        ctx.fillGradient(x, y, x + size, y + highlight, highlightColor, 0x00FFFFFF);
        ctx.fill(x, y, x + size, y + 1, 0x40000000);
        ctx.fill(x, y + size - 1, x + size, y + size, 0x40000000);
    }

    /** Thin horizontal divider, e.g. to separate the settings tabs from an About tab lower in the sidebar. */
    public static void divider(DrawContext ctx, int x, int y, int width) {
        ctx.fill(x, y, x + width, y + 1, DIVIDER);
    }

    /**
     * Soft drop shadow drawn behind a big panel (menu screens). Cheap fake:
     * a handful of expanding, fading rectangles rather than a real blur.
     * Not used for the small HUD overlay panels - keeps those lightweight.
     */
    public static void shadow(DrawContext ctx, int x, int y, int width, int height) {
        int[] alphas = {0x18, 0x12, 0x0C, 0x07};
        for (int i = 0; i < alphas.length; i++) {
            int inset = (i + 1) * 3;
            int a = alphas[i] << 24;
            fillRounded(ctx, x - inset, y - inset, width + inset * 2, height + inset * 2, a, PANEL_RADIUS + inset);
        }
    }

    /**
     * Same idea as {@link #shadow}, but tinted with the current accent
     * colors instead of flat black - a faint colored halo around the
     * panel, like the glow behind a lit piece of glass, rather than a
     * plain drop shadow. Drawn underneath {@link #shadow} so the panel
     * still reads as "lifted" even on busy backgrounds where the tint
     * alone would be too subtle to notice.
     */
    public static void glow(DrawContext ctx, int x, int y, int width, int height) {
        // Slow breathing intensity on top of the base halo - subtle
        // enough not to distract from a menu the player is trying to
        // read, but enough to make the panel feel "lit" rather than
        // static.
        float pulse = Animator.pulse01(4000);
        int[] alphas = {0x14, 0x0E, 0x08, 0x04};
        for (int i = 0; i < alphas.length; i++) {
            int inset = (i + 1) * 5;
            int baseAlpha = alphas[i];
            int scaledAlpha = Animator.enabled()
                    ? Math.min(0xFF, (int) (baseAlpha * (0.7f + 0.6f * pulse)))
                    : baseAlpha;
            int a = scaledAlpha << 24;
            int left = (ACCENT & 0x00FFFFFF) | a;
            int right = (ACCENT_SECONDARY & 0x00FFFFFF) | a;
            // fillRounded only takes one flat color, so this glow layer
            // blends the left/right accent tint into a single averaged
            // color per layer rather than a true horizontal gradient -
            // a fair trade for keeping the rounded, cut-corner silhouette.
            int blended = blend(left, right, 0.5f);
            fillRounded(ctx, x - inset, y - inset, width + inset * 2, height + inset * 2, blended, PANEL_RADIUS + inset);
        }
        shadow(ctx, x, y, width, height);
    }

    /**
     * Richer panel used for the full menu screen: drop shadow, a subtle
     * vertical gradient fill (instead of one flat color) and a two-tone
     * accent line along the top edge. Kept separate from {@link #panel}
     * so the tiny HUD overlay boxes stay cheap and unchanged.
     */
    public static void menuPanel(DrawContext ctx, int x, int y, int width, int height) {
        glow(ctx, x, y, width, height);

        // Deep near-black glass fill, faked as a solid rounded rect (the
        // real "glass" comes from the game world showing through the
        // rounded-away corners and, behind everything, the engine's own
        // background blur - see ConfigScreen#render for the blur note).
        fillRounded(ctx, x, y, width, height, 0xC8090A0E, PANEL_RADIUS);

        int highlightHeight = Math.max(1, height / 4);
        roundedTopSheen(ctx, x, y, width, highlightHeight, PANEL_RADIUS, 0x20FFFFFF, 0x00FFFFFF, true, true);

        // Two-tone accent hairline along the very top edge of the panel,
        // inset on its two rows so it follows the rounded top corners
        // instead of poking past them as a flat rectangle would.
        int hairlineRadius = legacyCorners ? 0 : PANEL_RADIUS;
        for (int row = 0; row < 2; row++) {
            int inset = cornerInset(hairlineRadius, row);
            ctx.fillGradient(x + inset, y + row, x + width - inset, y + row + 1, ACCENT, ACCENT_SECONDARY);
        }

        // Outer 1px border (bottom + sides only - the top is already
        // covered by the accent hairline above), rounded to match. A real
        // outline (not the fill+border silhouette trick used by panel()/
        // card()) so it only touches the true edge pixels rather than
        // tinting the whole panel body underneath it.
        outlineRounded(ctx, x, y, width, height, PANEL_BORDER, PANEL_RADIUS, true, true, true, true);
    }

    /** Sidebar column background + right-edge divider. Rounded only on the two corners that sit flush against the outer panel's own rounded corners (top-left / bottom-left) - its right edge is an internal seam and stays straight. */
    public static void sidebar(DrawContext ctx, int x, int y, int width, int height) {
        fillRounded(ctx, x, y, width, height, 0xB0060709, PANEL_RADIUS, true, false, true, false);
        ctx.fillGradient(x, y, x + width, y + Math.max(1, height / 3), 0x14FFFFFF, 0x00FFFFFF);
        ctx.fill(x + width - 1, y, x + width, y + height, SIDEBAR_BORDER);
    }

    /** Background for a sidebar nav tab, with a left accent bar when active. Rounded on its outer (right-hand) corners only, since the left edge sits flush against the sidebar's own left wall. */
    public static void tab(DrawContext ctx, int x, int y, int width, int height, boolean active, boolean hovered, int accent) {
        if (active) {
            fillRounded(ctx, x, y, width, height, TAB_FILL_ACTIVE, TAB_RADIUS, false, true, false, true);
            roundedTopSheen(ctx, x, y, width, Math.max(1, height / 2), TAB_RADIUS, 0x18FFFFFF, 0x00FFFFFF, false, true);
            ctx.fill(x, y, x + 3, y + height, accent);
        } else if (hovered) {
            fillRounded(ctx, x, y, width, height, TAB_FILL_HOVER, TAB_RADIUS, false, true, false, true);
            ctx.fill(x, y, x + 2, y + height, (accent & 0x00FFFFFF) | 0x80000000);
        }
    }

    /** Linearly interpolates two ARGB colors (all four channels) by {@code t} in [0, 1]. */
    public static int blend(int colorA, int colorB, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (colorA >>> 24) & 0xFF, rA = (colorA >>> 16) & 0xFF, gA = (colorA >>> 8) & 0xFF, bA = colorA & 0xFF;
        int aB = (colorB >>> 24) & 0xFF, rB = (colorB >>> 16) & 0xFF, gB = (colorB >>> 8) & 0xFF, bB = colorB & 0xFF;
        int a = (int) (aA + (aB - aA) * t);
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
