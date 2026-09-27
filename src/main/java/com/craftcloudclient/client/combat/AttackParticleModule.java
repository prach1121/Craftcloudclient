package com.craftcloudclient.client.combat;

import com.craftcloudclient.client.config.AttackParticleType;
import com.craftcloudclient.client.config.ModConfig;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.random.Random;

/**
 * Small colored spark burst - styled after firework sparks - spawned at
 * the point of impact whenever the player's attack connects with an
 * entity. Configurable in Settings > PvP: on/off, a Red/Yellow/Orange
 * color, and how many sparks spawn per hit (3-18).
 *
 * Purely cosmetic, same as {@link HitMarkerModule}: it doesn't change
 * damage, range, or targeting, and it only reacts to attacks the player
 * already initiated by clicking - it can't fire on its own.
 *
 * Implementation note: vanilla's actual "firework" particle
 * (minecraft:firework - the spark trail behind a firework rocket) has no
 * per-spawn color; its color only ever comes from the firework rocket
 * item/entity that spawned it, not from a plain client-side
 * {@code world.addParticleClient(...)} call. To offer a genuine
 * Red/Yellow/Orange choice here, this spawns colored
 * {@link DustParticleEffect} sparks - the same colorable particle type
 * redstone dust uses - tinted to the configured color, with a small
 * random burst velocity so it still reads as a little firework-style
 * flourish rather than a flat dust cloud.
 */
public final class AttackParticleModule {

    private static final Random RANDOM = Random.create();

    /** Enforced range for {@link ModConfig#attackParticleCount}, also mirrored by the config screen's stepper. */
    private static final int MIN_COUNT = 3;
    private static final int MAX_COUNT = 18;

    public void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity != player) {
                spawnBurst(entity);
            }
            return ActionResult.PASS;
        });
    }

    private void spawnBurst(Entity target) {
        ModConfig cfg = ModConfig.INSTANCE;
        if (!cfg.attackParticlesEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.world instanceof ClientWorld world)) return;

        AttackParticleType type = cfg.attackParticleType;
        int count = clampCount(cfg.attackParticleCount);

        // As of the mappings this project builds against, DustParticleEffect
        // takes a packed 0xRRGGBB int rather than a Vector3f - AttackParticleType
        // already stores its tint in that format, so it's passed straight through.
        DustParticleEffect effect = new DustParticleEffect(type.getRgb(), 1.15f);

        double x = target.getX();
        double y = target.getBodyY(0.6);
        double z = target.getZ();

        for (int i = 0; i < count; i++) {
            double velocityX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double velocityY = RANDOM.nextDouble() * 0.45;
            double velocityZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            world.addParticleClient(effect, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    private static int clampCount(int count) {
        return Math.max(MIN_COUNT, Math.min(MAX_COUNT, count));
    }
}
