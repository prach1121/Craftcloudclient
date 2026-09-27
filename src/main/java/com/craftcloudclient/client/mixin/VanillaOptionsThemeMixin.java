package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.gui.GlassTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends the Craftcloud "glass" look onto vanilla's own settings screens
 * (Options..., Video Settings, Controls, Sound, Skin Customization,
 * Accessibility, etc.) instead of only our own config menu.
 *
 * This mixes into the base {@link Screen} class rather than
 * {@code GameOptionsScreen} directly. Without a refmap (e.g. running the
 * built jar instead of through Loom's dev run), Mixin's {@code @Shadow}
 * can only resolve fields that are actually *declared* on the exact target
 * class - and {@code width}/{@code height} are declared on {@code Screen},
 * not on {@code GameOptionsScreen} - so shadowing them from
 * {@code GameOptionsScreen} failed at runtime with
 * "field ... was not located in the target class". Targeting {@code Screen}
 * itself, where they're actually declared, avoids that.
 *
 * Because this now technically runs for every screen, all the drawing is
 * gated behind an {@code instanceof GameOptionsScreen} check below, so
 * every other screen (inventory, chat, our own {@code ConfigScreen}, the
 * title screen, ...) renders exactly as it did before this mixin existed.
 */
@Mixin(Screen.class)
public class VanillaOptionsThemeMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("TAIL"),
            require = 0
    )
    private void craftcloudclient$paintGlassTheme(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(((Object) this) instanceof GameOptionsScreen)) {
            return;
        }

        // Top accent hairline, same two-tone gradient used on our own menu.
        GlassTheme.accentLine(context, 0, 0, this.width);

        // Faint full-frame border so the screen reads as "themed" even
        // though the widgets underneath are still vanilla.
        context.fill(0, 0, this.width, 1, GlassTheme.PANEL_BORDER);
        context.fill(0, this.height - 1, this.width, this.height, GlassTheme.PANEL_BORDER);
        context.fill(0, 0, 1, this.height, GlassTheme.PANEL_BORDER);
        context.fill(this.width - 1, 0, this.width, this.height, GlassTheme.PANEL_BORDER);

        // Small dim corner watermark, consistent with the branding on our
        // own menu screen.
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTextWithShadow(client.textRenderer, "Craftcloud Client", 6, this.height - 14, 0x556B7688);
    }
}
