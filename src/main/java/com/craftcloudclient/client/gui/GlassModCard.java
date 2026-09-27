package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.config.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The top face of a "mod card" tile - a compact square-ish button showing a
 * small letter icon, the element's name, and (when it has an on-screen
 * position that can be cycled) an "OPTIONS" caption underneath, mirroring
 * the card grid used by other polished third-party Minecraft clients: a
 * small icon glyph up top, the name in the middle, "OPTIONS" as a subtle
 * link-styled caption, and a separate full-width enabled/disabled pill
 * (see {@link GlassEnabledPill}) stacked directly below it to make one
 * complete card.
 *
 * <p>When {@code posGetter}/{@code posSetter} are null (a toggle with no
 * on-screen position to place, e.g. "Low Health Warning") the card still
 * renders its icon and title but skips the OPTIONS caption and the click
 * becomes a no-op - it's here purely so the grid keeps a uniform card shape
 * even for settings that have nothing to configure beyond on/off.</p>
 */
public class GlassModCard extends ButtonWidget {

    private final String label;
    private final String glyph;
    private final int accent;
    private final Supplier<HudPosition> posGetter;

    public GlassModCard(int x, int y, int width, int height, String label, int accent,
                         Supplier<HudPosition> posGetter, Consumer<HudPosition> posSetter) {
        super(x, y, width, height, net.minecraft.text.Text.literal(label),
                button -> {
                    if (posGetter != null && posSetter != null) {
                        posSetter.accept(posGetter.get().next());
                    }
                }, DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.glyph = glyphFor(label);
        this.accent = accent;
        this.posGetter = posGetter;
    }

    /** First letter of the first two words (or the first two letters of a single word), e.g. "Kill Streak" -> "KS", "FPS" -> "FP". */
    private static String glyphFor(String label) {
        String[] words = label.trim().split("\\s+");
        if (words.length >= 2 && !words[0].isEmpty() && !words[1].isEmpty()) {
            return ("" + Character.toUpperCase(words[0].charAt(0)) + Character.toUpperCase(words[1].charAt(0)));
        }
        String w = words.length > 0 ? words[0] : label;
        if (w.length() >= 2) {
            return w.substring(0, 2).toUpperCase(java.util.Locale.ROOT);
        }
        return w.toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();

        GlassTheme.card(context, x, y, w, h, hovered);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        // Real per-line height (9px: 8px glyph + 1px shadow) rather than
        // the "8" shorthand used elsewhere in this menu for centering a
        // single line - this widget stacks three lines, so using the
        // true line height here is what keeps them from visually
        // running into each other.
        int lineHeight = tr.fontHeight;

        // Small square icon with the glyph, sitting in the top third of the card.
        int iconSize = 18;
        int iconX = x + (w - iconSize) / 2;
        int iconY = y + 6;
        int iconRadius = 5;
        int iconFill = hovered ? GlassTheme.blend(accent, 0xFFFFFFFF, 0.12f) : accent;
        GlassTheme.strokeRounded(context, iconX, iconY, iconSize, iconSize,
                (iconFill & 0x00FFFFFF) | 0x33000000, (accent & 0x00FFFFFF) | 0x70000000, iconRadius);
        GlassTheme.roundedTopSheen(context, iconX, iconY, iconSize, iconSize / 2, iconRadius, 0x30FFFFFF, 0x00FFFFFF, true, true);
        int glyphWidth = tr.getWidth(glyph);
        context.drawTextWithShadow(tr, glyph, iconX + (iconSize - glyphWidth) / 2, iconY + (iconSize - 8) / 2, accent);

        // Title, on its own clear line below the icon (title's baseline
        // sits a full line-height plus a few px of breathing room past
        // the icon, so descenders/shadow never touch the icon).
        int titleY = iconY + iconSize + 6;
        String title = fit(tr, label, w - 8);
        int titleWidth = tr.getWidth(title);
        context.drawTextWithShadow(tr, title, x + (w - titleWidth) / 2, titleY, GlassTheme.TEXT_MAIN);

        // "OPTIONS" caption, one full line-height below the title (plus a
        // couple px of margin) so it never overlaps the title above it -
        // drawn without a shadow and in a dimmer color so it reads as a
        // quiet caption rather than a second, equally heavy line of text.
        if (posGetter != null) {
            String caption = "OPTIONS  " + abbrev(posGetter.get());
            int capY = titleY + lineHeight + 3;
            int capWidth = tr.getWidth(caption);
            int capColor = hovered ? accent : GlassTheme.TEXT_DIM;
            context.drawText(tr, caption, x + (w - capWidth) / 2, capY, capColor, false);
        }
    }

    private static String abbrev(HudPosition pos) {
        return switch (pos) {
            case TOP_LEFT -> "TL";
            case TOP_RIGHT -> "TR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_RIGHT -> "BR";
        };
    }

    private static String fit(TextRenderer tr, String text, int maxWidth) {
        if (tr.getWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        String out = text;
        while (!out.isEmpty() && tr.getWidth(out + ellipsis) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + ellipsis;
    }
}
