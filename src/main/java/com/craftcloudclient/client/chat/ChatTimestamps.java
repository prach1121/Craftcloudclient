package com.craftcloudclient.client.chat;

import com.craftcloudclient.client.config.ModConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Prefixes incoming chat lines with a local "[HH:mm]" timestamp - purely
 * a client-side display change via Fabric API's message-receive event,
 * so nothing sent to the server or seen by other players is touched.
 * Hooking the event instead of mixin-ing into ChatHud means this can't
 * drift out of sync with whatever internal rendering method that screen
 * renames between versions.
 */
public final class ChatTimestamps {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ChatTimestamps() {}

    public static void register() {
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (overlay) {
                // The "overlay" message is the action-bar line, not the
                // chat log - leave that alone entirely.
                return message;
            }
            if (!ModConfig.INSTANCE.chatTimestampsEnabled) {
                return message;
            }
            String stamp = "[" + LocalTime.now().format(FORMAT) + "] ";
            return Text.literal(stamp).formatted(Formatting.GRAY).append(message);
        });
    }
}
