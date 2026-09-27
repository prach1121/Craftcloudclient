package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backs the "Hide Players" toggle (Settings > Performance). When enabled, every
 * other player's entity is skipped at the culling step, before any of its
 * render passes (body, armor, held items, nametag) ever run.
 *
 * This mixes into the generic {@link EntityRenderer}, which every entity
 * renderer (including PlayerEntityRenderer) extends, so it only ever needs
 * one injection point regardless of skin/model variant. The client's own
 * player is deliberately exempted so third-person view and any "player
 * preview" UI are unaffected - only *other* players disappear.
 *
 * Purely a rendering skip: hitboxes, collision, tab-list presence, and all
 * actual gameplay state are completely untouched. A hidden player can
 * still hit you and still shows up everywhere that isn't the 3D world
 * render (tab list, scoreboard, chat, etc).
 */
@Mixin(EntityRenderer.class)
public class PlayerRenderMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftcloudclient$onShouldRender(Entity entity, Frustum frustum, double x, double y, double z,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.INSTANCE.hidePlayers) return;
        if (!(entity instanceof PlayerEntity)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player) return;

        cir.setReturnValue(false);
    }
}
