package com.craftcloudclient.client;

import com.craftcloudclient.client.chat.ChatTimestamps;
import com.craftcloudclient.client.chat.GgSender;
import com.craftcloudclient.client.combat.AttackParticleModule;
import com.craftcloudclient.client.combat.CombatManager;
import com.craftcloudclient.client.combat.HitMarkerModule;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.gui.ConfigScreen;
import com.craftcloudclient.client.gui.GlassTheme;
import com.craftcloudclient.client.gui.HudLayoutScreen;
import com.craftcloudclient.client.gui.ThemePreset;
import com.craftcloudclient.client.health.HungerWarningManager;
import com.craftcloudclient.client.health.LowHealthWarningManager;
import com.craftcloudclient.client.hud.HudManager;
import com.craftcloudclient.client.items.DurabilityWarningManager;
import com.craftcloudclient.client.movement.AutoSprintManager;
import com.craftcloudclient.client.performance.PerformanceManager;
import com.craftcloudclient.client.presence.DiscordPresenceManager;
import com.craftcloudclient.client.stats.StatsListener;
import com.craftcloudclient.client.stats.StatsManager;
import com.craftcloudclient.client.update.UpdateChecker;
import com.craftcloudclient.client.visual.FullBrightManager;
import com.craftcloudclient.client.visual.ZoomManager;
import com.craftcloudclient.client.waypoint.DeathTracker;
import com.craftcloudclient.client.waypoint.DimensionUtil;
import com.craftcloudclient.client.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;

public class CraftcloudClient implements ClientModInitializer {

    public static final String MOD_ID = "craftcloudclient";

    private static HudManager hudManager;
    private static final HitMarkerModule hitMarkerModule = new HitMarkerModule();
    private static final AttackParticleModule attackParticleModule = new AttackParticleModule();

    public static HudManager getHudManager() {
        return hudManager;
    }

