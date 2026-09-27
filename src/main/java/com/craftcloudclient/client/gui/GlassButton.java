package com.craftcloudclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.BooleanSupplier;

public class GlassButton extends ButtonWidget {

    private final String label;
    private final BooleanSupplier isSelected;

    public GlassButton(int x, int y, int width, int height, String label, Runnable action) {
        this(x, y, width, height, label, action, () -> false);
    }

    /** Variant that also draws a persistent "selected" highlight (not just hover), e.g. for a theme swatch list. */
    public GlassButton(int x, int y, int width, int height, String label, Runnable action, BooleanSupplier isSelected) {
        super(x, y, width, height, net.minecraft.text.Text.literal(label), button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.isSelected = isSelected;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        boolean selected = isSelected.getAsBoolean();

        int fill = selected ? 0xDD2A6E86 : (hovered ? 0xCC2A6E86 : 0xAA173A47);
        int borderColor = selected ? GlassTheme.ACCENT : (hovered ? GlassTheme.ACCENT : GlassTheme.PANEL_BORDER);
        int radius = Math.min(GlassTheme.ROW_RADIUS, this.getHeight() / 2);
        GlassTheme.strokeRounded(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), fill, borderColor, radius);
        GlassTheme.roundedTopSheen(context, this.getX(), this.getY(), this.getWidth(), Math.max(1, this.getHeight() / 2), radius,
                hovered || selected ? 0x33FFFFFF : 0x1EFFFFFF, 0x00FFFFFF, true, true);

        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(label);
        int textX = this.getX() + (this.getWidth() - textWidth) / 2;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        int textColor = selected ? GlassTheme.ACCENT : GlassTheme.TEXT_MAIN;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, textX, textY, textColor);

        if (selected) {
            int check = 8;
            context.fill(this.getX() + 6, this.getY() + (this.getHeight() - check) / 2, this.getX() + 6 + check, this.getY() + (this.getHeight() + check) / 2, GlassTheme.ACCENT);
        }
    }
}
