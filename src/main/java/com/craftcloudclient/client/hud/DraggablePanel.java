package com.craftcloudclient.client.hud;

/**
 * Anything the HUD layout editor ({@code HudLayoutScreen}) can grab and
 * drag around: the existing corner-grouped module panels
 * ({@link HudManager.GroupBox}), plus the server banner and waypoint
 * tracker cards, which used to always be pinned to top-center with no
 * way to reposition them at all.
 */
public interface DraggablePanel {

    /** Where this panel is on screen right now. */
    HudRect bounds();

    /** Persists a new top-left position after a drag ends. */
    void savePosition(int x, int y);

    /**
     * Whether right-clicking this panel should offer the "jump to
     * corner" quick-pick popup. The server banner and waypoint cards are
     * meant to stay near top-center rather than snap to a corner, so
     * they only support free dragging.
     */
    default boolean supportsCornerSnap() {
        return true;
    }
}
