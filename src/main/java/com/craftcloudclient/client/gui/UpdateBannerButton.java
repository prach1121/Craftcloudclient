package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.update.UpdateChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * Top-center "Update available - click to open Modrinth" pill shown on the
 * pause screen.
 *
 * This used to be painted by hand in {@code GameMenuLogoMixin} with its own
 * manually-tracked hit-test rectangle checked from an injected
 * {@code mouseClicked}. That meant the banner could be drawn (it's purely a
 * render-tail injection) while the separate click injection silently never
 * fired or never lined up with it, which is exactly what made the banner
 * effectively unclickable - the two were two different, independently
 * fragile code paths that had to agree with each other by hand every frame.
 *
 * A real {@link ButtonWidget} added through {@code Screen#addDrawableChild}
 * gets its rendering, hovering and click handling entirely from vanilla's
 * normal widget system - the same system {@link GlassButton} already relies
 * on for the equivalent "Update Available" row on the About tab (which,
 * unlike the pause-screen banner, does work) - so there's no separate click
 * math to keep in sync.
 */
public class UpdateBannerButton extends ButtonWidget {

    private static final int PADDING_X = 8;
    private static final int HEIGHT = 16;

    private final String label;

    private UpdateBannerButton(int x, int y, int width, String label, UpdateChecker.UpdateInfo info, Screen parent) {
        super(x, y, width, HEIGHT, net.minecraft.text.Text.literal(label),
                button -> openLink(info, parent), DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
    }

    /**
     * Builds a banner sized to fit {@code info}'s text and horizontally
     * centered on a screen of the given width, or returns {@code null} if
     * there's nothing to show. Called once from {@code GameMenuLogoMixin}'s
     * {@code init()} injection each time the pause screen is (re)built.
     */
    public static UpdateBannerButton create(int screenWidth, UpdateChecker.UpdateInfo info, Screen parent) {
        String text = "Update available: v" + info.version() + " - click to open Modrinth";
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        int width = textWidth + PADDING_X * 2;
        int x = (screenWidth - width) / 2;
        int y = 6;
        return new UpdateBannerButton(x, y, width, text, info, parent);
    }

    private static void openLink(UpdateChecker.UpdateInfo info, Screen parent) {
        MinecraftClient.getInstance().setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                net.minecraft.util.Util.getOperatingSystem().open(info.url());
            }
            MinecraftClient.getInstance().setScreen(parent);
        }, info.url(), true));
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        GlassTheme.panel(context, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        MinecraftClient client = MinecraftClient.getInstance();
        int textY = this.getY() + (this.getHeight() - client.textRenderer.fontHeight) / 2 + 1;
        context.drawTextWithShadow(client.textRenderer, label, this.getX() + PADDING_X, textY, 0xFFFFD166);
    }
}
