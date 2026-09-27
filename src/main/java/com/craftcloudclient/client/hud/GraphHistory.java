package com.craftcloudclient.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A small fixed-length rolling window of samples for a bar-graph HUD
 * widget. Oldest samples fall off the front once the window is full, so
 * iterating {@link #samples()} always yields oldest-to-newest, left-to-right.
 */
public class GraphHistory {

    private final int capacity;
    private final Deque<Double> samples = new ArrayDeque<>();

    public GraphHistory(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public void add(double value) {
        samples.addLast(value);
        while (samples.size() > capacity) {
            samples.removeFirst();
        }
    }

    /** Oldest-to-newest samples currently held. */
    public Deque<Double> samples() {
        return samples;
    }
}
