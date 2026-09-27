package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Clicks Per Second display.
 *
 * Deliberately does NOT mixin into the mouse/window input pipeline to
 * capture raw button events - that pipeline is one of the most
 * version-fragile parts of the game to target blind, and getting a
 * private target method wrong there is exactly the kind of mistake
 * that broke the button-theme mixin earlier. Instead this polls the
 * public {@code attackKey} / {@code useKey} KeyBindings once per client
 * tick (20/s) and counts rising edges (not-pressed -> pressed) as
 * clicks, then reports how many of those fell in the last 1000ms.
 *
 * This slightly under-counts extremely fast clicking (multiple clicks
 * inside the same 50ms tick collapse into one), which is the same
 * granularity limitation most simple CPS overlays have; it will never
 * crash from a bad mixin target, which matters more here.
 */
public class CpsModule implements HudModule {

    private final Deque<Long> leftClickTimes = new ArrayDeque<>();
    private final Deque<Long> rightClickTimes = new ArrayDeque<>();

    private boolean wasAttackPressed = false;
    private boolean wasUsePressed = false;

    private int leftCps = 0;
    private int rightCps = 0;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            leftClickTimes.clear();
            rightClickTimes.clear();
            leftCps = 0;
            rightCps = 0;
            return;
        }

        long now = System.currentTimeMillis();

        KeyBinding attack = client.options.attackKey;
        boolean attackPressed = attack.isPressed();
        if (attackPressed && !wasAttackPressed) {
            leftClickTimes.addLast(now);
        }
        wasAttackPressed = attackPressed;

        KeyBinding use = client.options.useKey;
        boolean usePressed = use.isPressed();
        if (usePressed && !wasUsePressed) {
            rightClickTimes.addLast(now);
        }
        wasUsePressed = usePressed;

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
        return "cps";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showCps;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.cpsPosition;
    }

    @Override
    public String getText() {
        return "CPS: " + leftCps + " | " + rightCps;
    }

    @Override
    public int getAccentColor() {
        int cps = Math.max(leftCps, rightCps);
        if (cps >= 12) return 0xFFFF5555;
        if (cps >= 6) return 0xFFE8FF55;
        return 0xFF55FF7A;
    }
}
