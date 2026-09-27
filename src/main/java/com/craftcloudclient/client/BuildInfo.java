package com.craftcloudclient.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * Central source of truth for the version strings shown on the config
 * screen's About tab.
 *
 * <p>{@link #VERSION} is read straight from the mod's own Fabric Loader
 * metadata, so it always matches {@code mod_version} in
 * {@code gradle.properties} (and therefore {@code fabric.mod.json}) with
 * no risk of the two drifting apart.
 *
 * <p>{@link #BUILD} is a separate, more granular identifier - stamped in
 * at compile time from {@code craftcloudclient_build.properties}, which
 * Gradle's {@code processResources} task generates fresh on every build
 * (see {@code build.gradle}). This lets two jars that share the same
 * {@code VERSION} (e.g. two dev builds between releases) still be told
 * apart when someone reports a bug. When that generated resource isn't
 * present - e.g. running straight from an IDE without a Gradle build -
 * this falls back to {@code "dev"} rather than failing.
 */
public final class BuildInfo {

    public static final String VERSION;
    public static final String BUILD;

    static {
        VERSION = FabricLoader.getInstance()
                .getModContainer("craftcloudclient")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        BUILD = readBuildId().orElse("dev");
    }

    private static Optional<String> readBuildId() {
        try (InputStream in = BuildInfo.class.getResourceAsStream("/craftcloudclient_build.properties")) {
            if (in == null) {
                return Optional.empty();
            }
            Properties props = new Properties();
            props.load(in);
            String build = props.getProperty("build");
            return (build == null || build.isBlank() || build.startsWith("$")) ? Optional.empty() : Optional.of(build);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private BuildInfo() {}
}
