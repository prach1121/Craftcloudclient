package com.craftcloudclient.client.stats;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.EnumMap;
import java.util.Map;

/**
 * Wires up every stat-tracking signal and feeds counts into {@link StatsManager}.
 * Three independent signals, each picked for being the least fragile way to
 * observe that category of event from purely client-side code - see the
 * comment on {@link com.craftcloudclient.client.hud.CpsModule} for why deep
 * mixins into the interaction pipeline are treated as off-limits here:
 *
 * <ul>
 *   <li><b>Hits</b> - Fabric API's {@link AttackEntityCallback}, which fires
 *   exactly when the local player's attack is recognized against an entity
 *   in range. This is the same "an attack happened" signal every combat HUD
 *   mod relies on. It doesn't guarantee the server accepted the hit (lag,
 *   invulnerability frames, shields, etc. can still no-op it server-side).</li>
 *
 *   <li><b>Placements</b> - polled once per tick from {@link #tick()}: if the
 *   use-item key is held down and a tracked item's held stack count drops by
 *   exactly one from the previous tick, that's counted as one placement.
 *   Known trade-offs: it won't see placements in creative mode (stack count
 *   never drops), and swapping hotbar slots between two stacks of the same
 *   tracked item on the exact tick the count happens to drop by one could
 *   rarely be misread as a placement.</li>
 *
 *   <li><b>Kills</b> - death messages are parsed as they arrive on the
 *   game/system chat channel. Vanilla death messages always end with
 *   "... by &lt;killer&gt;" (optionally followed by "using &lt;item&gt;"),
 *   so a message counts as one of our kills exactly when that killer name
 *   matches the local player's name. Servers that fully replace death
 *   messages with custom text won't be picked up by this.</li>
 * </ul>
 */
public final class StatsListener {

    private static final Map<Hand, Item> lastHeldItem = new EnumMap<>(Hand.class);
    private static final Map<Hand, Integer> lastHeldCount = new EnumMap<>(Hand.class);

    private StatsListener() {}

    /** Call once during mod init to register the event-based hooks (hits, kills). */
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // This mod is client-only (see fabric.mod.json's "environment": "client"),
            // so AttackEntityCallback only ever fires on the client side here anyway -
            // no need to check world.isClient (and recent mappings don't expose it as
            // a public accessor).
            if (hand == Hand.MAIN_HAND) {
                StatsManager.INSTANCE.record(StatCategory.HITS);
            }
            return ActionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // ignore the action-bar channel, only care about chat/system log lines
            onGameMessage(message.getString());
        });
    }

    private static void onGameMessage(String plain) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int byIndex = plain.lastIndexOf(" by ");
        if (byIndex < 0) return;

        String killer = plain.substring(byIndex + 4).trim();
        int usingIndex = killer.indexOf(" using ");
        if (usingIndex >= 0) {
            killer = killer.substring(0, usingIndex).trim();
        }

        String ourName = client.getSession().getUsername();
        if (!killer.isEmpty() && killer.equals(ourName)) {
            StatsManager.INSTANCE.record(StatCategory.KILLS);
            KillStreakManager.INSTANCE.onKill();
        }
    }

    /** Call once per client tick from {@link com.craftcloudclient.client.CraftcloudClient} to poll for placements. */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            lastHeldItem.clear();
            lastHeldCount.clear();
            return;
        }

        boolean usePressed = client.options.useKey.isPressed();
        checkHand(client, Hand.MAIN_HAND, usePressed);
        checkHand(client, Hand.OFF_HAND, usePressed);
    }

    private static void checkHand(MinecraftClient client, Hand hand, boolean usePressed) {
        ItemStack stack = client.player.getStackInHand(hand);
        Item item = stack.getItem();
        int count = stack.getCount();

        Item prevItem = lastHeldItem.get(hand);
        Integer prevCount = lastHeldCount.get(hand);

        if (usePressed && item == prevItem && prevCount != null && count == prevCount - 1) {
            StatCategory category = categoryFor(item);
            if (category != null) {
                StatsManager.INSTANCE.record(category);
            }
        }

        lastHeldItem.put(hand, item);
        lastHeldCount.put(hand, count);
    }

    private static StatCategory categoryFor(Item item) {
        if (item == Items.END_CRYSTAL) return StatCategory.END_CRYSTAL;
        if (item == Items.RESPAWN_ANCHOR) return StatCategory.RESPAWN_ANCHOR;
        if (item == Items.OBSIDIAN) return StatCategory.OBSIDIAN;
        return null;
    }
}
