package com.craftcloudclient.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.util.Arrays;

/**
 * Decodes and caches the current server's list icon (the small favicon
 * shown next to its name on the vanilla multiplayer screen) as a GPU
 * texture, so {@link ServerBannerModule} can show the real server logo
 * instead of a plain accent-colored dot.
 *
 * The icon only needs to be decoded/uploaded once per join - not every
 * frame - so this caches the last-seen icon bytes alongside the
 * texture id it produced, and only redoes the work when the bytes
 * change (i.e. when {@link MinecraftClient#getCurrentServerEntry()}
 * starts pointing at a different server or a different icon).
 */
final class ServerIconTexture {

    private static final Identifier TEXTURE_ID = Identifier.of("craftcloudclient", "server_icon");

    private static byte[] cachedIconData;
    private static boolean cachedIconValid;

    private ServerIconTexture() {
    }

    /**
     * @return the texture id for the current server's icon, already
     * registered with the {@link TextureManager} and ready to draw, or
     * {@code null} if there isn't one (LAN/direct-connect with no
     * server-list entry, or an entry with no icon set).
     */
    static Identifier get(MinecraftClient client) {
        ServerInfo entry = client.getCurrentServerEntry();
        byte[] iconData = (entry == null) ? null : entry.getFavicon();

        if (iconData == null || iconData.length == 0) {
            cachedIconData = null;
            cachedIconValid = false;
            return null;
        }

        if (cachedIconData != null && Arrays.equals(iconData, cachedIconData)) {
            return cachedIconValid ? TEXTURE_ID : null;
        }

        cachedIconData = Arrays.copyOf(iconData, iconData.length);
        cachedIconValid = false;

        try {
            NativeImage image = NativeImage.read(iconData);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "craftcloudclient/server_icon", image);
            client.getTextureManager().registerTexture(TEXTURE_ID, texture);
            cachedIconValid = true;
            return TEXTURE_ID;
        } catch (Exception e) {
            // Malformed/undecodable icon data (rare, but server-supplied
            // favicons aren't guaranteed valid) - fall back to the dot.
            return null;
        }
    }
}
