package com.craftcloudclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The bottom face of a "mod card" tile - a full-width solid pill reading
 * "ENABLED" (green) or "DISABLED" (dim red), matching the always-visible
 * on/off pill under each mod tile in the reference layout, rather than the
 * small left-accented row + switch look {@link GlassToggleRow} uses
 * elsewhere in this menu.
 */
public class GlassEnabledPill extends ButtonWidget {

    private final BooleanSupplier getter;

    public GlassEnabledPill(int x, int y, int width, int height,
                             BooleanSupplier getter, Consumer<Boolean> setter, Runnable onChange) {
        super(x, y, width, height, net.minecraft.text.Text.literal(""), button -> {
            boolean newValue = !getter.getAsBoolean();
            setter.accept(newValue);
            if (onChange != null) onChange.run();
        }, DEFAULT_NARRATION_SUPPLIER);
        this.getter = getter;
    }

    public GlassEnabledPill(int x, int y, int width, int height, BooleanSupplier getter, Consumer<Boolean> setter) {
        this(x, y, width, height, getter, setter, null);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean on = getter.getAsBoolean();
        boolean hovered = this.isHovered();
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();

        // Solid, fully-saturated green when ON / red when OFF (a touch
        // brighter on hover) instead of the old dimmed grey-blue OFF
        // state - "on = green, off = red" should be readable at a glance.
        int base = on ? GlassTheme.ON_COLOR : GlassTheme.OFF_COLOR;
        int fill = hovered ? GlassTheme.blend(base, 0xFFFFFFFF, 0.10f) : base;

        int radius = Math.min(GlassTheme.ROW_RADIUS, h / 2);
        GlassTheme.fillRounded(context, x, y, w, h, fill, radius);
        GlassTheme.roundedTopSheen(context, x, y, w, Math.max(1, h / 2), radius, 0x26FFFFFF, 0x00FFFFFF, true, true);

        String text = on ? "ENABLED" : "DISABLED";
        var tr = MinecraftClient.getInstance().textRenderer;
        int textWidth = tr.getWidth(text);
        int color = on ? 0xFF0B2A14 : 0xFF3A0808;
        context.drawTextWithShadow(tr, text, x + (w - textWidth) / 2, y + (h - 8) / 2, color);
    }
}
