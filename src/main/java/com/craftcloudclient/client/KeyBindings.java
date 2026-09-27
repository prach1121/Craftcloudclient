package com.craftcloudclient.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Central registry for every keybind this client adds.
 *
 * Both the mod entrypoint (which polls wasPressed() every tick) and the
 * in-game menu (which lists and rebinds them) share these exact
 * KeyBinding instances, so there's only ever one source of truth for
 * "what key does this do" - the vanilla Options > Controls screen and
 * our own Keybinds tab always agree.
 */
public final class KeyBindings {

    public static final KeyBinding OPEN_MENU;
    public static final KeyBinding TOGGLE_FPS_BOOST;
    public static final KeyBinding TOGGLE_PVP_MODE;
    public static final KeyBinding TOGGLE_HUD;
    public static final KeyBinding EDIT_HUD_LAYOUT;
    public static final KeyBinding SAY_GG;
    public static final KeyBinding ADD_WAYPOINT;
    public static final KeyBinding COPY_COORDS;
    public static final KeyBinding ZOOM;
    public static final KeyBinding TOGGLE_FULL_BRIGHT;
    public static final KeyBinding TOGGLE_AUTO_SPRINT;
    public static final KeyBinding TOGGLE_RAIN;
    public static final KeyBinding TOGGLE_HIDE_PLAYERS;

    static {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(CraftcloudClient.MOD_ID, "general"));

        // Bound by default so the menu is always reachable out of the box.
        OPEN_MENU = register("openmenu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);

        // Unbound by default (GLFW_KEY_UNKNOWN) - same convention vanilla
        // uses for optional hotkeys - so players opt into these themselves,
        // either from Options > Controls or from our Keybinds tab.
        TOGGLE_FPS_BOOST = register("togglefpsboost", GLFW.GLFW_KEY_UNKNOWN, category);
        TOGGLE_PVP_MODE = register("togglepvpmode", GLFW.GLFW_KEY_UNKNOWN, category);
        TOGGLE_HUD = register("togglehud", GLFW.GLFW_KEY_UNKNOWN, category);
        EDIT_HUD_LAYOUT = register("edithudlayout", GLFW.GLFW_KEY_UNKNOWN, category);
        SAY_GG = register("saygg", GLFW.GLFW_KEY_UNKNOWN, category);
        ADD_WAYPOINT = register("addwaypoint", GLFW.GLFW_KEY_UNKNOWN, category);
        COPY_COORDS = register("copycoords", GLFW.GLFW_KEY_UNKNOWN, category);

        // Bound to C by default, matching the zoom hotkey most other
        // clients/resource packs already use, so it works out of the box.
        ZOOM = register("zoom", GLFW.GLFW_KEY_C, category);
        TOGGLE_FULL_BRIGHT = register("togglefullbright", GLFW.GLFW_KEY_UNKNOWN, category);
        TOGGLE_AUTO_SPRINT = register("toggleautosprint", GLFW.GLFW_KEY_UNKNOWN, category);
        TOGGLE_RAIN = register("togglerain", GLFW.GLFW_KEY_UNKNOWN, category);
        TOGGLE_HIDE_PLAYERS = register("togglehideplayers", GLFW.GLFW_KEY_UNKNOWN, category);
    }

    private KeyBindings() {}

    private static KeyBinding register(String id, int defaultKey, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.craftcloudclient." + id,
                InputUtil.Type.KEYSYM,
                defaultKey,
                category
        ));
    }

    /** No-op call that just forces this class's static initializer to run during mod init. */
    public static void init() {}
}
