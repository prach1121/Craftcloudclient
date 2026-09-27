package com.craftcloudclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.BooleanSupplier;

/**
 * A single row in the sidebar navigation list: a colored dot "icon",
 * a label, and a left accent bar that lights up when this tab is the
 * active one. Modeled after the category list used by other polished
 * third-party clients (a vertical tab rail instead of one long
 * scrolling settings page).
 */
public class GlassTabButton extends ButtonWidget {

    private final String label;
    private final int accent;
    private final BooleanSupplier isActive;
    private final BooleanSupplier showBadge;

    public GlassTabButton(int x, int y, int width, int height, String label, int accent,
                           BooleanSupplier isActive, Runnable onClick) {
        this(x, y, width, height, label, accent, isActive, onClick, () -> false);
    }

    /**
     * Same as the other constructor, plus a small red notification dot
     * in the top-right corner while {@code showBadge} is true - used by
     * the About tab to flag an available update without the player
     * having to go looking for it.
     */
    public GlassTabButton(int x, int y, int width, int height, String label, int accent,
                           BooleanSupplier isActive, Runnable onClick, BooleanSupplier showBadge) {
        super(x, y, width, height, net.minecraft.text.Text.literal(label), button -> onClick.run(), DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.accent = accent;
        this.isActive = isActive;
        this.showBadge = showBadge;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean active = isActive.getAsBoolean();
        boolean hovered = this.isHovered();

        GlassTheme.tab(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), active, hovered, accent);

        int dotSize = 6;
        int dotX = this.getX() + 12;
        int dotY = this.getY() + (this.getHeight() - dotSize) / 2;
        GlassTheme.dot(context, dotX, dotY, dotSize, active ? accent : 0x66FFFFFF);

        int textColor = active ? GlassTheme.TEXT_MAIN : GlassTheme.TEXT_DIM;
        int textX = dotX + dotSize + 8;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, textX, textY, textColor);

        if (showBadge.getAsBoolean()) {
            int badgeSize = 6;
            int badgeX = this.getX() + this.getWidth() - badgeSize - 4;
            int badgeY = this.getY() + 4;
            GlassTheme.dot(context, badgeX, badgeY, badgeSize, 0xFFFF5555);
        }
    }
}
