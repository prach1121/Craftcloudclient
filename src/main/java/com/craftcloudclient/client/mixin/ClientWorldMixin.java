package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts all client-side particle spawn entry points on {@link ClientWorld}
 * so we can selectively drop specific particle effects (end crystal explosions,
 * totem of undying) purely for visual clarity in PvP. This does not touch any
 * game logic, damage calculation, or network packets - it only stops a
 * particle from being handed to the renderer.
 *
 * As of Minecraft 1.21.11, the old addParticle(...)/addImportantParticle(...)
 * methods on ClientWorld were consolidated into a single addParticleClient(...)
 * overload family. All three known overloads are hooked below.
 */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(
            method = "addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void craftcloudclient$onAddParticleClient(ParticleEffect parameters, double x, double y, double z,
                                                   double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (shouldCancel(parameters)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addParticleClient(Lnet/minecraft/particle/ParticleEffect;ZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void craftcloudclient$onAddParticleClientForced(ParticleEffect parameters, boolean force, double x, double y, double z,
                                                         double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (shouldCancel(parameters)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addParticleClient(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void craftcloudclient$onAddParticleClientForcedMinimal(ParticleEffect parameters, boolean force, boolean canSpawnOnMinimal,
                                                                double x, double y, double z,
                                                                double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (shouldCancel(parameters)) {
            ci.cancel();
        }
    }

    private static boolean shouldCancel(ParticleEffect parameters) {
        ModConfig cfg = ModConfig.INSTANCE;
        if (!cfg.hideCrystalParticles && !cfg.hideTotemParticles) {
            return false;
        }

        var type = parameters.getType();

        if (cfg.hideCrystalParticles
                && (type == ParticleTypes.EXPLOSION_EMITTER || type == ParticleTypes.EXPLOSION)) {
            return true;
        }

        if (cfg.hideTotemParticles && type == ParticleTypes.TOTEM_OF_UNDYING) {
            return true;
        }

        return false;
    }
}
