package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.util.KeyNames;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * A single "label + current key" settings row that lets the player rebind
 * a {@link KeyBinding} without ever leaving our own menu.
 *
 * Click the row, then press any key - the new binding is written straight
 * through {@link KeyBinding#setBoundKey} / {@link KeyBinding#updateKeysByCode()}
 * and immediately saved to options.txt, so vanilla's Options > Controls
 * screen and this menu are always in sync.
 *
 * Note: this deliberately does NOT override ButtonWidget's onPress(...) -
 * its signature changed between Minecraft versions (it now takes an
 * AbstractInput in 1.21.11+) and isn't worth chasing. Instead the normal
 * PressAction passed to the super constructor flips a boolean held in a
 * one-element array, which sidesteps the "can't reference this before
 * super()" restriction entirely.
 */
public class GlassKeybindRow extends ButtonWidget {

    private final String label;
    private final KeyBinding binding;
    private final boolean[] listeningHolder;

    public GlassKeybindRow(int x, int y, int width, int height, String label, KeyBinding binding) {
        this(x, y, width, height, label, binding, new boolean[]{false});
    }

    private GlassKeybindRow(int x, int y, int width, int height, String label, KeyBinding binding, boolean[] listeningHolder) {
        super(x, y, width, height, net.minecraft.text.Text.literal(label), b -> {
            listeningHolder[0] = true;
        }, DEFAULT_NARRATION_SUPPLIER);
        this.label = label;
        this.binding = binding;
        this.listeningHolder = listeningHolder;
    }

    public boolean isListening() {
        return listeningHolder[0];
    }

    /**
     * Called by the owning screen when a key is pressed while this row is
     * in "listening" state.
     */
    public void applyKey(int keyCode) {
        listeningHolder[0] = false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return; // Escape cancels the rebind instead of binding to Escape itself.
        }
        binding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
    }

    /**
     * Right-click unbinds this key entirely (sets it to GLFW_KEY_UNKNOWN,
     * the same "not bound" sentinel vanilla's own Controls screen uses),
     * without opening the "press a key" listening state at all.
     */
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (click.button() == 1 && this.isHovered()) {
            listeningHolder[0] = false;
            binding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_UNKNOWN));
            KeyBinding.updateKeysByCode();
            MinecraftClient.getInstance().options.write();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean listening = listeningHolder[0];
        boolean hovered = this.isHovered();
        GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered || listening);

        boolean customized = !binding.isDefault();
        int accentColor = listening ? GlassTheme.ACCENT : (customized ? GlassTheme.ACCENT_SECONDARY : 0x33FFFFFF);
        context.fill(this.getX(), this.getY() + 1, this.getX() + 3, this.getY() + this.getHeight() - 1, accentColor);

        var tr = MinecraftClient.getInstance().textRenderer;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        context.drawTextWithShadow(tr, label, this.getX() + 10, textY, GlassTheme.TEXT_MAIN);

        String keyLabel;
        if (listening) {
            keyLabel = "> Press a Key <";
        } else {
            InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
            // KEYSYM (regular keyboard keys) get our own fixed, English
            // label - see KeyNames for why: the vanilla localized text
            // renders in whatever character the player's OS keyboard
            // layout maps that physical key to (e.g. Thai, Cyrillic),
            // which reads as garbled/wrong-language text for a simple
            // shortcut label. Mouse buttons have no such layout, so those
            // still use vanilla's own localized text.
            keyLabel = key.getCategory() == InputUtil.Type.KEYSYM
                    ? KeyNames.of(key.getCode())
                    : binding.getBoundKeyLocalizedText().getString();
        }
        int keyColor = listening ? GlassTheme.ACCENT : (customized ? GlassTheme.ACCENT_SECONDARY : GlassTheme.TEXT_DIM);
        int keyWidth = tr.getWidth(keyLabel);
        int keyX = this.getX() + this.getWidth() - keyWidth - 10;
        context.drawTextWithShadow(tr, keyLabel, keyX, textY, keyColor);
    }
}
