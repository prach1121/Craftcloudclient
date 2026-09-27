package com.craftcloudclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Glass-styled search box used at the top of every config tab: a flat
 * bar (same visual language as {@link GlassButton}/{@link GlassToggleRow}
 * - solid fill, faint top gradient, 1px border, no vanilla white text-box
 * chrome) with a small hand-drawn magnifying-glass icon on the left and
 * a "Search Feature" placeholder.
 *
 * The icon lives in a reserved {@link #ICON_AREA}-pixel strip to this
 * widget's left. Callers construct it with {@code x} already shifted
 * right by that amount (see {@code ConfigScreen#searchBarRow}) so the
 * field's own text/cursor never overlaps the icon; this widget then
 * paints the *combined* bar background (icon strip + text area) as one
 * piece behind both, and lets the vanilla text-field logic render the
 * text/cursor/selection on top, unaffected.
 */
public class GlassSearchField extends TextFieldWidget {

    /** Width reserved to the left of the text area for the magnifying-glass icon. */
    public static final int ICON_AREA = 20;

    private final int barX;
    private final int barY;
    private final int barWidth;
    private final int barHeight;

    public GlassSearchField(int x, int y, int width, int height) {
        // Vanilla TextFieldWidget only insets/centers its text (getX()+4,
        // getY()+(height-8)/2) when drawsBackground is true; with it false
        // (our case - we paint the bar ourselves below) it draws the text
        // at exactly (getX(), getY()) with no centering at all. Handing
        // the full bar height straight to super() used to glue the text
        // to the bar's top-left corner instead of centering it, making it
        // render above the visible border. So the underlying field gets
        // its own small, vertically-centered bounds just for the text/
        // cursor, and the full-size decorative bar is drawn separately
        // from the barX/barY/barWidth/barHeight fields below.
        super(MinecraftClient.getInstance().textRenderer,
                x, y + (height - 8) / 2, width, 8, null, Text.literal("Search"));
        this.barX = x - ICON_AREA;
        this.barY = y;
        this.barWidth = width + ICON_AREA;
        this.barHeight = height;
        this.setDrawsBackground(false);
        this.setPlaceholder(Text.literal("Search Feature"));
        this.setMaxLength(48);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // The field's own bounds are now just the thin centered text
        // strip (see constructor); clicks anywhere on the visible bar -
        // including the icon strip - should still focus it.
        return this.isVisible() && mouseX >= barX && mouseY >= barY
                && mouseX < barX + barWidth && mouseY < barY + barHeight;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        boolean focused = this.isFocused();

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA173A47);
        context.fillGradient(barX, barY, barX + barWidth, barY + barHeight / 2, 0x1EFFFFFF, 0x00FFFFFF);

        int borderColor = focused ? GlassTheme.ACCENT : GlassTheme.PANEL_BORDER;
        context.fill(barX, barY, barX + barWidth, barY + 1, borderColor);
        context.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, borderColor);
        context.fill(barX, barY, barX + 1, barY + barHeight, borderColor);
        context.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, borderColor);

        drawMagnifyingGlass(context, barX + ICON_AREA / 2 - 3, barY + barHeight / 2 - 3,
                focused ? GlassTheme.ACCENT : GlassTheme.TEXT_DIM);

        super.renderWidget(context, mouseX, mouseY, deltaTicks);
    }

    /**
     * Draws a tiny magnifying glass purely out of {@code fill()} rects -
     * a hollow 5x5 ring for the lens plus a 2px stepped diagonal for the
     * handle - so it needs no texture asset of its own.
     */
    private void drawMagnifyingGlass(DrawContext context, int x, int y, int color) {
        // Hollow ring (5x5 outer square, 3x3 hollow center)
        context.fill(x, y, x + 5, y + 1, color);
        context.fill(x, y + 4, x + 5, y + 5, color);
        context.fill(x, y, x + 1, y + 5, color);
        context.fill(x + 4, y, x + 5, y + 5, color);
        // Handle, stepping down-right from the ring's bottom-right corner
        context.fill(x + 5, y + 5, x + 7, y + 6, color);
        context.fill(x + 6, y + 6, x + 8, y + 7, color);
    }
}
