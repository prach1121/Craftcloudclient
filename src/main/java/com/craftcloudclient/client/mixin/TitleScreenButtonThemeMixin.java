package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.gui.GlassTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays the Craftcloud "glass" look on top of vanilla buttons on both
 * the main menu (Singleplayer, Multiplayer, Realms, Mods, Options, Quit
 * Game) and the in-game pause menu (Back to Game, Advancements, Statistics,
 * Mods, Options, Server Links, Player Reporting, Disconnect).
 *
 * This mixes into {@code PressableWidget} - the class that actually owns
 * the final {@code renderWidget(...)} method's bytecode - and injects at
 * the TAIL of it, so this fires right after vanilla has already drawn its
 * normal background and label for that button. {@code ButtonWidget} itself
 * (what both screens' buttons are made of) inherits this method without
 * overriding it, so mixing into {@code PressableWidget} covers it. We only
 * add a translucent tint, a top sheen and an accent border on top; we never
 * touch the text itself, so nothing about the button's behavior or label
 * changes.
 *
 * Gated to {@code TitleScreen}/{@code GameMenuScreen} only via
 * {@code MinecraftClient.currentScreen}, since without that check this
 * would tint every pressable widget in every screen in the game (inventory,
 * chat, etc.) - including our own {@code GlassButton}, which only overrides
 * the separate {@code drawIcon} hook and never touches {@code renderWidget},
 * so there's no double-styling there either way.
 *
 * Note: any third-party launcher sidebar (things like "Host" / "Social" /
 * "Wardrobe" seen bolted onto some title screens) is not part of vanilla
 * or this mod, so it isn't - and can't safely be - reskinned here.
 */
@Mixin(PressableWidget.class)
public class TitleScreenButtonThemeMixin {

    @Inject(
            method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("TAIL"),
            require = 0
    )
    private void craftcloudclient$paintGlassOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object currentScreen = MinecraftClient.getInstance().currentScreen;
        if (!(currentScreen instanceof TitleScreen) && !(currentScreen instanceof GameMenuScreen)) {
            return;
        }

        PressableWidget self = (PressableWidget) (Object) this;
        boolean hovered = self.isHovered();
        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();

        int tint = hovered ? 0x552A6E86 : 0x33173A47;
        context.fill(x, y, x + w, y + h, tint);

        int sheen = Math.max(1, h / 3);
        context.fillGradient(x, y, x + w, y + sheen, hovered ? 0x33FFFFFF : 0x1EFFFFFF, 0x00FFFFFF);

        int border = hovered ? GlassTheme.ACCENT : GlassTheme.PANEL_BORDER;
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        context.fill(x, y, x + 1, y + h, border);
        context.fill(x + w - 1, y, x + w, y + h, border);
    }
}
