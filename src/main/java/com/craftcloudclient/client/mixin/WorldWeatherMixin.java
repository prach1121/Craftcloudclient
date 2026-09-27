package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backs the "Disable Rain" toggle (Settings > Performance). When enabled,
 * it zeroes out the rain/thunder gradient that drives the rain overlay,
 * sky darkening, and weather fog - purely a rendering value.
 *
 * This mixes into {@link World} (shared by both {@link ClientWorld} and
 * ServerWorld) rather than ClientWorld directly, since getRainGradient/
 * getThunderGradient are only ever declared once, on World itself. The
 * {@code instanceof ClientWorld} check below is what keeps this
 * client-only in practice: a dedicated server's ServerWorld never matches
 * it, and in singleplayer the integrated server's own ServerWorld is left
 * completely alone too, so actual weather state (isRaining/isThundering,
 * crop hydration, cauldron filling, fire extinguishing, lightning
 * strikes) is untouched either way - only what gets drawn on screen
 * changes.
 */
@Mixin(World.class)
public class WorldWeatherMixin {

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftcloudclient$onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (ModConfig.INSTANCE.disableRain && ((Object) this) instanceof ClientWorld) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftcloudclient$onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (ModConfig.INSTANCE.disableRain && ((Object) this) instanceof ClientWorld) {
            cir.setReturnValue(0.0f);
        }
    }
}
