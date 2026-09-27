package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the player hide vanilla's boss bar HUD (the bar(s) shown top-center
 * for the Ender Dragon, Wither, or any server-sent boss bar) via the
 * "Hide Boss Bar" toggle on the config screen's HUD tab.
 *
 * This only cancels {@link BossBarHud#render(DrawContext)}, i.e. the drawing
 * step - it does not touch {@code handlePacket}, so the underlying boss bar
 * data (and therefore sky darkening, fog thickening, and dragon music, which
 * all read from that same data) keeps working exactly as if the bar were
 * still visible. Only the on-screen bar itself disappears.
 */
@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void craftcloudclient$onRender(DrawContext context, CallbackInfo ci) {
        if (ModConfig.INSTANCE.hideBossBar) {
            ci.cancel();
        }
    }
}
