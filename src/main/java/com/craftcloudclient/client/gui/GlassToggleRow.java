package com.craftcloudclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A single settings row: a label on the left and a real toggle-switch pill
 * on the right (instead of a plain "ON"/"OFF" text button).
 *
 * Built on top of {@link ButtonWidget} rather than {@link net.minecraft.client.gui.widget.ClickableWidget}
 * directly, so click handling (which Minecraft has changed the internals of
 * across versions) stays whatever vanilla/Yarn currently does - we only
 * override rendering.
 */
public class GlassToggleRow extends ButtonWidget {

    private final String label;
    private final BooleanSupplier getter;

    public GlassToggleRow(int x, int y, int width, int height, String label,
                           BooleanSupplier getter, Consumer<Boolean> setter, Runnable onChange) {
        super(x, y, width, height, net.minecraft.text.Text.literal(label), button -> {
            boolean newValue = !getter.getAsBoolean();
            setter.accept(newValue);
            if (onChange != null) onChange.run();
        }, DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.getter = getter;
    }

    public GlassToggleRow(int x, int y, int width, int height, String label,
                          BooleanSupplier getter, Consumer<Boolean> setter) {
        this(x, y, width, height, label, getter, setter, null);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean on = getter.getAsBoolean();
        boolean hovered = this.isHovered();

        GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered);

        // Left accent bar reflecting state
        int accentColor = on ? GlassTheme.ON_COLOR : GlassTheme.OFF_COLOR;
        context.fill(this.getX(), this.getY() + 1, this.getX() + 3, this.getY() + this.getHeight() - 1, accentColor);

        // Label
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label,
                this.getX() + 10, textY, GlassTheme.TEXT_MAIN);

        // Toggle pill (right-aligned) - a true rounded capsule: solid
        // green track when on, solid red when off, with a round knob
        // that slides to whichever side is active.
        int pillWidth = 32;
        int pillHeight = 14;
        int pillX = this.getX() + this.getWidth() - pillWidth - 8;
        int pillY = this.getY() + (this.getHeight() - pillHeight) / 2;
        int pillRadius = pillHeight / 2;

        int trackColor = on ? GlassTheme.ON_COLOR : GlassTheme.OFF_COLOR;
        GlassTheme.fillRounded(context, pillX, pillY, pillWidth, pillHeight, trackColor, pillRadius);
        GlassTheme.roundedTopSheen(context, pillX, pillY, pillWidth, pillHeight / 2, pillRadius, 0x2AFFFFFF, 0x00FFFFFF, true, true);

        int knobSize = pillHeight - 4;
        int knobX = on ? (pillX + pillWidth - knobSize - 2) : (pillX + 2);
        int knobY = pillY + 2;
        GlassTheme.fillRounded(context, knobX, knobY, knobSize, knobSize, 0xFFF5F7FA, knobSize / 2);
    }
}
