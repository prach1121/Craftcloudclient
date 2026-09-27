package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * A single HUD readout line, e.g. "FPS: 240" - or, if {@link #getGraphWidth()}
 * / {@link #getGraphHeight()} are overridden to be positive, a self-drawn
 * graph widget instead (see {@link #drawGraph}).
 */
public interface HudModule {

    /** Unique key used for ordering / config lookups. */
    String getId();

    /** Whether the module is currently enabled in the config. */
    boolean isEnabled();

    /** Where this module should be drawn. */
    HudPosition getPosition();

    /** Called once per client tick so the module can update its internal state. */
    default void tick() {}

    /** Text to render this frame, e.g. "FPS: 240". */
    String getText();

    /**
     * Color accent for the value (label stays white/gray).
     * ARGB int, e.g. 0xFF55FF55 for green.
     */
    int getAccentColor();

    /**
     * Optional item icon to draw to the left of {@link #getText()}, e.g. the
     * helmet icon in front of its durability number. Empty (the default)
     * means "text-only row" - {@link HudManager} falls back to its old
     * layout for those.
     */
    default ItemStack getIcon() {
        return ItemStack.EMPTY;
    }

    /**
     * Non-zero on both this and {@link #getGraphHeight()} marks this module
     * as a self-drawn graph widget rather than a normal icon+text row -
     * {@link HudManager} reserves exactly this much space for it inside its
     * corner panel and calls {@link #drawGraph} instead of drawing an
     * icon/text row. Zero (the default) means "normal row".
     */
    default int getGraphWidth() {
        return 0;
    }

    default int getGraphHeight() {
        return 0;
    }

    /**
     * Draws this module's graph content into the given rectangle, already
     * positioned inside its panel by {@link HudManager}. Only called when
     * {@link #getGraphWidth()} and {@link #getGraphHeight()} are both positive.
     */
    default void drawGraph(DrawContext context, int x, int y, int w, int h) {}
}
