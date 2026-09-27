package com.craftcloudclient.client.util;

/**
 * Rolling average tracker used for both the frame counter (FPS) and the
 * client tick counter (used to estimate TPS).
 *
 * For FPS: call {@link #tick()} once every rendered frame.
 * For TPS: call {@link #tick()} once every client tick (20/s target) and
 * read {@link #getRate()} which will drop below 20 whenever the client has
 * to wait on the server / falls behind, giving a reasonable smoothness
 * indicator even without direct access to the remote server's real tick
 * timings (which vanilla does not expose over the network).
 */
public class TickTimeTracker {

    private static final int WINDOW = 20;
    private final long[] samples = new long[WINDOW];
    private int index = 0;
    private int filled = 0;
    private long lastTime = 0L;

    // Simple 1-second frame counting, more stable for FPS display than
    // averaging frame deltas which can be noisy.
    private int frameCounter = 0;
    private long secondStart = System.nanoTime();
    private int lastRate = 0;

    public void tick() {
        long now = System.nanoTime();
        if (lastTime != 0L) {
            samples[index] = now - lastTime;
            index = (index + 1) % WINDOW;
            if (filled < WINDOW) filled++;
        }
        lastTime = now;

        frameCounter++;
        if (now - secondStart >= 1_000_000_000L) {
            lastRate = frameCounter;
            frameCounter = 0;
            secondStart = now;
        }
    }

    /** Frames (or ticks) counted in the last full second window. */
    public int getCountedRate() {
        return lastRate;
    }

    /** Average rate derived from the rolling delta buffer, clamped to [0, cap]. */
    public double getRate(double cap) {
        if (filled == 0) return cap;
        long sum = 0;
        for (int i = 0; i < filled; i++) sum += samples[i];
        double avgNanos = (double) sum / filled;
        if (avgNanos <= 0) return cap;
        double rate = 1_000_000_000.0 / avgNanos;
        return Math.min(cap, rate);
    }
}
