package com.craftcloudclient.client.presence;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Discord Rich Presence for Craftcloud Client. Started from
 * {@code CraftcloudClient#onInitializeClient()} when the Security tab's
 * "Discord Status (RPC)" toggle is on (the default), and can be started or
 * stopped at any time from that toggle via {@link #start()}/{@link #stop()}
 * - both are idempotent and safe to call repeatedly.
 *
 * Two threads are involved and they never share mutable state directly:
 * <ul>
 *   <li>The client tick thread calls {@link #updateState} roughly once a
 *       second with a fresh, immutable {@link PresenceState} snapshot -
 *       it only ever touches {@link MinecraftClient} fields, never the
 *       network.</li>
 *   <li>A single daemon background thread ({@link #runLoop}) owns the
 *       actual IPC connection: it reconnects on a slow retry loop whenever
 *       Discord isn't reachable, and otherwise just republishes whatever
 *       the latest snapshot is every {@link #UPDATE_INTERVAL_MS}.</li>
 * </ul>
 *
 * Every operation on the background thread is wrapped so nothing here can
 * ever throw back into game code: if Discord isn't running, isn't
 * installed, or closes the pipe mid-session, this quietly drops the
 * connection and retries later instead of surfacing anything.
 */
public final class DiscordPresenceManager {

    // Application registered in the Discord Developer Portal for Craftcloud Client.
    private static final String CLIENT_ID = "1525565792895369416";

    private static final Logger LOGGER = LoggerFactory.getLogger("craftcloudclient/discord-rpc");

    // Discord rate-limits activity updates to roughly once every 15s; this
    // also just doubles as the idle poll interval for the reconnect check.
    private static final long UPDATE_INTERVAL_MS = 15_000;
    private static final long RECONNECT_BACKOFF_MS = 20_000;

    private static final long SESSION_START_EPOCH = Instant.now().getEpochSecond();

    private static volatile PresenceState latestState = PresenceState.menu(SESSION_START_EPOCH);
    private static volatile boolean running = false;
    private static Thread thread;

    private DiscordPresenceManager() {}

    /** Starts the background presence thread. Safe to call more than once - later calls are no-ops. */
    public static synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(DiscordPresenceManager::runLoop, "craftcloudclient-discord-rpc");
        thread.setDaemon(true);
        thread.start();
    }

    /** Stops the background thread and drops the connection. Called once from {@code CLIENT_STOPPING}. */
    public static synchronized void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    /**
     * Refreshes what the background thread will publish next. Cheap and
     * safe to call every tick - only reads a few {@link MinecraftClient}
     * fields, does no I/O, and never throws.
     */
    public static void updateState(MinecraftClient client) {
        try {
            latestState = PresenceState.capture(client, SESSION_START_EPOCH);
        } catch (Throwable ignored) {
            // Presence tracking must never be able to affect the game loop.
        }
    }

    private static void runLoop() {
        DiscordIpcClient ipc = null;
        PresenceState lastSent = null;
        long lastConnectAttempt = 0L;

        while (running) {
            try {
                if (ipc == null) {
                    long now = System.currentTimeMillis();
                    if (now - lastConnectAttempt < RECONNECT_BACKOFF_MS) {
                        sleep(1000);
                        continue;
                    }
                    lastConnectAttempt = now;
                    ipc = tryConnect();
                    lastSent = null; // force a resend once we're back up
                    if (ipc == null) {
                        sleep(1000);
                        continue;
                    }
                }

                PresenceState snapshot = latestState;
                try {
                    ipc.setActivity(snapshot.details(), snapshot.state(), snapshot.startEpochSeconds());
                    if (!Objects.equals(snapshot, lastSent)) {
                        LOGGER.info("Discord presence updated: {} / {}", snapshot.details(), snapshot.state());
                    }
                    lastSent = snapshot;
                } catch (Exception e) {
                    // Discord closed the pipe (quit, crashed, etc.) - drop it and reconnect on the next loop.
                    LOGGER.debug("Discord IPC write failed, will reconnect: {}", e.toString());
                    closeQuietly(ipc);
                    ipc = null;
                }
            } catch (Throwable t) {
                // Absolute safety net: whatever happened, forget the
                // connection and keep the loop alive rather than let this
                // thread die or, worse, throw somewhere unexpected.
                closeQuietly(ipc);
                ipc = null;
            }

            sleep(UPDATE_INTERVAL_MS);
        }

        closeQuietly(ipc);
    }

    private static DiscordIpcClient tryConnect() {
        try {
            return DiscordIpcClient.connect(CLIENT_ID);
        } catch (Exception e) {
            // Discord not running / not installed / handshake rejected - all logged
            // inside DiscordIpcClient already; just try again later.
            LOGGER.debug("Discord IPC connect attempt failed: {}", e.toString());
            return null;
        }
    }

    private static void closeQuietly(DiscordIpcClient ipc) {
        if (ipc != null) {
            try {
                ipc.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Immutable snapshot of what the presence should currently show. */
    private record PresenceState(String details, String state, long startEpochSeconds) {

        static PresenceState menu(long sessionStart) {
            return new PresenceState("In the Main Menu", null, sessionStart);
        }

        static PresenceState capture(MinecraftClient client, long sessionStart) {
            if (client.world == null) {
                return menu(sessionStart);
            }

            if (client.isIntegratedServerRunning()) {
                return new PresenceState("Playing Singleplayer", null, sessionStart);
            }

            if (ModConfig.INSTANCE.streamerMode) {
                // Streamer Mode: don't leak which server the player is on
                // to anyone viewing their Discord profile/status.
                return new PresenceState("Playing Multiplayer", null, sessionStart);
            }

            ServerInfo server = client.getCurrentServerEntry();
            String name = server != null && server.name != null && !server.name.isBlank()
                    ? server.name
                    : "a server";
            return new PresenceState("Playing Multiplayer", "On " + name, sessionStart);
        }
    }
}
