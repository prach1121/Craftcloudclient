package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.config.BannerPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A small widget showing a 3x2 grid icon with the active position
 * highlighted, plus a short abbreviation (TL/TC/TR/BL/BC/BR). Clicking
 * cycles to the next position. Sibling of {@link GlassCornerPicker}, but
 * for {@link BannerPosition} - which adds a center column the plain
 * 2x2 corner grid doesn't have - so it's only used on the Server Banner
 * row rather than the generic corner-panel rows.
 */
public class GlassBannerPositionPicker extends ButtonWidget {

    private final Supplier<BannerPosition> getter;

    public GlassBannerPositionPicker(int x, int y, int width, int height, Supplier<BannerPosition> getter, Consumer<BannerPosition> setter) {
        super(x, y, width, height, net.minecraft.text.Text.literal("Position"), button -> setter.accept(getter.get().next()), DEFAULT_NARRATION_SUPPLIER);
        this.getter = getter;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered);

        BannerPosition pos = getter.get();

        // Mini 3x2 grid icon on the left side of the widget
        int cellSize = 5;
        int gap = 2;
        int gridX = this.getX() + 8;
        int gridY = this.getY() + (this.getHeight() - (cellSize * 2 + gap)) / 2;

        drawCell(context, gridX, gridY, cellSize, pos == BannerPosition.TOP_LEFT);
        drawCell(context, gridX + cellSize + gap, gridY, cellSize, pos == BannerPosition.TOP_CENTER);
        drawCell(context, gridX + (cellSize + gap) * 2, gridY, cellSize, pos == BannerPosition.TOP_RIGHT);
        drawCell(context, gridX, gridY + cellSize + gap, cellSize, pos == BannerPosition.BOTTOM_LEFT);
        drawCell(context, gridX + cellSize + gap, gridY + cellSize + gap, cellSize, pos == BannerPosition.BOTTOM_CENTER);
        drawCell(context, gridX + (cellSize + gap) * 2, gridY + cellSize + gap, cellSize, pos == BannerPosition.BOTTOM_RIGHT);

        // Abbreviation text to the right of the icon
        String abbrev = switch (pos) {
            case TOP_LEFT -> "TL";
            case TOP_CENTER -> "TC";
            case TOP_RIGHT -> "TR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_CENTER -> "BC";
            case BOTTOM_RIGHT -> "BR";
        };
        int textX = gridX + (cellSize + gap) * 3 + 6;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, abbrev, textX, textY, GlassTheme.ACCENT);
    }

    private void drawCell(DrawContext context, int x, int y, int size, boolean active) {
        int fill = active ? GlassTheme.ACCENT : 0x33FFFFFF;
        context.fill(x, y, x + size, y + size, fill);
    }
}
