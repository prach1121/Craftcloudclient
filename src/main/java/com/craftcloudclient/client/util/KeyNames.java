package com.craftcloudclient.client.util;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Plain, always-English labels for {@code GLFW_KEY_*} codes.
 *
 * <p>{@link net.minecraft.client.option.KeyBinding#getBoundKeyLocalizedText()}
 * (and the {@code GLFW.glfwGetKeyName} call it ultimately goes through) asks
 * the OS to translate a physical key into whatever character *that key*
 * produces under the player's current keyboard layout - by design, so that
 * e.g. a French AZERTY player rebinding a key sees the letter they'd
 * actually type. On a non-Latin layout (Thai, Russian, Korean, ...) that
 * means an ordinary letter key like {@code G} or {@code L} renders as a
 * Thai/Cyrillic/Hangul glyph instead, which is what was happening on the
 * Keybinds tab here.
 *
 * <p>This mod's binds aren't meant to be typed as text - they're shortcuts
 * ("press L for Say GG") - so what's actually useful is a fixed, physical
 * key label that reads the same for every player no matter their layout.
 * This class supplies that directly from the GLFW key code instead of
 * asking the OS to localize it.
 */
public final class KeyNames {

    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        // Letters
        int[] letterKeys = {
                GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_E,
                GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_J,
                GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_O,
                GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_T,
                GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_Y,
                GLFW.GLFW_KEY_Z
        };
        for (int i = 0; i < letterKeys.length; i++) {
            NAMES.put(letterKeys[i], String.valueOf((char) ('A' + i)));
        }

        // Digit row
        int[] digitKeys = {
                GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4,
                GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9
        };
        for (int i = 0; i < digitKeys.length; i++) {
            NAMES.put(digitKeys[i], String.valueOf(i));
        }

        // Function row
        int[] fKeys = {
                GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4, GLFW.GLFW_KEY_F5,
                GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10,
                GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12, GLFW.GLFW_KEY_F13, GLFW.GLFW_KEY_F14, GLFW.GLFW_KEY_F15,
                GLFW.GLFW_KEY_F16, GLFW.GLFW_KEY_F17, GLFW.GLFW_KEY_F18, GLFW.GLFW_KEY_F19, GLFW.GLFW_KEY_F20,
                GLFW.GLFW_KEY_F21, GLFW.GLFW_KEY_F22, GLFW.GLFW_KEY_F23, GLFW.GLFW_KEY_F24, GLFW.GLFW_KEY_F25
        };
        for (int i = 0; i < fKeys.length; i++) {
            NAMES.put(fKeys[i], "F" + (i + 1));
        }

        // Numpad digits + operators
        int[] numKeys = {
                GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_4,
                GLFW.GLFW_KEY_KP_5, GLFW.GLFW_KEY_KP_6, GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_9
        };
        for (int i = 0; i < numKeys.length; i++) {
            NAMES.put(numKeys[i], "Num " + i);
        }
        NAMES.put(GLFW.GLFW_KEY_KP_DECIMAL, "Num .");
        NAMES.put(GLFW.GLFW_KEY_KP_DIVIDE, "Num /");
        NAMES.put(GLFW.GLFW_KEY_KP_MULTIPLY, "Num *");
        NAMES.put(GLFW.GLFW_KEY_KP_SUBTRACT, "Num -");
        NAMES.put(GLFW.GLFW_KEY_KP_ADD, "Num +");
        NAMES.put(GLFW.GLFW_KEY_KP_ENTER, "Num Enter");
        NAMES.put(GLFW.GLFW_KEY_KP_EQUAL, "Num =");

        // Everything else worth naming
        NAMES.put(GLFW.GLFW_KEY_SPACE, "Space");
        NAMES.put(GLFW.GLFW_KEY_APOSTROPHE, "'");
        NAMES.put(GLFW.GLFW_KEY_COMMA, ",");
        NAMES.put(GLFW.GLFW_KEY_MINUS, "-");
        NAMES.put(GLFW.GLFW_KEY_PERIOD, ".");
        NAMES.put(GLFW.GLFW_KEY_SLASH, "/");
        NAMES.put(GLFW.GLFW_KEY_SEMICOLON, ";");
        NAMES.put(GLFW.GLFW_KEY_EQUAL, "=");
        NAMES.put(GLFW.GLFW_KEY_LEFT_BRACKET, "[");
        NAMES.put(GLFW.GLFW_KEY_BACKSLASH, "\\");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_BRACKET, "]");
        NAMES.put(GLFW.GLFW_KEY_GRAVE_ACCENT, "`");
        NAMES.put(GLFW.GLFW_KEY_ESCAPE, "Escape");
        NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
        NAMES.put(GLFW.GLFW_KEY_TAB, "Tab");
        NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "Backspace");
        NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert");
        NAMES.put(GLFW.GLFW_KEY_DELETE, "Delete");
        NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
        NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
        NAMES.put(GLFW.GLFW_KEY_DOWN, "Down");
        NAMES.put(GLFW.GLFW_KEY_UP, "Up");
        NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "Page Up");
        NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "Page Down");
        NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
        NAMES.put(GLFW.GLFW_KEY_END, "End");
        NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps Lock");
        NAMES.put(GLFW.GLFW_KEY_SCROLL_LOCK, "Scroll Lock");
        NAMES.put(GLFW.GLFW_KEY_NUM_LOCK, "Num Lock");
        NAMES.put(GLFW.GLFW_KEY_PRINT_SCREEN, "Print Screen");
        NAMES.put(GLFW.GLFW_KEY_PAUSE, "Pause");
        NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "Left Shift");
        NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "Left Ctrl");
        NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "Left Alt");
        NAMES.put(GLFW.GLFW_KEY_LEFT_SUPER, "Left Super");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "Right Shift");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "Right Ctrl");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "Right Alt");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SUPER, "Right Super");
        NAMES.put(GLFW.GLFW_KEY_MENU, "Menu");
        NAMES.put(GLFW.GLFW_KEY_WORLD_1, "World 1");
        NAMES.put(GLFW.GLFW_KEY_WORLD_2, "World 2");
    }

    private KeyNames() {}

    /**
     * A fixed, layout-independent label for a keyboard scancode, or
     * {@code "Not Bound"} for {@link GLFW#GLFW_KEY_UNKNOWN}. Falls back to
     * {@code "Key " + keyCode} for anything not in the table above (should
     * only ever happen for exotic/vendor-specific keys).
     */
    public static String of(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "Not Bound";
        }
        return NAMES.getOrDefault(keyCode, "Key " + keyCode);
    }
}
