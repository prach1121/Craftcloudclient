package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Classic "keystrokes" overlay: highlights WASD and both mouse buttons the
 * instant they're pressed, with a small CPS readout underneath - the same
 * widget PvP players/streamers know from Lunar Client, Vape, 5zig, etc.
 * One of the most commonly requested overlays for this kind of client, and
 * purely cosmetic/informational like every other HUD module here - it only
 * polls public key state, never touches gameplay.
 *
 * Click counting reuses the same rising-edge-on-attackKey/useKey approach
 * as {@link CpsModule} (polled once per tick rather than mixed into the
 * input pipeline - see that class's javadoc for why) instead of depending
 * on CpsModule directly, since this widget needs its own per-key state
 * (W/A/S/D, separate L/R highlight) that CpsModule doesn't track.
 *
 * Drawn as a self-contained graph widget (see {@link HudModule#drawGraph})
 * rather than a text row, since a WASD grid + mouse buttons doesn't fit
 * the icon+text row layout every other module uses.
 */
public class KeystrokesModule implements HudModule {

    private static final int KEY = 14;
    private static final int GAP = 2;
    private static final int GRID_W = KEY * 3 + GAP * 2; // A S D
    private static final int GRID_H = KEY * 2 + GAP;     // W row + ASD row
    private static final int MOUSE_GAP = 6;
    private static final int CPS_GAP = 4;
    private static final int CPS_LINE_HEIGHT = 10;

    // Widest thing this widget ever draws is the CPS line, not the WASD
    // grid itself - measured against the live font rather than hardcoded
    // so it stays correct if the text renderer's glyph widths ever change.
    private static final String CPS_SAMPLE = "CPS: 99 | 99";

    private boolean w, a, s, d;
    private boolean left, right;

    private final Deque<Long> leftClickTimes = new ArrayDeque<>();
    private final Deque<Long> rightClickTimes = new ArrayDeque<>();
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;
    private int leftCps = 0;
    private int rightCps = 0;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            w = a = s = d = left = right = false;
            leftClickTimes.clear();
            rightClickTimes.clear();
            leftCps = 0;
            rightCps = 0;
            return;
        }

        w = client.options.forwardKey.isPressed();
        a = client.options.leftKey.isPressed();
        s = client.options.backKey.isPressed();
        d = client.options.rightKey.isPressed();

        long now = System.currentTimeMillis();

        KeyBinding attack = client.options.attackKey;
        left = attack.isPressed();
        if (left && !wasLeftPressed) leftClickTimes.addLast(now);
        wasLeftPressed = left;

        KeyBinding use = client.options.useKey;
        right = use.isPressed();
        if (right && !wasRightPressed) rightClickTimes.addLast(now);
        wasRightPressed = right;

        leftCps = prune(leftClickTimes, now);
        rightCps = prune(rightClickTimes, now);
    }

    private int prune(Deque<Long> times, long now) {
        while (!times.isEmpty() && now - times.peekFirst() > 1000L) {
            times.pollFirst();
        }
        return times.size();
    }

    @Override
    public String getId() {
        return "keystrokes";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showKeystrokes;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.keystrokesPosition;
    }

    @Override
    public String getText() {
        return "Keystrokes";
    }

    @Override
    public int getAccentColor() {
        return 0xFFF2F5FA;
    }

    @Override
    public int getGraphWidth() {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int cpsWidth = textRenderer != null ? textRenderer.getWidth(CPS_SAMPLE) : 0;
        return Math.max(GRID_W + MOUSE_GAP + KEY, cpsWidth);
    }

    @Override
    public int getGraphHeight() {
        return GRID_H + CPS_GAP + CPS_LINE_HEIGHT;
    }

    @Override
    public void drawGraph(DrawContext context, int x, int y, int w2, int h2) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int col1 = x;
        int col2 = x + KEY + GAP;
        int col3 = x + (KEY + GAP) * 2;
        int rowTop = y;
        int rowBottom = y + KEY + GAP;

        drawKey(context, textRenderer, col2, rowTop, "W", w);
        drawKey(context, textRenderer, col1, rowBottom, "A", a);
        drawKey(context, textRenderer, col2, rowBottom, "S", s);
        drawKey(context, textRenderer, col3, rowBottom, "D", d);

        int mouseX = x + GRID_W + MOUSE_GAP;
        drawKey(context, textRenderer, mouseX, rowTop, "L", left);
        drawKey(context, textRenderer, mouseX, rowBottom, "R", right);

        String cpsText = "CPS: " + leftCps + " | " + rightCps;
        int cpsY = y + GRID_H + CPS_GAP;
        context.drawTextWithShadow(textRenderer, cpsText, x, cpsY, 0xFFF2F5FA);
    }

    private void drawKey(DrawContext context, TextRenderer textRenderer, int x, int y, String label, boolean pressed) {
        int bg = pressed ? 0xFF55FF7A : 0x552A2E38;
        context.fill(x, y, x + KEY, y + KEY, bg);
        int textColor = pressed ? 0xFF102315 : 0xFFF2F5FA;
        int tw = textRenderer.getWidth(label);
        int tx = x + (KEY - tw) / 2;
        int ty = y + (KEY - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, label, tx, ty, textColor);
    }
}
