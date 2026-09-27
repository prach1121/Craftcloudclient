package com.craftcloudclient.client.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

/**
 * Backs the "Say GG" keybind: sends a single "gg" chat message and then
 * enforces a cooldown before it can fire again.
 *
 * The cooldown exists purely to stop accidental spam - a key held down,
 * a sticky keyboard, a double-tap in the heat of a fight - from flooding
 * chat with repeated "gg"s, which plenty of servers auto-mute or kick for.
 * It is a local rate limit only; it doesn't change how often the key can
 * be *pressed*, only how often a press actually reaches chat.
 */
public final class GgSender {

    private static final long COOLDOWN_MS = 5000L;
    private static final String MESSAGE = "gg";

    private static long lastSentMs = 0L;

    private GgSender() {}

    /** Call this once per keybind press (from wasPressed()'s while-loop). */
    public static void trigger() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        long now = System.currentTimeMillis();
        long remainingMs = COOLDOWN_MS - (now - lastSentMs);
        if (remainingMs > 0) {
            long remainingSeconds = (remainingMs + 999) / 1000; // round up so it never shows "0s" while still blocked
            client.player.sendMessage(
                    Text.literal("GG is on cooldown (" + remainingSeconds + "s)"),
                    true // action-bar only, never touches actual chat
            );
            return;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) return;

        handler.sendChatMessage(MESSAGE);
        lastSentMs = now;
    }
}
