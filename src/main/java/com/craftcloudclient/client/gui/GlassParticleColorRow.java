package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.config.AttackParticleType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A single settings row: a label on the left, a small color swatch plus
 * the current option's name on the right. Clicking cycles to the next
 * {@link AttackParticleType}, the same "click cycles" pattern
 * {@link GlassCornerPicker} uses for HUD corners.
 */
public class GlassParticleColorRow extends ButtonWidget {

    private final String label;
    private final Supplier<AttackParticleType> getter;

    public GlassParticleColorRow(int x, int y, int width, int height, String label,
                                  Supplier<AttackParticleType> getter, Consumer<AttackParticleType> setter) {
        super(x, y, width, height, net.minecraft.text.Text.literal(label),
                button -> setter.accept(getter.get().next()), DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.getter = getter;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered);

        AttackParticleType type = getter.get();
        int accentColor = 0xFF000000 | type.getRgb();

        // Left accent bar tinted to the currently selected color.
        context.fill(this.getX(), this.getY() + 1, this.getX() + 3, this.getY() + this.getHeight() - 1, accentColor);

        var tr = MinecraftClient.getInstance().textRenderer;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(tr, label, this.getX() + 10, textY, GlassTheme.TEXT_MAIN);

        String valueLabel = type.getDisplayName();
        int dotSize = 8;
        int valueWidth = tr.getWidth(valueLabel);
        int dotX = this.getX() + this.getWidth() - valueWidth - dotSize - 16;
        int dotY = this.getY() + (this.getHeight() - dotSize) / 2;
        GlassTheme.dot(context, dotX, dotY, dotSize, accentColor);
        context.drawTextWithShadow(tr, valueLabel, dotX + dotSize + 6, textY, GlassTheme.ACCENT);
    }
}
