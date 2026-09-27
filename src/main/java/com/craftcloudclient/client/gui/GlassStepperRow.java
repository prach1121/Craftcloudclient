package com.craftcloudclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A single settings row for a clamped int value: a label on the left,
 * and a "‹ N ›" control on the right where clicking the left third of
 * that control decrements and the right third increments. There's no
 * separate press action on the widget itself (unlike
 * {@link GlassCornerPicker}'s single "click cycles" zone) since a plain
 * value needs both directions - see the {@link #mouseClicked} override.
 */
public class GlassStepperRow extends ButtonWidget {

    private static final int VALUE_BOX_WIDTH = 70;

    private final String label;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int min;
    private final int max;
    private final Runnable onChange;

    public GlassStepperRow(int x, int y, int width, int height, String label,
                            IntSupplier getter, IntConsumer setter, int min, int max, Runnable onChange) {
        // No-op press action: every click is handled directly in
        // mouseClicked below, since this widget needs two independent
        // zones (decrement/increment) rather than one whole-row action.
        super(x, y, width, height, net.minecraft.text.Text.literal(label), button -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (this.isHovered() && click.button() == 0) {
            int boxX = this.getX() + this.getWidth() - VALUE_BOX_WIDTH - 8;
            int zoneWidth = VALUE_BOX_WIDTH / 3;
            double localX = click.x() - boxX;

            if (localX >= 0 && localX < zoneWidth) {
                adjust(-1);
                return true;
            } else if (localX >= VALUE_BOX_WIDTH - zoneWidth && localX < VALUE_BOX_WIDTH) {
                adjust(1);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void adjust(int delta) {
        int current = getter.getAsInt();
        int next = Math.max(min, Math.min(max, current + delta));
        if (next != current) {
            setter.accept(next);
            if (onChange != null) onChange.run();
        }
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered);

        context.fill(this.getX(), this.getY() + 1, this.getX() + 3, this.getY() + this.getHeight() - 1, GlassTheme.ACCENT);

        var tr = MinecraftClient.getInstance().textRenderer;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(tr, label, this.getX() + 10, textY, GlassTheme.TEXT_MAIN);

        int value = getter.getAsInt();
        int boxX = this.getX() + this.getWidth() - VALUE_BOX_WIDTH - 8;
        int zoneWidth = VALUE_BOX_WIDTH / 3;

        boolean canDecrement = value > min;
        boolean canIncrement = value < max;
        int decColor = canDecrement ? GlassTheme.TEXT_MAIN : GlassTheme.TEXT_DIM;
        int incColor = canIncrement ? GlassTheme.TEXT_MAIN : GlassTheme.TEXT_DIM;

        context.drawTextWithShadow(tr, "-", boxX + zoneWidth / 2 - 2, textY, decColor);
        context.drawTextWithShadow(tr, "+", boxX + VALUE_BOX_WIDTH - zoneWidth / 2 - 3, textY, incColor);

        String valueText = Integer.toString(value);
        int valueWidth = tr.getWidth(valueText);
        int valueX = boxX + VALUE_BOX_WIDTH / 2 - valueWidth / 2;
        context.drawTextWithShadow(tr, valueText, valueX, textY, GlassTheme.ACCENT);
    }
}
