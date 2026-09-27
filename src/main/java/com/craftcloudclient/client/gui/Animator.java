package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.config.ModConfig;

/**
 * Tiny time-based helpers for the mod's motion: breathing accent glows,
 * ease-out "pop in" scales for cards/panels, and flash pulses on stat
 * increases.
 *
 * Everything here is driven off wall-clock time ({@link System#currentTimeMillis()})
 * rather than client ticks, so animations stay smooth regardless of the
 * current TPS/FPS and don't need any per-frame state threaded through
 * from a tick method.
 *
 * Every helper collapses to its "resting" value whenever
 * {@link ModConfig#animationsEnabled} is off, so the "Animations" toggle
 * in the Theme tab is a true zero-cost no-op - callers don't need their
 * own enabled checks scattered everywhere, and nothing gets stuck
 * mid-animation (half-faded, half-scaled) if the player turns it off
 * while something is mid-flight.
 */
public final class Animator {

    private Animator() {}

    public static boolean enabled() {
        return ModConfig.INSTANCE.animationsEnabled;
    }

    /**
     * Smooth 0..1 sine wave with the given period in milliseconds.
     * Resting value (animations off) is a flat 0.5, so anything blended
     * with this - like {@link #pulseAlpha} - lands exactly halfway
     * between its min and max rather than snapping to one extreme.
     */
    public static float pulse01(long periodMs) {
        if (!enabled()) return 0.5f;
        long now = System.currentTimeMillis();
        double phase = (now % periodMs) / (double) periodMs;
        return (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
    }

    /** Same as {@link #pulse01} but phase-shifted, for a second element that shouldn't breathe in lockstep with the first. */
    public static float pulse01Offset(long periodMs, long offsetMs) {
        if (!enabled()) return 0.5f;
        return pulse01AtTime(System.currentTimeMillis() + offsetMs, periodMs);
    }

    private static float pulse01AtTime(long timeMs, long periodMs) {
        double phase = (timeMs % periodMs) / (double) periodMs;
        return (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
    }

    /** Blends the alpha channel of {@code color} between {@code minAlpha} and {@code maxAlpha} on a breathing sine wave. */
    public static int pulseAlpha(int color, int minAlpha, int maxAlpha, long periodMs) {
        float t = pulse01(periodMs);
        int alpha = (int) (minAlpha + (maxAlpha - minAlpha) * t);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * 0..1 "pop in" progress since {@code startMillis}, easing out (fast
     * start, slow settle) over {@code durationMs}. Returns 1 once the
     * animation has finished - or immediately when disabled, so callers
     * that scale/fade a card based on this never get stuck partway
     * through.
     */
    public static float popIn(long startMillis, long durationMs) {
        if (!enabled()) return 1f;
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= durationMs) return 1f;
        if (elapsed <= 0) return 0f;
        float t = elapsed / (float) durationMs;
        return 1f - (float) Math.pow(1f - t, 3); // ease-out cubic
    }

    /**
     * 1..0 "flash" progress since {@code startMillis} - starts at 1 (full
     * intensity) and eases back down to 0 over {@code durationMs}. Used
     * for one-shot highlight flashes (e.g. a stat ticking up) rather than
     * a continuous loop. Returns 0 once finished, or immediately when
     * disabled.
     */
    public static float flashOut(long startMillis, long durationMs) {
        if (!enabled()) return 0f;
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed >= durationMs || elapsed < 0) return 0f;
        float t = elapsed / (float) durationMs;
        return 1f - t * t; // ease-in fade, fast fall-off at the end
    }
}
