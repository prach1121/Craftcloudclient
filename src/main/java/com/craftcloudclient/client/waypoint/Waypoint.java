package com.craftcloudclient.client.waypoint;

/**
 * One saved location: either a waypoint the player dropped themselves
 * (see the "Add Waypoint" keybind) or an automatic marker this client
 * dropped for them when they died (see {@link DeathTracker}) - the
 * {@link #death} flag is the only thing distinguishing the two.
 */
public class Waypoint {

    public String name;
    public double x, y, z;
    /** e.g. "minecraft:overworld" - see {@link DimensionUtil}. */
    public String dimension;
    public long createdAtMs;
    public boolean death;
    /** When true, this waypoint is skipped by the nearest-waypoint HUD tracker but stays in the saved list so it can be shown again later. */
    public boolean hidden;

    /** No-arg constructor required by Gson when reading the save file back in. */
    public Waypoint() {}

    public Waypoint(String name, double x, double y, double z, String dimension, boolean death) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.createdAtMs = System.currentTimeMillis();
        this.death = death;
    }

    /** Straight-line distance from the given position to this waypoint. Caller is responsible for checking dimension first. */
    public double distanceTo(double px, double py, double pz) {
        double dx = this.x - px;
        double dy = this.y - py;
        double dz = this.z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
