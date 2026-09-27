package com.craftcloudclient.client.items;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.Map;

/**
 * Warns the player - one action-bar line plus a short "ding", distinct
 * from {@link com.craftcloudclient.client.combat.HitMarkerModule}'s
 * click - the moment any equipped item or armor piece's durability drops
 * to the low-durability threshold. ArmorDurabilityModule's HUD numbers
 * are passive (you have to be looking at that corner of the screen);
 * this makes sure a favorite pickaxe or elytra doesn't just snap
 * mid-use without ever having been noticed.
 *
 * Deliberately not configurable beyond a single on/off toggle in
 * settings - a threshold slider and per-slot options would be a lot of
 * new UI for what's meant to just work as a "heads up" safety net.
 */
public final class DurabilityWarningManager {

    /** Warn once an item has this fraction of its durability (or less) remaining. */
    private static final float THRESHOLD = 0.10f;

    /** Minimum ticks between repeat warnings for the *same* item stack, so it doesn't ding every tick while sitting just under the threshold. */
    private static final int COOLDOWN_TICKS = 20 * 30;

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<EquipmentSlot, Integer> lastWarnedTick = new EnumMap<>(EquipmentSlot.class);
    // Tracks *which* ItemStack instance a slot was last warned about (by
    // identity, not just type) so swapping in a fresh replacement item of
    // the same kind - e.g. a brand new pickaxe - can warn again right
    // away instead of inheriting the old item's cooldown.
    private static final Map<EquipmentSlot, ItemStack> lastWarnedStack = new EnumMap<>(EquipmentSlot.class);

    private DurabilityWarningManager() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (!ModConfig.INSTANCE.durabilityWarningEnabled) return;

        int now = player.age;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable()) {
                continue;
            }

            float remaining = 1f - ((float) stack.getDamage() / stack.getMaxDamage());
            if (remaining > THRESHOLD) {
                continue;
            }

            ItemStack lastStack = lastWarnedStack.get(slot);
            Integer lastTick = lastWarnedTick.get(slot);
            boolean sameStackAsLastWarning = lastStack != null && lastStack == stack;
            if (sameStackAsLastWarning && lastTick != null && now - lastTick < COOLDOWN_TICKS) {
                continue;
            }

            lastWarnedTick.put(slot, now);
            lastWarnedStack.put(slot, stack);

            player.sendMessage(Text.literal(stack.getName().getString() + " is about to break!"), true);
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.6f));
        }
    }
}
