package com.craftcloudclient.client.combat;

import com.craftcloudclient.client.config.ModConfig;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/**
 * Small crosshair pulse (plus an optional click sound) whenever the player's
 * attack connects with an entity - the same kind of hit-confirmation
 * feedback most modern shooters give you. Purely cosmetic/audio: it doesn't
 * change damage, range, targeting, or anything else about combat, and it
 * only reacts to attacks the player already initiated by clicking - it
 * can't fire on its own.
 */
public final class HitMarkerModule {

    private static final int PULSE_DURATION_TICKS = 6;

    private int ticksRemaining = 0;

    public void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity != player) {
                onHit();
            }
            return ActionResult.PASS;
        });
    }

    private void onHit() {
        ModConfig cfg = ModConfig.INSTANCE;
        if (!cfg.hitMarkersEnabled) return;

        ticksRemaining = PULSE_DURATION_TICKS;

        if (cfg.hitMarkerSoundEnabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.8f));
        }
    }

    /** Call once per client tick. */
    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    /** Call once per rendered frame, after the rest of the HUD. */
    public void render(DrawContext context) {
        if (ticksRemaining <= 0 || !ModConfig.INSTANCE.hitMarkersEnabled) return;

        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;

        float progress = ticksRemaining / (float) PULSE_DURATION_TICKS;
        int alpha = Math.max(0, Math.min(255, (int) (progress * 255)));
        int color = (alpha << 24) | 0xFFFFFF;

        int size = 4 + (int) ((1f - progress) * 3f);
        int gap = 3;

        context.fill(cx - gap - size, cy - 1, cx - gap, cy + 1, color);
        context.fill(cx + gap, cy - 1, cx + gap + size, cy + 1, color);
        context.fill(cx - 1, cy - gap - size, cx + 1, cy - gap, color);
        context.fill(cx - 1, cy + gap, cx + 1, cy + gap + size, color);
    }
}
