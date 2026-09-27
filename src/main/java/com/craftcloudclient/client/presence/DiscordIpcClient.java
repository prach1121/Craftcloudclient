package com.craftcloudclient.client.presence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Minimal implementation of Discord's local IPC protocol
 * (https://discord.com/developers/docs/topics/rpc), just enough to set a
 * Rich Presence activity. Deliberately dependency-free - talks straight to
 * the platform transport Discord listens on rather than pulling in a JNA
 * wrapper or a native discord-rpc build:
 *
 * <ul>
 *   <li>Windows: Discord listens on a named pipe ({@code \\.\pipe\discord-ipc-N}).
 *       There's no first-class named-pipe client in the JDK, but opening
 *       that path with {@link RandomAccessFile} in {@code "rw"} mode works
 *       in practice and is the same trick several other pure-Java Discord
 *       IPC clients use.</li>
 *   <li>Linux/macOS: Discord listens on a Unix domain socket under a temp/
 *       runtime directory. {@link SocketChannel} has supported
 *       {@link StandardProtocolFamily#UNIX} natively since Java 16, so no
 *       extra library is needed here either.</li>
 * </ul>
 *
 * One instance = one connection. Callers only ever see {@link IOException}
 * out of this class (never a runtime crash from a malformed response) -
 * every failure mode collapses to "the write/connect didn't work", which is
 * exactly what {@link DiscordPresenceManager} needs to decide whether to
 * retry.
 *
 * {@link #connect} reads back Discord's handshake response (with a short
 * timeout) purely to log whether it was accepted - if the client ID isn't a
 * real/registered application, Discord accepts the pipe/socket connection
 * fine but replies with an error frame instead of READY, which would
 * otherwise look identical to "working" from the outside. Every other
 * frame after that is still write-only, same as before.
 */
final class DiscordIpcClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger("craftcloudclient/discord-rpc");

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");

    private RandomAccessFile windowsPipe;
    private SocketChannel unixSocket;

    private DiscordIpcClient() {}

    /**
     * Opens a connection and performs the initial handshake. Throws
     * {@link IOException} if Discord isn't running, no pipe/socket could be
     * opened, or the handshake was rejected - callers should treat that as
     * "try again later", not a fatal error.
     */
    static DiscordIpcClient connect(String clientId) throws IOException {
        DiscordIpcClient client = new DiscordIpcClient();
        if (IS_WINDOWS) {
            client.connectWindows();
        } else {
            client.connectUnix();
        }
        LOGGER.info("Discord IPC transport connected ({})", IS_WINDOWS ? "named pipe" : "unix socket");

        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", 1);
        handshake.addProperty("client_id", clientId);
        client.writeFrame(OP_HANDSHAKE, handshake.toString());

        client.confirmHandshake();
        return client;
    }

    /**
     * Reads Discord's response to the handshake (should be a DISPATCH/READY
     * event) and logs the outcome. This is the only frame we ever read -
     * everything after this stays write-only (see class javadoc). A missing
     * response within the timeout, or an explicit error frame, both throw
     * so the caller retries instead of sitting on a dead connection.
     */
    private void confirmHandshake() throws IOException {
        String body;
        try {
            body = readFrameWithTimeout(2000);
        } catch (IOException e) {
            LOGGER.warn("Discord IPC handshake failed: no response ({})", e.toString());
            throw e;
        }

        if (body == null) {
            LOGGER.warn("Discord IPC handshake timed out waiting for a response - closing and will retry");
            throw new IOException("Handshake timed out");
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String evt = json.has("evt") && !json.get("evt").isJsonNull() ? json.get("evt").getAsString() : null;
            if ("ERROR".equals(evt)) {
                String message = json.has("data") && json.getAsJsonObject("data").has("message")
                        ? json.getAsJsonObject("data").get("message").getAsString()
                        : body;
                LOGGER.warn("Discord rejected the handshake: {}", message);
                throw new IOException("Discord rejected handshake: " + message);
            }
            LOGGER.info("Discord IPC handshake accepted (evt={})", evt);
        } catch (com.google.gson.JsonSyntaxException e) {
            LOGGER.warn("Discord IPC handshake response wasn't valid JSON: {}", body);
        }
    }

    private void connectWindows() throws IOException {
        IOException last = null;
        for (int i = 0; i < 10; i++) {
            try {
                windowsPipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                return;
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("No Discord IPC pipe found");
    }

    private void connectUnix() throws IOException {
        for (Path dir : candidateUnixDirs()) {
            for (int i = 0; i < 10; i++) {
                Path socketPath = dir.resolve("discord-ipc-" + i);
                if (!Files.exists(socketPath)) continue;
                try {
                    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    channel.connect(UnixDomainSocketAddress.of(socketPath));
                    unixSocket = channel;
                    return;
                } catch (IOException ignored) {
                    // Stale/unbound socket file - try the next candidate.
                }
            }
        }
        throw new IOException("No Discord IPC socket found");
    }

    /** Every directory Discord (stock, Flatpak, or Snap) is known to drop its IPC sockets in. */
    private static Iterable<Path> candidateUnixDirs() {
        java.util.List<Path> dirs = new java.util.ArrayList<>();
        String[] envVars = {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
        for (String var : envVars) {
            String value = System.getenv(var);
            if (value == null || value.isBlank()) continue;
            Path base = Path.of(value);
            dirs.add(base);
            dirs.add(base.resolve("app/com.discordapp.Discord"));
            dirs.add(base.resolve("snap.discord"));
        }
        dirs.add(Path.of("/tmp"));
        return dirs;
    }

    /**
     * Sets the Rich Presence activity shown on this account. {@code state}/
     * {@code details} may be null to omit that line.
     */
    void setActivity(String details, String state, long startEpochSeconds) throws IOException {
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", startEpochSeconds);

        JsonObject activity = new JsonObject();
        if (details != null) activity.addProperty("details", details);
        if (state != null) activity.addProperty("state", state);
        activity.add("timestamps", timestamps);

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", UUID.randomUUID().toString());

        writeFrame(OP_FRAME, payload.toString());
    }

    private void writeFrame(int opcode, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(payload.length);
        header.flip();

        if (windowsPipe != null) {
            windowsPipe.write(header.array());
            windowsPipe.write(payload);
        } else if (unixSocket != null) {
            unixSocket.write(header);
            unixSocket.write(ByteBuffer.wrap(payload));
        } else {
            throw new IOException("Not connected");
        }
    }

    /**
     * Blocks (on a throwaway daemon thread, so a stuck read can never hang
     * the caller) waiting for one frame, up to {@code timeoutMs}. Returns
     * the frame's JSON body, or {@code null} on timeout. If nothing ever
     * arrives, this closes the connection to unstick the reader thread
     * rather than leaking it indefinitely.
     */
    private String readFrameWithTimeout(long timeoutMs) throws IOException {
        final String[] result = new String[1];
        final IOException[] failure = new IOException[1];

        Thread reader = new Thread(() -> {
            try {
                result[0] = readFrameBlocking();
            } catch (IOException e) {
                failure[0] = e;
            }
        }, "craftcloudclient-discord-rpc-handshake-read");
        reader.setDaemon(true);
        reader.start();

        try {
            reader.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (reader.isAlive()) {
            // Nothing arrived in time - closing unblocks the reader thread's
            // pending read (it'll throw and exit) instead of leaving it
            // parked forever.
            close();
            return null;
        }

        if (failure[0] != null) throw failure[0];
        return result[0];
    }

    private String readFrameBlocking() throws IOException {
        byte[] header = new byte[8];
        if (windowsPipe != null) {
            windowsPipe.readFully(header);
        } else if (unixSocket != null) {
            readFully(unixSocket, ByteBuffer.wrap(header));
        } else {
            throw new IOException("Not connected");
        }

        int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        byte[] payload = new byte[length];
        if (windowsPipe != null) {
            windowsPipe.readFully(payload);
        } else {
            readFully(unixSocket, ByteBuffer.wrap(payload));
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static void readFully(SocketChannel channel, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            if (channel.read(buf) < 0) throw new ClosedChannelException();
        }
    }

    @Override
    public void close() {
        try {
            if (windowsPipe != null) windowsPipe.close();
        } catch (IOException ignored) {
        }
        try {
            if (unixSocket != null) unixSocket.close();
        } catch (IOException ignored) {
        }
    }
}

