package com.craftcloudclient.client.update;

import com.craftcloudclient.client.BuildInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Checks Modrinth for a newer release of this mod than the one currently
 * running. Entirely background/async - nothing here ever blocks the game
 * thread. {@link #checkAsync()} is fired once from
 * {@code CraftcloudClient#onInitializeClient()}; the result (if any) is
 * cached in {@link #latest}, which {@code ConfigScreen}'s About tab and
 * sidebar badge, and {@code GameMenuLogoMixin}'s pause-menu banner, just
 * poll every frame via {@link #available()} - there's nothing to await
 * or block on from the render thread.
 */
public final class UpdateChecker {

    /** This mod's Modrinth project ID (https://modrinth.com/mod/lvP71ewW). */
    private static final String PROJECT_ID = "lvP71ewW";
    private static final String GAME_VERSION = "1.21.11";
    private static final String LOADER = "fabric";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Null until a strictly-newer, compatible version has been confirmed available. */
    private static volatile UpdateInfo latest = null;

    private UpdateChecker() {}

    /** @param version the newer version's version_number, e.g. "1.1.0" @param url the direct Modrinth page for that version */
    public record UpdateInfo(String version, String url) {}

    /** Currently-known update, or empty if none found (or the check hasn't finished/run yet). Safe to poll every frame - never blocks. */
    public static Optional<UpdateInfo> available() {
        return Optional.ofNullable(latest);
    }

    /**
     * Fires the Modrinth request on a background thread and returns
     * immediately. Called once at startup; cheap and idempotent, so it's
     * also safe to call again later (e.g. from a future "check now"
     * button) without any extra guarding.
     */
    public static void checkAsync() {
        // Dev/IDE runs (no Fabric Loader metadata to read a real version
        // from - see BuildInfo) have nothing real to compare against, so
        // skip rather than nagging about a phantom update every launch.
        if ("dev".equals(BuildInfo.VERSION)) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"))
                // Modrinth asks API consumers to identify themselves (see
                // https://docs.modrinth.com/api/) rather than send a bare
                // default HttpClient user agent, which they're more likely
                // to rate-limit or block outright.
                .header("User-Agent", "CraftcloudNet/craftcloudclient/" + BuildInfo.VERSION
                        + " (https://modrinth.com/mod/" + PROJECT_ID + ")")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(UpdateChecker::handleResponse)
                .exceptionally(err -> null); // offline / API hiccup - stay silent, there's just nothing to show
    }

    private static void handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) return;

        try {
            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            // Modrinth returns versions newest-first, so the first entry
            // that actually matches our loader + game version is "latest".
            for (JsonElement el : versions) {
                JsonObject version = el.getAsJsonObject();
                if (!supportsCurrentGame(version)) continue;

                String versionNumber = version.get("version_number").getAsString();
                if (versionNumber.equals(BuildInfo.VERSION)) return; // already on it - no update

                latest = new UpdateInfo(versionNumber,
                        "https://modrinth.com/mod/" + PROJECT_ID + "/version/" + versionNumber);
                return;
            }
        } catch (Exception ignored) {
            // Unexpected/malformed response shape - fail quietly, same as a network error above.
        }
    }

    private static boolean supportsCurrentGame(JsonObject version) {
        boolean loaderMatch = false;
        for (JsonElement loader : version.getAsJsonArray("loaders")) {
            if (loader.getAsString().equalsIgnoreCase(LOADER)) {
                loaderMatch = true;
                break;
            }
        }
        if (!loaderMatch) return false;

        for (JsonElement gv : version.getAsJsonArray("game_versions")) {
            if (gv.getAsString().equals(GAME_VERSION)) return true;
        }
        return false;
    }
}
