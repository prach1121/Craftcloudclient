package com.craftcloudclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-backed configuration for the client.
 * Stored at .minecraft/config/craftcloudclient.json
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("craftcloudclient.json");

    public static ModConfig INSTANCE = load();

    // --- HUD toggles ---
    public boolean showFps = true;
    public boolean showCoords = true;
    public boolean showTps = true;
    public boolean showPing = true;
    public boolean showCps = true;
    public boolean showArmorStatus = true;
    public boolean showTotemCounter = true;
    public boolean showStatsHud = true;
    // Off by default - a graph takes noticeably more screen space than a
    // single text line, so these only appear once the player opts in.
    public boolean showPingGraph = false;
    public boolean showStatusGraph = false;
    public boolean showKillStreak = true;
    // Off by default for the same reason as the graphs above - the WASD +
    // mouse grid takes more space than a text line, so it's an opt-in.
    public boolean showKeystrokes = false;
    // Countdown to the next day/night transition ("Night in: 4:12" /
    // "Day in: 2:03") - see TimeUntilDayModule.
    public boolean showTimeUntilDay = true;
    // Top-of-screen "nearest waypoint" / "latest death" tracker cards.
    // Separate from the corner-anchored modules above since this HUD is
    // always centered near the top rather than user-positionable.
    public boolean showWaypointHud = true;
    // Compass-style bar showing which way to turn to face the nearest
    // waypoint, plus its distance - separate toggle from showWaypointHud
    // above since one is a name/distance card and this is a directional
    // aid; players can run either, both, or neither.
    public boolean showWaypointNavigator = true;
    // Hides vanilla's own boss bar (the bar(s) that appear top-center for
    // bosses like the Ender Dragon/Wither, or any server-sent boss bar UI).
    // Purely a rendering toggle via BossBarHudMixin - it does not affect
    // boss health, fog/sky darkening, or dragon music, which are still
    // driven by the real (hidden) boss bar data.
    public boolean hideBossBar = false;
    // Session timer (resets on world join/rejoin, see PlaytimeModule).
    public boolean showPlaytime = true;
    // Block-light reading at the player's feet - the same number that
    // governs hostile mob spawning, surfaced as a quick HUD readout for
    // base-proofing instead of digging through F3.
    public boolean showLightLevel = true;
    // Real-world (system clock) time readout - not in-game day/night time,
    // which vanilla already shows. See ClockModule.
    public boolean showClock = true;
    // 24-hour "14:32" vs 12-hour "2:32 PM" formatting for the clock above.
    public boolean clockUse24Hour = true;
    // Current biome name, resolved the same way vanilla's F3 screen does.
    public boolean showBiome = true;
    // Count of hostile mobs within a fixed radius - see HostileMobModule.
    public boolean showHostileMobs = true;
    // Distance to the nearest world border edge - off by default since most
    // worlds never have a border close enough to matter.
    public boolean showWorldBorder = false;
    // Session odometer (resets on world join/rejoin, see DistanceTraveledModule).
    public boolean showDistanceTraveled = true;
    // Session XP gained (resets on world join/rejoin, see XpGainedModule).
    public boolean showXpGained = false;
    // "In Combat: Xs" countdown, only visible while actually in combat -
    // see CombatManager/CombatTimerModule.
    public boolean showCombatTimer = true;

    // --- HUD positions ---
    public HudPosition fpsPosition = HudPosition.TOP_LEFT;
    public HudPosition coordsPosition = HudPosition.TOP_LEFT;
    public HudPosition tpsPosition = HudPosition.TOP_LEFT;
    public HudPosition pingPosition = HudPosition.TOP_LEFT;
    public HudPosition cpsPosition = HudPosition.TOP_LEFT;
    public HudPosition armorStatusPosition = HudPosition.TOP_RIGHT;
    public HudPosition statsPosition = HudPosition.BOTTOM_RIGHT;
    public HudPosition pingGraphPosition = HudPosition.TOP_RIGHT;
    public HudPosition statusGraphPosition = HudPosition.TOP_RIGHT;
    public HudPosition killStreakPosition = HudPosition.BOTTOM_RIGHT;
    public HudPosition keystrokesPosition = HudPosition.BOTTOM_LEFT;
    public HudPosition timeUntilDayPosition = HudPosition.TOP_LEFT;
    public HudPosition playtimePosition = HudPosition.TOP_LEFT;
    public HudPosition lightLevelPosition = HudPosition.TOP_LEFT;
    public HudPosition clockPosition = HudPosition.TOP_RIGHT;
    public HudPosition biomePosition = HudPosition.TOP_LEFT;
    public HudPosition hostileMobsPosition = HudPosition.BOTTOM_RIGHT;
    public HudPosition worldBorderPosition = HudPosition.TOP_RIGHT;
    public HudPosition distanceTraveledPosition = HudPosition.BOTTOM_LEFT;
    public HudPosition xpGainedPosition = HudPosition.BOTTOM_LEFT;
    public HudPosition combatTimerPosition = HudPosition.BOTTOM_LEFT;
    // Free-form panel positions for the HUD layout editor. A value of -1 means
    // "use the default corner-based fallback" until the player has moved it.
    public int topLeftPanelX = -1;
    public int topLeftPanelY = -1;
    public int topRightPanelX = -1;
    public int topRightPanelY = -1;
    public int bottomLeftPanelX = -1;
    public int bottomLeftPanelY = -1;
    public int bottomRightPanelX = -1;
    public int bottomRightPanelY = -1;
    // Totem Counter is always anchored just above the hotbar (like the
    // vanilla offhand slot), so it intentionally has no HudPosition field.

    // Free-form pixel positions for the server banner and waypoint tracker
    // cards, settable by dragging them in the HUD layout editor. These two
    // aren't corner-anchored like the modules above - they default to
    // centered near the top of the screen - so they get their own simple
    // x/y pair instead of a HudPosition. -1 means "not moved yet - use the
    // default centered position".
    public int serverBannerX = -1;
    public int serverBannerY = -1;
    // Quick position choice for the server banner, offered as a picker in
    // the settings screen next to its toggle. Uses BannerPosition rather
    // than the generic HudPosition since the banner is a single centered
    // pill (not a corner panel) and needs a "center" option. Only used to
    // compute the *default* position (see boundsFor in ServerBannerModule)
    // - it's ignored once the player has dragged the banner to a custom
    // spot via the HUD layout editor, at which point serverBannerX/Y take
    // over. Picking a position from the settings screen resets
    // serverBannerX/Y back to -1 so the new position applies. Defaults to
    // top-center, matching its "centered near the top" design.
    public BannerPosition serverBannerPosition = BannerPosition.TOP_CENTER;
    public int waypointHudX = -1;
    public int waypointHudY = -1;
    public int waypointNavigatorX = -1;
    public int waypointNavigatorY = -1;

    // --- Visual ---
    // Master "smooth UI" switch: when on, every HUD panel/card (and the
    // server banner below) is drawn with the translucent glass look via
    // GlassTheme; when off they fall back to a flat, cheaper fill.
    public boolean glassBackground = true;
    public float hudScale = 1.0f;
    // Master switch for all of the mod's motion - breathing accent glows,
    // pop-in card animations, stat-increase flashes. Off by default is
    // NOT the case here: it starts on, and turning it off is a true
    // zero-cost no-op (see Animator) rather than "animate slower".
    public boolean animationsEnabled = true;

    // --- Config menu layout: when on, the HUD tab of this settings menu
    // itself (not the in-game HUD) falls back to the original one-row-
    // per-setting list instead of the icon + OPTIONS + ENABLED/DISABLED
    // mod-card grid. Purely a config-screen preference for people who
    // preferred the old dense list - has no effect on anything in-game.
    public boolean legacyConfigUi = false;

    // --- Whole-mod corner/design style: when on, every panel, card, pill
    // and button in this menu (and the HUD boxes) fall back to the old
    // flat square-corner look instead of the newer rounded black-glass
    // style. A single flag (GlassTheme.legacyCorners) is flipped from the
    // Theme tab's toggle below and read by every shared GlassTheme drawing
    // helper, so this one setting affects the whole UI at once rather than
    // needing a separate switch per widget.
    public boolean oldCornerDesign = false;

    // --- Security / privacy tab: whether Discord Rich Presence is allowed
    // to run at all. On by default (matches the previous always-on
    // behavior) but now a real switch - flipping it off calls
    // DiscordPresenceManager#stop() immediately and skips #start() on the
    // next launch, so nothing gets sent to the local Discord client at all
    // for players who'd rather not share their status.
    public boolean discordRpcEnabled = true;

    // --- Streamer Mode: hides which server you're on from anything the
    // mod shows to other people - the top-of-screen Server Banner (see
    // ServerBannerModule) and the server name in Discord Rich Presence
    // (see DiscordPresenceManager) both check this and fall back to a
    // generic "on a server" state instead of the real address/name while
    // it's on. Doesn't touch anything gameplay-facing or local-only, like
    // waypoints or stats.
    public boolean streamerMode = false;

    // --- Server banner: small top-center pill showing a logo dot plus
    // the address of the server you're connected to. Purely cosmetic -
    // meant for streamers so viewers can see which server is being
    // played without the player having to say it out loud. Off the HUD
    // master toggle since it's a "who am I" watermark, not gameplay info.
    public boolean showServerBanner = true;

    // --- Master HUD toggle, flipped by the "Toggle HUD" keybind. Separate
    // from vanilla's own F1/hudHidden so the two don't fight each other. ---
    public boolean hudModulesEnabled = true;

    // --- Theme: index into ThemePreset.values(), picked from the Theme tab. ---
    public int themePreset = 0;

    // --- Performance ---
    public boolean reduceParticles = false;
    public boolean disableClouds = false;
    // Zeroes out the client's own rain/thunder gradient (see
    // WorldWeatherMixin), hiding the rain/snow overlay, sky darkening,
    // and weather fog. Purely a rendering value on the client's own
    // world - it never touches the real weather state, so crop growth,
    // cauldron filling, fire extinguishing, and lightning strikes all
    // still work exactly as if it were actually raining.
    public boolean disableRain = false;
    public boolean smoothFpsLimiter = false;
    public int fpsLimit = 260;

    // --- FPS Boost (bundled preset: render distance cap, lower entity
    // distance, Fast graphics, no entity shadows, no ambient occlusion) ---
    public boolean fpsBoostEnabled = false;
    public int boostRenderDistance = 6;
    public double boostEntityDistanceScale = 0.5;

    // --- PvP visibility (particle removal) ---
    public boolean hideCrystalParticles = false;
    public boolean hideTotemParticles = false;

    // --- Hide Players: stops other players' entities (model + nametag)
    // from being rendered. Purely visual - hitboxes, collision, and
    // actual gameplay state are untouched, only what gets drawn. ---
    public boolean hidePlayers = false;

    // --- Zoom: hold a key to temporarily narrow the FOV, like a spyglass
    // without needing one in hand. Purely a camera effect - restores the
    // player's normal FOV setting the instant the key is released. ---
    public boolean zoomEnabled = true;
    // Smaller = more zoomed in. 10 is a tight "sniper" zoom; players who
    // want a gentler zoom can raise this from the settings screen.
    public int zoomFov = 10;

    // --- Full Bright: temporarily maxes out the in-game gamma/brightness
    // slider while enabled, then restores whatever the player had it set
    // to before. Same slider vanilla already exposes in Options > Video -
    // this just gives it a quick toggle/keybind instead of digging through
    // menus, the same way many clients and resource packs do. ---
    public boolean fullBrightEnabled = false;

    // --- Hit Markers: small crosshair pulse + click sound whenever an
    // attack connects with an entity, like the hit-confirmation feedback
    // in most modern shooters. Cosmetic/audio feedback only - it doesn't
    // change damage, range, or targeting in any way. ---
    public boolean hitMarkersEnabled = true;
    public boolean hitMarkerSoundEnabled = true;

    // --- Auto Sprint: holds sprint on automatically while walking forward,
    // instead of needing to hold the sprint key - see AutoSprintManager
    // for the exact (vanilla-parity) conditions it checks before doing so. ---
    public boolean autoSprintEnabled = false;

    // --- Durability Warning: one action-bar message + a distinct "ding"
    // (see DurabilityWarningManager) the moment any equipped item or
    // armor piece crosses a low-durability threshold. On by default since
    // it only ever fires rarely and is purely a heads-up, never a
    // gameplay change. ---
    public boolean durabilityWarningEnabled = true;

    // --- Low Health Warning: one action-bar message + a distinct sound
    // (see LowHealthWarningManager) the moment health drops to ~25% of
    // max, re-arming only once it climbs back above ~40%. On by default,
    // same reasoning as the durability warning: rare, heads-up only,
    // never a gameplay change. ---
    public boolean lowHealthWarningEnabled = true;

    // --- Hunger Warning: one action-bar message + a soft "bite" sound
    // (see HungerWarningManager) the moment food level drops to 6 - the
    // same point vanilla itself disables sprinting - re-arming only once
    // it climbs back above 8. On by default, same reasoning as the low
    // health warning above: rare, heads-up only, never a gameplay
    // change. ---
    public boolean hungerWarningEnabled = true;

    // --- Chat Timestamps: prefixes incoming chat lines with a local
    // "[HH:mm]" stamp, purely client-side (see ChatTimestamps). Off by
    // default since it changes the look of every chat line, and that's
    // the kind of thing a player should opt into rather than find
    // already on. ---
    public boolean chatTimestampsEnabled = false;

    // --- Attack Particles: a small colored spark burst (styled after
    // firework sparks) spawned at the point of impact whenever an
    // attack connects with an entity, on top of/independent from Hit
    // Markers above. Purely cosmetic world-space feedback - it doesn't
    // change damage, range, or targeting in any way, and only fires on
    // attacks the player already initiated. See AttackParticleModule. ---
    public boolean attackParticlesEnabled = false;
    public AttackParticleType attackParticleType = AttackParticleType.ORANGE;
    // Clamped to [3, 18] by AttackParticleModule/the config screen's
    // stepper, even if an older/hand-edited config file has something
    // outside that range.
    public int attackParticleCount = 8;

    public void setPanelPosition(HudPosition position, int x, int y) {
        switch (position) {
            case TOP_LEFT -> {
                topLeftPanelX = x;
                topLeftPanelY = y;
            }
            case TOP_RIGHT -> {
                topRightPanelX = x;
                topRightPanelY = y;
            }
            case BOTTOM_LEFT -> {
                bottomLeftPanelX = x;
                bottomLeftPanelY = y;
            }
            case BOTTOM_RIGHT -> {
                bottomRightPanelX = x;
                bottomRightPanelY = y;
            }
        }
    }

    public int getPanelX(HudPosition position, int fallbackX) {
        return switch (position) {
            case TOP_LEFT -> topLeftPanelX >= 0 ? topLeftPanelX : fallbackX;
            case TOP_RIGHT -> topRightPanelX >= 0 ? topRightPanelX : fallbackX;
            case BOTTOM_LEFT -> bottomLeftPanelX >= 0 ? bottomLeftPanelX : fallbackX;
            case BOTTOM_RIGHT -> bottomRightPanelX >= 0 ? bottomRightPanelX : fallbackX;
        };
    }

    public int getPanelY(HudPosition position, int fallbackY) {
        return switch (position) {
            case TOP_LEFT -> topLeftPanelY >= 0 ? topLeftPanelY : fallbackY;
            case TOP_RIGHT -> topRightPanelY >= 0 ? topRightPanelY : fallbackY;
            case BOTTOM_LEFT -> bottomLeftPanelY >= 0 ? bottomLeftPanelY : fallbackY;
            case BOTTOM_RIGHT -> bottomRightPanelY >= 0 ? bottomRightPanelY : fallbackY;
        };
    }

    public static ModConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig fresh = new ModConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
