package com.craftcloudclient.client.hud;

/**
 * A simple immutable on-screen rectangle. Shared between rendering and the
 * HUD layout editor's hit-testing/drag logic so every draggable element -
 * whether it's a corner-grouped module panel, the server banner, or the
 * waypoint tracker cards - exposes its bounds the same way.
 */
public record HudRect(int x, int y, int width, int height) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