    @Override
    public void onInitializeClient() {
        ModConfig.INSTANCE = ModConfig.load();
        ThemePreset.fromIndex(ModConfig.INSTANCE.themePreset).apply();
        GlassTheme.legacyCorners = ModConfig.INSTANCE.oldCornerDesign;

        // Fire-and-forget: checks Modrinth for a newer release on a
        // background thread. Result (if any) is polled by ConfigScreen's
        // About tab/sidebar badge and the pause-menu banner - nothing to
        // wait on here.
        UpdateChecker.checkAsync();

        hudManager = new HudManager();

        // Discord Rich Presence - entirely self-contained: it only ever
        // talks to the local Discord client over IPC on its own background
        // thread, and every failure mode (Discord not running, pipe closed
        // mid-session, etc.) is swallowed inside DiscordPresenceManager so
        // it can never affect gameplay or crash the client. Gated behind
        // the Security tab's "Discord Status (RPC)" toggle, on by default.
        if (ModConfig.INSTANCE.discordRpcEnabled) {
            DiscordPresenceManager.start();
        }

        // Registers every KeyBinding up front. This also makes them show up
        // (and be rebindable) in vanilla's Options > Controls screen, in
        // addition to our own Keybinds tab in ConfigScreen.
        KeyBindings.init();

        // Hits / kills are event-driven (see StatsListener); placements are
        // polled per-tick below alongside hudManager.tick().
        StatsListener.register();

        // Hit-confirmation crosshair pulse + click sound (Settings > PvP).
        hitMarkerModule.register();

        // Colored spark burst on hit, styled after firework sparks
        // (Settings > PvP: on/off, color, particles per attack 3-18).
        attackParticleModule.register();

        // Purely client-side "[HH:mm]" prefix on incoming chat lines.
        ChatTimestamps.register();

        // "Dealt damage" half of the in-combat timer; the "took damage"
        // half is polled per-tick below alongside everything else.
        CombatManager.register();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (KeyBindings.OPEN_MENU.wasPressed()) {
                client.setScreen(new ConfigScreen());
            }

            while (KeyBindings.EDIT_HUD_LAYOUT.wasPressed()) {
                client.setScreen(new HudLayoutScreen());
            }

            ModConfig cfg = ModConfig.INSTANCE;

            while (KeyBindings.TOGGLE_FPS_BOOST.wasPressed()) {
                cfg.fpsBoostEnabled = !cfg.fpsBoostEnabled;
                PerformanceManager.apply();
                cfg.save();
            }

            while (KeyBindings.TOGGLE_PVP_MODE.wasPressed()) {
                // One hotkey flips both particle-hiding options together,
                // treating them as a single "PvP mode" preset.
                boolean enabling = !(cfg.hideCrystalParticles && cfg.hideTotemParticles);
                cfg.hideCrystalParticles = enabling;
                cfg.hideTotemParticles = enabling;
                cfg.save();
            }

            while (KeyBindings.TOGGLE_HUD.wasPressed()) {
                cfg.hudModulesEnabled = !cfg.hudModulesEnabled;
                cfg.save();
            }

            // GgSender enforces its own cooldown internally, so a held key
            // or a fast double-tap can call trigger() repeatedly here
            // without actually spamming chat.
            while (KeyBindings.SAY_GG.wasPressed()) {
                GgSender.trigger();
            }

            while (KeyBindings.ADD_WAYPOINT.wasPressed()) {
                addWaypointHere(client);
            }

            while (KeyBindings.COPY_COORDS.wasPressed()) {
                copyCoordsHere(client);
            }

            while (KeyBindings.TOGGLE_FULL_BRIGHT.wasPressed()) {
                cfg.fullBrightEnabled = !cfg.fullBrightEnabled;
                FullBrightManager.apply(client, cfg.fullBrightEnabled);
                cfg.save();
            }

            while (KeyBindings.TOGGLE_AUTO_SPRINT.wasPressed()) {
                cfg.autoSprintEnabled = !cfg.autoSprintEnabled;
                cfg.save();
            }

            // No live-apply call needed here - WorldWeatherMixin reads
            // cfg.disableRain directly every time the game asks for the
            // rain/thunder gradient, so flipping the flag takes effect
            // on the very next frame with nothing to invalidate/restore.
            while (KeyBindings.TOGGLE_RAIN.wasPressed()) {
                cfg.disableRain = !cfg.disableRain;
                cfg.save();
            }

            // No live-apply call needed here either - PlayerRenderMixin reads
            // cfg.hidePlayers directly every time a player entity is about
            // to be rendered, so flipping the flag takes effect on the
            // very next frame with nothing to invalidate/restore.
            while (KeyBindings.TOGGLE_HIDE_PLAYERS.wasPressed()) {
                cfg.hidePlayers = !cfg.hidePlayers;
                cfg.save();
            }

            // Held, not pressed - checks every tick so it turns off the
            // instant the key is released rather than needing a second tap.
            ZoomManager.tick(client);

            // Also polled every tick - see AutoSprintManager for the exact
            // (vanilla-parity) conditions it checks before sprinting.
            AutoSprintManager.tick(client);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            hudManager.tick();
            hitMarkerModule.tick();
            StatsListener.tick();
            StatsManager.INSTANCE.tick();
            DeathTracker.tick();
            DurabilityWarningManager.tick(client);
            LowHealthWarningManager.tick(client);
            HungerWarningManager.tick(client);
            CombatManager.tick(client);

            // Once a second is plenty - the presence thread only republishes
            // on its own slower interval anyway (see DiscordPresenceManager).
            if (ModConfig.INSTANCE.discordRpcEnabled
                    && (client.world == null || client.player == null || client.player.age % 20 == 0)) {
                DiscordPresenceManager.updateState(client);
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            hudManager.onFrame();
            hudManager.render(context);
            hitMarkerModule.render(context);
        });

        // Apply saved performance settings once the client has fully finished
        // starting up - MinecraftClient.options is not yet assigned while
        // onInitializeClient() runs (it's invoked from inside the
        // MinecraftClient constructor itself), so calling this synchronously
        // here throws a NullPointerException.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            PerformanceManager.apply();
            FullBrightManager.applyFromConfig(client);
        });

        // Safety net: if the player disconnects (or crashes to the title
        // screen) mid-zoom, make sure the FOV override can't get stuck.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ZoomManager.reset(client));

        // Stats are only flushed to disk every few seconds while playing
        // (see StatsManager.tick()) - make sure whatever happened in the
        // last few seconds before the game closes isn't lost.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            StatsManager.INSTANCE.save();
            WaypointManager.INSTANCE.save();
            DiscordPresenceManager.stop();
        });
    }

    /** Adds a waypoint named "Waypoint N" at the player's current position, then confirms it locally via the action bar. */
    private static void addWaypointHere(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) return;

        String name = "Waypoint " + (WaypointManager.INSTANCE.userWaypointCount() + 1);
        WaypointManager.INSTANCE.addWaypoint(
                name,
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                DimensionUtil.currentDimensionKey(client)
        );

        client.player.sendMessage(Text.literal("Waypoint added: " + name), true);
    }

    /** Copies the player's current block position as "X, Y, Z" to the system clipboard, then confirms locally via the action bar. */
    private static void copyCoordsHere(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) return;

        String coords = String.format(
                "%d, %d, %d",
                (int) Math.floor(client.player.getX()),
                (int) Math.floor(client.player.getY()),
                (int) Math.floor(client.player.getZ())
        );

        client.keyboard.setClipboard(coords);
        client.player.sendMessage(Text.literal("Copied coordinates: " + coords), true);
    }
}
