package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.config.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A small widget showing a 2x2 grid icon with the active corner highlighted,
 * plus a short abbreviation (TL/TR/BL/BR). Clicking cycles to the next corner.
 * Built on {@link ButtonWidget} so click handling stays whatever vanilla does.
 */
public class GlassCornerPicker extends ButtonWidget {

    private final Supplier<HudPosition> getter;

    public GlassCornerPicker(int x, int y, int width, int height, Supplier<HudPosition> getter, Consumer<HudPosition> setter) {
        super(x, y, width, height, net.minecraft.text.Text.literal("Position"), button -> setter.accept(getter.get().next()), DEFAULT_NARRATION_SUPPLIER);
        this.getter = getter;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered);

        HudPosition pos = getter.get();

        // Mini 2x2 grid icon on the left side of the widget
        int gridSize = 8;
        int gap = 2;
        int gridX = this.getX() + 8;
        int gridY = this.getY() + (this.getHeight() - (gridSize * 2 + gap)) / 2;

        drawCell(context, gridX, gridY, gridSize, pos == HudPosition.TOP_LEFT);
        drawCell(context, gridX + gridSize + gap, gridY, gridSize, pos == HudPosition.TOP_RIGHT);
        drawCell(context, gridX, gridY + gridSize + gap, gridSize, pos == HudPosition.BOTTOM_LEFT);
        drawCell(context, gridX + gridSize + gap, gridY + gridSize + gap, gridSize, pos == HudPosition.BOTTOM_RIGHT);

        // Abbreviation text to the right of the icon
        String abbrev = switch (pos) {
            case TOP_LEFT -> "TL";
            case TOP_RIGHT -> "TR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_RIGHT -> "BR";
        };
        int textX = gridX + gridSize * 2 + gap + 8;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, abbrev, textX, textY, GlassTheme.ACCENT);
    }

    private void drawCell(DrawContext context, int x, int y, int size, boolean active) {
        int fill = active ? GlassTheme.ACCENT : 0x33FFFFFF;
        context.fill(x, y, x + size, y + size, fill);
    }
}
