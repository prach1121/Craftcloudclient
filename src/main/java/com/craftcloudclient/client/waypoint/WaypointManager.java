package com.craftcloudclient.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persistent list of waypoints - both the ones the player drops manually
 * (the "Add Waypoint" keybind) and the ones this client drops for them
 * automatically whenever they die (see {@link DeathTracker}). Both kinds
 * live in the same file/list, distinguished by {@link Waypoint#death},
 * since a "where did I die" marker really is just a specially-flagged
 * waypoint the client created instead of the player.
 *
 * Stored at {@code .minecraft/config/craftcloudclient_waypoints.json}.
 */
public final class WaypointManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("craftcloudclient_waypoints.json");
    private static final Type LIST_TYPE = new TypeToken<ArrayList<Waypoint>>() {}.getType();

    /** Oldest death markers beyond this count are dropped automatically so the file can't grow forever on a long-lived world. User waypoints are never auto-trimmed. */
    private static final int MAX_DEATH_MARKERS = 20;

    public static WaypointManager INSTANCE = load();

    private final List<Waypoint> waypoints;

    private WaypointManager(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public static WaypointManager load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                List<Waypoint> data = GSON.fromJson(reader, LIST_TYPE);
                if (data != null) {
                    return new WaypointManager(new ArrayList<>(data));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new WaypointManager(new ArrayList<>());
    }

    public synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(waypoints, LIST_TYPE, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Waypoints are added rarely (a manual keybind press, or a death) so saving immediately - rather than batching like {@link com.craftcloudclient.client.stats.StatsManager} does - is fine here. */
    public synchronized Waypoint addWaypoint(String name, double x, double y, double z, String dimension) {
        Waypoint wp = new Waypoint(name, x, y, z, dimension, false);
        waypoints.add(wp);
        save();
        return wp;
    }

    public synchronized void recordDeath(double x, double y, double z, String dimension) {
        waypoints.add(new Waypoint("Death", x, y, z, dimension, true));

        List<Waypoint> deaths = waypoints.stream().filter(w -> w.death).collect(Collectors.toList());
        if (deaths.size() > MAX_DEATH_MARKERS) {
            deaths.stream()
                    .sorted(Comparator.comparingLong(w -> w.createdAtMs))
                    .limit(deaths.size() - MAX_DEATH_MARKERS)
                    .forEach(waypoints::remove);
        }

        save();
    }

    /** Player-added waypoints only, newest first. */
    public synchronized List<Waypoint> getUserWaypoints() {
        return waypoints.stream()
                .filter(w -> !w.death)
                .sorted(Comparator.comparingLong((Waypoint w) -> w.createdAtMs).reversed())
                .collect(Collectors.toList());
    }

    /** Auto death markers only, newest first. */
    public synchronized List<Waypoint> getDeathMarkers() {
        return waypoints.stream()
                .filter(w -> w.death)
                .sorted(Comparator.comparingLong((Waypoint w) -> w.createdAtMs).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Waypoint getLastDeath() {
        List<Waypoint> deaths = getDeathMarkers();
        return deaths.isEmpty() ? null : deaths.get(0);
    }

    /** Most recent death marker recorded in the given dimension, or null if there isn't one - used by the HUD tracker so it never shows a stale cross-dimension distance. */
    public synchronized Waypoint getLastDeathInDimension(String dimension) {
        return getDeathMarkers().stream()
                .filter(w -> dimension == null || w.dimension == null || w.dimension.equals(dimension))
                .findFirst()
                .orElse(null);
    }

    /** Closest player-added waypoint to the given position, restricted to the given dimension so distances are always meaningful. Null if there are none in that dimension. */
    public synchronized Waypoint getNearestUserWaypoint(double x, double y, double z, String dimension) {
        Waypoint nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Waypoint w : waypoints) {
            if (w.death || w.hidden) continue;
            if (dimension != null && w.dimension != null && !w.dimension.equals(dimension)) continue;
            double dist = w.distanceTo(x, y, z);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = w;
            }
        }
        return nearest;
    }

    public synchronized int userWaypointCount() {
        int count = 0;
        for (Waypoint w : waypoints) {
            if (!w.death) count++;
        }
        return count;
    }

    public synchronized void clearUserWaypoints() {
        waypoints.removeIf(w -> !w.death);
        save();
    }

    public synchronized void clearDeathMarkers() {
        waypoints.removeIf(w -> w.death);
        save();
    }

    /** Deletes exactly one waypoint (user-placed or a death marker) by identity. Safe to call with a Waypoint instance obtained from any of the getters above. */
    public synchronized void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        save();
    }

    /** Hides (or unhides) exactly one waypoint. A hidden waypoint is skipped by {@link #getNearestUserWaypoint} but is never deleted, so it can be toggled back on later. */
    public synchronized void setHidden(Waypoint waypoint, boolean hidden) {
        waypoint.hidden = hidden;
        save();
    }

    /** Renames exactly one waypoint (user-placed or a death marker) by identity, then persists the change. Blank names are rejected by the caller before this is invoked. */
    public synchronized void renameWaypoint(Waypoint waypoint, String newName) {
        waypoint.name = newName;
        save();
    }
}
