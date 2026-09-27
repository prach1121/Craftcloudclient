package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.BuildInfo;
import com.craftcloudclient.client.KeyBindings;
import com.craftcloudclient.client.config.AttackParticleType;
import com.craftcloudclient.client.config.BannerPosition;
import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.config.ProfileManager;
import com.craftcloudclient.client.presence.DiscordPresenceManager;
import com.craftcloudclient.client.performance.PerformanceManager;
import com.craftcloudclient.client.stats.KillStreakManager;
import com.craftcloudclient.client.stats.StatCategory;
import com.craftcloudclient.client.stats.StatPeriod;
import com.craftcloudclient.client.stats.StatsManager;
import com.craftcloudclient.client.update.UpdateChecker;
import com.craftcloudclient.client.visual.FullBrightManager;
import com.craftcloudclient.client.waypoint.DimensionUtil;
import com.craftcloudclient.client.waypoint.Waypoint;
import com.craftcloudclient.client.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Glass-themed config screen, redesigned around a sidebar of category
 * tabs (HUD / PvP / Performance) next to a single content pane, rather
 * than one long scrolling list of every setting. This is the same
 * layout language used by other polished third-party Minecraft clients:
 * a fixed-size floating panel, a nav rail on the left, live-applying
 * toggles on the right, and no explicit "Save" button - changes are
 * written to disk as soon as the screen closes.
 *
 * Opened with the configurable "Open Craftcloud Client Menu" keybind
 * (default: RIGHT SHIFT).
 */
public class ConfigScreen extends Screen {

    private enum Category {
        HUD("HUD Display", "FPS, TPS, Ping, CPS, Armor & Totems", GlassTheme.ACCENT),
        PVP("PvP Visibility", "Hide distracting particles", 0xFFFF6F91),
        STATS("Stats", "Hits, placements & kills - today through all-time", 0xFF55FF7A),
        WAYPOINTS("Waypoints", "Manual markers, plus auto death locations", 0xFF6FD1FF),
        PERFORMANCE("Performance", "Smoother frames while fighting", 0xFF9B8CFF),
        KEYBINDS("Keybinds", "Click a row, press a key - right-click unbinds", 0xFFFFD166),
        THEME("Theme", "Pick an accent color for the whole UI", GlassTheme.ACCENT_SECONDARY),
        SECURITY("Security", "Privacy, streamer mode & third-party status sharing", 0xFF5AD1B3),
        PROFILES("Profiles", "Save, load & rename full setting presets", 0xFFFF9F5A),
        ABOUT("About", "Version info & credits", 0xFF8FA3B8);

        final String title;
        final String subtitle;
        final int accent;

        Category(String title, String subtitle, int accent) {
            this.title = title;
            this.subtitle = subtitle;
            this.accent = accent;
        }
    }

    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 440;

    // --- Auto-scale ---
    // The panel is authored at a fixed pixel size (PANEL_WIDTH x
    // PANEL_HEIGHT) in a "virtual" coordinate space. On a window/GUI
    // Scale combination that's too small to fit that virtual space with
    // room to breathe, uiScale shrinks below 1 and the whole panel -
    // chrome, text and every child widget - is rendered through a single
    // matrix scale so it always fits on screen instead of clipping or
    // overflowing off the edges. All widget bounds, hit-testing and
    // manual layout math throughout this class stay in virtual-space
    // pixels; only render() and the mouse event overrides near the
    // bottom of the class ever need to know uiScale exists.
    private static final int SCREEN_MARGIN = 20;
    private float uiScale = 1f;
    private static final int SIDEBAR_WIDTH = 132;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_GAP = 4;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 6;
    private static final int TOGGLE_ROW_WIDTH = 205;
    private static final int PICKER_WIDTH = 82;

    // Range for the "Particles Per Attack" stepper on the PvP tab -
    // mirrors the clamp AttackParticleModule applies at spawn time.
    private static final int MIN_ATTACK_PARTICLE_COUNT = 3;
    private static final int MAX_ATTACK_PARTICLE_COUNT = 18;
    private static final int CONTENT_PAD = 16;
    private static final int WAYPOINT_ROW_HEIGHT = 11;
    private static final int ACTION_BTN_HEIGHT = 10;
    private static final int ACTION_HIDE_WIDTH = 32;
    private static final int ACTION_DELETE_WIDTH = 34;
    private static final int ACTION_RENAME_WIDTH = 40;
    private static final int ACTION_GAP = 3;

    // --- Profiles tab layout ---
    // Slightly taller than a waypoint row since each profile row only
    // needs two buttons (Load/Rename) rather than three, leaving a bit
    // more breathing room for the name text next to them.
    private static final int PROFILE_ROW_HEIGHT = 16;
    private static final int PROFILE_BTN_HEIGHT = 12;
    private static final int PROFILE_LOAD_WIDTH = 34;
    private static final int PROFILE_RENAME_WIDTH = 44;
    private static final int PROFILE_DELETE_WIDTH = 34;
    private static final int PROFILE_SAVE_BTN_WIDTH = 60;

    // --- Discord link ---
    // Small, static icon tucked into the sidebar footer next to the
    // version string - deliberately not a popup/banner and not shown on
    // first open, just a quiet click-to-join affordance that never moves
    // or demands attention.
    private static final Identifier DISCORD_ICON = Identifier.of("craftcloudclient", "textures/gui/discord_icon.png");
    private static final int DISCORD_ICON_TEX_SIZE = 64;
    private static final int DISCORD_ICON_DRAW_SIZE = 16;
    private static final String DISCORD_INVITE_URL = "https://discord.gg/WPXJ8eDYUG";

    private final List<ButtonWidget> contentWidgets = new ArrayList<>();
    private Category current = Category.HUD;

    private int panelX, panelY;
    private int contentX, contentY, contentWidth;
    private int sidebarDividerY = -1;

    // --- Scrolling ---
    // Rows are laid out top-to-bottom starting from a fixed "logical" y
    // (see buildContent); scrollOffset is subtracted from that logical y
    // to get the actual on-screen position, so scrolling down just means
    // sliding everything up by scrollOffset pixels. viewportTop/Bottom
    // mark the visible window of the content pane (below the tab header,
    // above the panel's bottom edge) - rows outside that window are
    // hidden entirely rather than clipped mid-row.
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int viewportTop, viewportBottom;
    private static final int SCROLL_SPEED = 18;

    // --- Mod-card grid (HUD tab) ---
    // The HUD tab lays its entries out as a grid of square-ish "mod
    // cards" (icon + name + OPTIONS caption on top, a full-width
    // ENABLED/DISABLED pill underneath) instead of one-row-per-setting,
    // matching the card-grid mod menu look of other polished third-party
    // Minecraft clients. gridCol/gridRowY/cardWidth are transient layout
    // state, valid only while actively laying out a grid between
    // startGrid()/endGrid() in buildContent().
    private static final int CARD_GRID_COLUMNS = 2;
    private static final int CARD_GAP = 10;
    private static final int CARD_TOP_HEIGHT = 58;
    private static final int CARD_INNER_GAP = 6;
    private static final int CARD_PILL_HEIGHT = 16;
    private static final int CARD_HEIGHT = CARD_TOP_HEIGHT + CARD_INNER_GAP + CARD_PILL_HEIGHT;
    private int gridCol = 0;
    private int gridRowY = 0;
    private int cardWidth = 0;

    // --- Stats tab state ---
    private boolean statsResetArmed = false;
    private boolean streakResetArmed = false;
    private int statsTableY;

    // --- Waypoints tab state ---
    private boolean waypointsClearArmed = false;
    private boolean deathsClearArmed = false;
    private int waypointsListY;
    /** Waypoints currently showing a "Sure?" confirm state for their individual delete button - identity-based, cleared whenever the tab rebuilds for any other reason. */
    private final java.util.Set<Waypoint> waypointDeleteArmed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** The single waypoint currently being renamed (null if none), plus the text field standing in for its row while editing. Only one row can be in rename mode at a time. */
    private Waypoint renamingWaypoint = null;
    private net.minecraft.client.gui.widget.TextFieldWidget renameField;

    // --- Search bar (every tab) ---
    // A single live-filter field shown at the top of every category,
    // right below the "On All"/"Close All" row (or in its place, for
    // tabs that don't have one). Typing filters every row built through
    // toggleRow/hudRow/bannerRow/keybindRow/buttonRow/themeRow by a
    // case-insensitive substring match against that row's own label -
    // see the "filterMatches" check near the top of each of those
    // helpers. Non-toggle content that's drawn directly rather than
    // through those helpers (the Stats table, Waypoints/Profiles list
    // rows, the About tab's static info) isn't filterable and is always
    // shown regardless of the query - this searches *settings*, not
    // dynamic user data.
    private String searchQuery = "";
    private GlassSearchField searchField;
    /** True once the user has actually typed in the search box this tab visit, so {@link #searchBarRow} knows to keep re-focusing it across the rebuild each keystroke triggers - even after backspacing back to empty. Reset whenever the tab changes. */
    private boolean searchFieldActive = false;
    /** Logical (pre-scroll) y to show a small "No matching settings" message at, or -1 when not applicable. Set at the end of buildContent() once a tab's rows have all been laid out. */
    private int noResultsY = -1;
    /** How many filterable rows survived the current query on this build pass - see {@link #filterMatches}. Reset to 0 at the top of every {@link #buildContent()} call. */
    private int matchedRowCount = 0;

    // --- Profiles tab state ---
    /** Text typed into the "New profile name" field, kept independent of the widget itself so it survives a full tab rebuild (e.g. after Loading or Renaming a different profile) instead of only living inside a widget that gets torn down and recreated. */
    private String newProfileDraft = "";
    private net.minecraft.client.gui.widget.TextFieldWidget newProfileField;
    private int profilesListY;
    /** The single profile currently being renamed (null if none), plus the text field standing in for its row while editing - mirrors renamingWaypoint/renameField above. Only one row (waypoint or profile) can be in rename mode at a time. */
    private ProfileManager.ProfileEntry renamingProfile = null;
    private net.minecraft.client.gui.widget.TextFieldWidget profileRenameField;
    /** Profiles currently showing a "Sure?" confirm state for their individual delete button - identity-based (keyed by file path), mirrors waypointDeleteArmed above. Cleared whenever the tab rebuilds for any other reason. */
    private final java.util.Set<java.nio.file.Path> profileDeleteArmed = new java.util.HashSet<>();
    /** Brief inline feedback shown under the "New profile name" row - e.g. confirming a Save, or explaining why a Save/Rename was rejected. Cleared whenever the tab is left or another action succeeds. */
    private String profileMessage = null;
    /** Logical (pre-scroll) y where {@link #profileMessage}, if any, should be drawn - set alongside it in {@link #buildProfilesSection} so render() doesn't need to re-derive the row math. */
    private int profileMessageY;

    /** Frame-based open animation (not time-based, so it's independent of tick rate). */
    private float openProgress = 0f;

    public ConfigScreen() {
        super(net.minecraft.text.Text.literal("Craftcloud Client"));
    }

    @Override
    protected void init() {
        updateScale();

        // Lay everything out in "virtual" pixels: the size the screen
        // would need to be, at 1:1, for the fixed-size panel to occupy
        // the same fraction of it that it'll occupy on the real screen
        // once uiScale is applied. When uiScale is 1 (plenty of room)
        // this is identical to this.width/this.height, so nothing
        // changes on screens where the panel already fit fine.
        int virtualWidth = Math.round(this.width / uiScale);
        int virtualHeight = Math.round(this.height / uiScale);

        panelX = virtualWidth / 2 - PANEL_WIDTH / 2;
        panelY = virtualHeight / 2 - PANEL_HEIGHT / 2;
        contentX = panelX + SIDEBAR_WIDTH + CONTENT_PAD;
        contentY = panelY + 54;
        contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - CONTENT_PAD * 2;
        viewportTop = contentY + 20;
        viewportBottom = panelY + PANEL_HEIGHT - 14;

        buildSidebar();
        buildContent();

        this.addDrawableChild(new CloseButton(panelX + PANEL_WIDTH - 26, panelY + 8, () -> {
            ModConfig.INSTANCE.save();
            this.close();
        }));

        // Sidebar footer, right next to the "vX.X.X" text - small enough
        // to read as part of the same footer row rather than a separate
        // call-to-action, and it's the one spot on the panel that's
        // never scrolled, never re-laid-out per tab, and doesn't compete
        // with any setting.
        this.addDrawableChild(new DiscordButton(
                panelX + SIDEBAR_WIDTH - 20 - DISCORD_ICON_DRAW_SIZE,
                panelY + PANEL_HEIGHT - 18 - (DISCORD_ICON_DRAW_SIZE - 8) / 2,
                this::openDiscordLink));
    }

    /**
     * Recomputes {@link #uiScale} from the current real window size.
     * Shrinks (never grows past 1x - this is scaling down for tight
     * spaces, not blowing the menu up on huge monitors) just enough that
     * the fixed {@link #PANEL_WIDTH}x{@link #PANEL_HEIGHT} panel plus a
     * {@link #SCREEN_MARGIN}-pixel breathing gap on every side always
     * fits within the actual window, regardless of resolution or GUI
     * Scale. Called from {@link #init()}, which the vanilla screen
     * system already re-invokes on every resize, so this stays correct
     * as the window is dragged around live.
     */
    private void updateScale() {
        float scaleX = (this.width - SCREEN_MARGIN * 2f) / (float) PANEL_WIDTH;
        float scaleY = (this.height - SCREEN_MARGIN * 2f) / (float) PANEL_HEIGHT;
        uiScale = Math.min(1f, Math.min(scaleX, scaleY));
        // Safety floor for pathologically tiny windows so we never divide
        // by (near) zero when converting mouse coordinates back to
        // virtual space below.
        if (uiScale < 0.35f) {
            uiScale = 0.35f;
        }
    }

    /** Real-screen-pixels -> virtual-panel-pixels, accounting for {@link #uiScale}. Used by every mouse event override below so widget hit-testing (authored in virtual space) lines up with what's drawn on screen. */
    private double toVirtualX(double realX) {
        return realX / uiScale;
    }

    private double toVirtualY(double realY) {
        return realY / uiScale;
    }

    /**
     * Sends the player to the Craftcloud Discord invite, through vanilla's
     * usual "you're about to leave Minecraft" confirmation - same
     * treatment {@link #openUpdateLink} gives the update link, so this
     * never opens a browser silently.
     */
    private void openDiscordLink() {
        Screen parent = this;
        this.client.setScreen(new net.minecraft.client.gui.screen.ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                net.minecraft.util.Util.getOperatingSystem().open(DISCORD_INVITE_URL);
            }
            this.client.setScreen(parent);
        }, DISCORD_INVITE_URL, true));
    }

    private void buildSidebar() {
        int tabX = panelX + 10;
        int tabWidth = SIDEBAR_WIDTH - 20;
        int y = panelY + 54;
        sidebarDividerY = -1;

        for (Category cat : Category.values()) {
            if (cat == Category.ABOUT) {
                y += 4;
                sidebarDividerY = y;
                y += 6;
            }
            this.addDrawableChild(new GlassTabButton(tabX, y, tabWidth, TAB_HEIGHT, shortLabel(cat),
                    cat.accent, () -> current == cat, () -> switchTo(cat),
                    () -> cat == Category.ABOUT && UpdateChecker.available().isPresent()));
            y += TAB_HEIGHT + TAB_GAP;
        }
    }

    private String shortLabel(Category cat) {
        return switch (cat) {
            case HUD -> "HUD";
            case PVP -> "PvP";
            case STATS -> "Stats";
            case WAYPOINTS -> "Waypoints";
            case PERFORMANCE -> "Performance";
            case KEYBINDS -> "Keybinds";
            case THEME -> "Theme";
            case SECURITY -> "Security";
            case PROFILES -> "Profiles";
            case ABOUT -> "About";
        };
    }

    private void switchTo(Category cat) {
        if (current == cat) return;
        current = cat;
        scrollOffset = 0;
        searchQuery = "";
        searchFieldActive = false;
        if (cat != Category.STATS) {
            statsResetArmed = false;
            streakResetArmed = false;
        }
        if (cat != Category.WAYPOINTS) {
            waypointsClearArmed = false;
            deathsClearArmed = false;
            waypointDeleteArmed.clear();
            renamingWaypoint = null;
        }
        if (cat != Category.PROFILES) {
            renamingProfile = null;
            profileMessage = null;
            profileDeleteArmed.clear();
        }
        rebuildContent();
    }

    /** Clears and re-adds every widget for the current tab. Used both when switching tabs and when a tab needs to refresh its own widgets (e.g. the reset-stats confirm step). */
    private void rebuildContent() {
        for (ButtonWidget w : contentWidgets) {
            this.remove(w);
        }
        contentWidgets.clear();
        clearRenameField();
        buildContent();
    }

    /** Removes any in-progress inline text field(s) - waypoint rename, new-profile name, profile rename, and the search bar - from the screen. Safe to call even when none are in progress. */
    private void clearRenameField() {
        if (renameField != null) {
            this.remove(renameField);
            renameField = null;
        }
        if (newProfileField != null) {
            this.remove(newProfileField);
            newProfileField = null;
        }
        if (profileRenameField != null) {
            this.remove(profileRenameField);
            profileRenameField = null;
        }
        if (searchField != null) {
            this.remove(searchField);
            searchField = null;
        }
    }

    private void buildContent() {
        ModConfig cfg = ModConfig.INSTANCE;
        int y = contentY + 26;
        matchedRowCount = 0;
        noResultsY = -1;

        y = searchBarRow(y);

        switch (current) {
            case HUD -> {
                // NOTE: hudToggles feeds this tab's "On All"/"Close All" row
                // (see bulkToggleRow). Any new HUD-tab toggle/hudRow/bannerRow
                // added below MUST also pass hudToggles as its collector, or
                // it silently won't be covered by On All/Close All.
                List<Consumer<Boolean>> hudToggles = new ArrayList<>();
                y = bulkToggleRow(y, hudToggles);
                y = buttonRow(y, "Edit HUD Layout", () -> {
                    ModConfig.INSTANCE.save();
                    this.client.setScreen(new HudLayoutScreen());
                });

                if (cfg.legacyConfigUi) {
                    // Legacy layout: original one-row-per-setting list.
                    y = hudRow(y, "FPS", () -> cfg.showFps, v -> cfg.showFps = v, () -> cfg.fpsPosition, p -> cfg.fpsPosition = p, hudToggles);
                    y = hudRow(y, "Coordinates", () -> cfg.showCoords, v -> cfg.showCoords = v, () -> cfg.coordsPosition, p -> cfg.coordsPosition = p, hudToggles);
                    y = hudRow(y, "TPS", () -> cfg.showTps, v -> cfg.showTps = v, () -> cfg.tpsPosition, p -> cfg.tpsPosition = p, hudToggles);
                    y = hudRow(y, "Ping", () -> cfg.showPing, v -> cfg.showPing = v, () -> cfg.pingPosition, p -> cfg.pingPosition = p, hudToggles);
                    y = hudRow(y, "CPS", () -> cfg.showCps, v -> cfg.showCps = v, () -> cfg.cpsPosition, p -> cfg.cpsPosition = p, hudToggles);
                    y = hudRow(y, "Armor Status", () -> cfg.showArmorStatus, v -> cfg.showArmorStatus = v, () -> cfg.armorStatusPosition, p -> cfg.armorStatusPosition = p, hudToggles);
                    y = toggleRow(y, "Durability Warning", () -> cfg.durabilityWarningEnabled, v -> cfg.durabilityWarningEnabled = v, null, hudToggles);
                    y = hudRow(y, "Stats", () -> cfg.showStatsHud, v -> cfg.showStatsHud = v, () -> cfg.statsPosition, p -> cfg.statsPosition = p, hudToggles);
                    y = hudRow(y, "Ping Graph", () -> cfg.showPingGraph, v -> cfg.showPingGraph = v, () -> cfg.pingGraphPosition, p -> cfg.pingGraphPosition = p, hudToggles);
                    y = hudRow(y, "Status Graph", () -> cfg.showStatusGraph, v -> cfg.showStatusGraph = v, () -> cfg.statusGraphPosition, p -> cfg.statusGraphPosition = p, hudToggles);
                    y = hudRow(y, "Kill Streak", () -> cfg.showKillStreak, v -> cfg.showKillStreak = v, () -> cfg.killStreakPosition, p -> cfg.killStreakPosition = p, hudToggles);
                    y = hudRow(y, "Keystrokes", () -> cfg.showKeystrokes, v -> cfg.showKeystrokes = v, () -> cfg.keystrokesPosition, p -> cfg.keystrokesPosition = p, hudToggles);
                    y = hudRow(y, "Time Until Day/Night", () -> cfg.showTimeUntilDay, v -> cfg.showTimeUntilDay = v, () -> cfg.timeUntilDayPosition, p -> cfg.timeUntilDayPosition = p, hudToggles);
                    y = hudRow(y, "Playtime", () -> cfg.showPlaytime, v -> cfg.showPlaytime = v, () -> cfg.playtimePosition, p -> cfg.playtimePosition = p, hudToggles);
                    y = hudRow(y, "Light Level", () -> cfg.showLightLevel, v -> cfg.showLightLevel = v, () -> cfg.lightLevelPosition, p -> cfg.lightLevelPosition = p, hudToggles);
                    y = hudRow(y, "Clock", () -> cfg.showClock, v -> cfg.showClock = v, () -> cfg.clockPosition, p -> cfg.clockPosition = p, hudToggles);
                    y = toggleRow(y, "Clock: 24-Hour", () -> cfg.clockUse24Hour, v -> cfg.clockUse24Hour = v, null, hudToggles);
                    y = hudRow(y, "Biome", () -> cfg.showBiome, v -> cfg.showBiome = v, () -> cfg.biomePosition, p -> cfg.biomePosition = p, hudToggles);
                    y = hudRow(y, "Hostile Mob Counter", () -> cfg.showHostileMobs, v -> cfg.showHostileMobs = v, () -> cfg.hostileMobsPosition, p -> cfg.hostileMobsPosition = p, hudToggles);
                    y = hudRow(y, "World Border Distance", () -> cfg.showWorldBorder, v -> cfg.showWorldBorder = v, () -> cfg.worldBorderPosition, p -> cfg.worldBorderPosition = p, hudToggles);
                    y = hudRow(y, "Distance Traveled", () -> cfg.showDistanceTraveled, v -> cfg.showDistanceTraveled = v, () -> cfg.distanceTraveledPosition, p -> cfg.distanceTraveledPosition = p, hudToggles);
                    y = hudRow(y, "XP Gained", () -> cfg.showXpGained, v -> cfg.showXpGained = v, () -> cfg.xpGainedPosition, p -> cfg.xpGainedPosition = p, hudToggles);
                    y = hudRow(y, "Combat Timer", () -> cfg.showCombatTimer, v -> cfg.showCombatTimer = v, () -> cfg.combatTimerPosition, p -> cfg.combatTimerPosition = p, hudToggles);
                    y = toggleRow(y, "Low Health Warning", () -> cfg.lowHealthWarningEnabled, v -> cfg.lowHealthWarningEnabled = v, null, hudToggles);
                    y = toggleRow(y, "Hunger Warning", () -> cfg.hungerWarningEnabled, v -> cfg.hungerWarningEnabled = v, null, hudToggles);
                    y = toggleRow(y, "Totem Counter", () -> cfg.showTotemCounter, v -> cfg.showTotemCounter = v, null, hudToggles);
                    y = toggleRow(y, "Hide Boss Bar", () -> cfg.hideBossBar, v -> cfg.hideBossBar = v, null, hudToggles);
                    y = toggleRow(y, "Smooth UI (Glass)", () -> cfg.glassBackground, v -> cfg.glassBackground = v, null, hudToggles);
                    y = toggleRow(y, "Animations", () -> cfg.animationsEnabled, v -> cfg.animationsEnabled = v, null, hudToggles);
                    y = toggleRow(y, "Chat Timestamps", () -> cfg.chatTimestampsEnabled, v -> cfg.chatTimestampsEnabled = v, null, hudToggles);
                } else {
                    // Everything below is laid out as a grid of mod cards
                    // (icon + name + OPTIONS on top, an ENABLED/DISABLED pill
                    // underneath) instead of one-row-per-setting - see
                    // startGrid/hudModCard/toggleModCard/endGrid.
                    startGrid(y);
                    hudModCard("FPS", () -> cfg.showFps, v -> cfg.showFps = v, () -> cfg.fpsPosition, p -> cfg.fpsPosition = p, hudToggles);
                    hudModCard("Coordinates", () -> cfg.showCoords, v -> cfg.showCoords = v, () -> cfg.coordsPosition, p -> cfg.coordsPosition = p, hudToggles);
                    hudModCard("TPS", () -> cfg.showTps, v -> cfg.showTps = v, () -> cfg.tpsPosition, p -> cfg.tpsPosition = p, hudToggles);
                    hudModCard("Ping", () -> cfg.showPing, v -> cfg.showPing = v, () -> cfg.pingPosition, p -> cfg.pingPosition = p, hudToggles);
                    hudModCard("CPS", () -> cfg.showCps, v -> cfg.showCps = v, () -> cfg.cpsPosition, p -> cfg.cpsPosition = p, hudToggles);
                    hudModCard("Armor Status", () -> cfg.showArmorStatus, v -> cfg.showArmorStatus = v, () -> cfg.armorStatusPosition, p -> cfg.armorStatusPosition = p, hudToggles);
                    toggleModCard("Durability Warning", () -> cfg.durabilityWarningEnabled, v -> cfg.durabilityWarningEnabled = v, hudToggles);
                    hudModCard("Stats", () -> cfg.showStatsHud, v -> cfg.showStatsHud = v, () -> cfg.statsPosition, p -> cfg.statsPosition = p, hudToggles);
                    hudModCard("Ping Graph", () -> cfg.showPingGraph, v -> cfg.showPingGraph = v, () -> cfg.pingGraphPosition, p -> cfg.pingGraphPosition = p, hudToggles);
                    hudModCard("Status Graph", () -> cfg.showStatusGraph, v -> cfg.showStatusGraph = v, () -> cfg.statusGraphPosition, p -> cfg.statusGraphPosition = p, hudToggles);
                    hudModCard("Kill Streak", () -> cfg.showKillStreak, v -> cfg.showKillStreak = v, () -> cfg.killStreakPosition, p -> cfg.killStreakPosition = p, hudToggles);
                    hudModCard("Keystrokes", () -> cfg.showKeystrokes, v -> cfg.showKeystrokes = v, () -> cfg.keystrokesPosition, p -> cfg.keystrokesPosition = p, hudToggles);
                    hudModCard("Time Until Day/Night", () -> cfg.showTimeUntilDay, v -> cfg.showTimeUntilDay = v, () -> cfg.timeUntilDayPosition, p -> cfg.timeUntilDayPosition = p, hudToggles);
                    hudModCard("Playtime", () -> cfg.showPlaytime, v -> cfg.showPlaytime = v, () -> cfg.playtimePosition, p -> cfg.playtimePosition = p, hudToggles);
                    hudModCard("Light Level", () -> cfg.showLightLevel, v -> cfg.showLightLevel = v, () -> cfg.lightLevelPosition, p -> cfg.lightLevelPosition = p, hudToggles);
                    hudModCard("Clock", () -> cfg.showClock, v -> cfg.showClock = v, () -> cfg.clockPosition, p -> cfg.clockPosition = p, hudToggles);
                    toggleModCard("Clock: 24-Hour", () -> cfg.clockUse24Hour, v -> cfg.clockUse24Hour = v, hudToggles);
                    hudModCard("Biome", () -> cfg.showBiome, v -> cfg.showBiome = v, () -> cfg.biomePosition, p -> cfg.biomePosition = p, hudToggles);
                    hudModCard("Hostile Mob Counter", () -> cfg.showHostileMobs, v -> cfg.showHostileMobs = v, () -> cfg.hostileMobsPosition, p -> cfg.hostileMobsPosition = p, hudToggles);
                    hudModCard("World Border Distance", () -> cfg.showWorldBorder, v -> cfg.showWorldBorder = v, () -> cfg.worldBorderPosition, p -> cfg.worldBorderPosition = p, hudToggles);
                    hudModCard("Distance Traveled", () -> cfg.showDistanceTraveled, v -> cfg.showDistanceTraveled = v, () -> cfg.distanceTraveledPosition, p -> cfg.distanceTraveledPosition = p, hudToggles);
                    hudModCard("XP Gained", () -> cfg.showXpGained, v -> cfg.showXpGained = v, () -> cfg.xpGainedPosition, p -> cfg.xpGainedPosition = p, hudToggles);
                    hudModCard("Combat Timer", () -> cfg.showCombatTimer, v -> cfg.showCombatTimer = v, () -> cfg.combatTimerPosition, p -> cfg.combatTimerPosition = p, hudToggles);
                    toggleModCard("Low Health Warning", () -> cfg.lowHealthWarningEnabled, v -> cfg.lowHealthWarningEnabled = v, hudToggles);
                    toggleModCard("Hunger Warning", () -> cfg.hungerWarningEnabled, v -> cfg.hungerWarningEnabled = v, hudToggles);
                    toggleModCard("Totem Counter", () -> cfg.showTotemCounter, v -> cfg.showTotemCounter = v, hudToggles);
                    toggleModCard("Hide Boss Bar", () -> cfg.hideBossBar, v -> cfg.hideBossBar = v, hudToggles);
                    toggleModCard("Smooth UI (Glass)", () -> cfg.glassBackground, v -> cfg.glassBackground = v, hudToggles);
                    toggleModCard("Animations", () -> cfg.animationsEnabled, v -> cfg.animationsEnabled = v, hudToggles);
                    toggleModCard("Chat Timestamps", () -> cfg.chatTimestampsEnabled, v -> cfg.chatTimestampsEnabled = v, hudToggles);
                    y = endGrid();
                }

                // Server Banner keeps its own full-width row below the grid:
                // it cycles a BannerPosition (which adds a center option),
                // not the plain 4-corner HudPosition the cards above use.
                y = bannerRow(y, "Server Banner", () -> cfg.showServerBanner, v -> cfg.showServerBanner = v,
                        () -> cfg.serverBannerPosition, p -> {
                            cfg.serverBannerPosition = p;
                            // Picking a position here should win over any earlier free-drag
                            // placement from the HUD layout editor, so clear it.
                            cfg.serverBannerX = -1;
                            cfg.serverBannerY = -1;
                        }, hudToggles);
            }
            case PVP -> {
                // NOTE: same rule as hudToggles above - any new PvP-tab
                // toggle added below MUST also pass pvpToggles, or On
                // All/Close All silently won't cover it.
                List<Consumer<Boolean>> pvpToggles = new ArrayList<>();
                y = bulkToggleRow(y, pvpToggles);
                y = toggleRow(y, "Hide Crystal Particles", () -> cfg.hideCrystalParticles, v -> cfg.hideCrystalParticles = v, null, pvpToggles);
                y = toggleRow(y, "Hide Totem Particles", () -> cfg.hideTotemParticles, v -> cfg.hideTotemParticles = v, null, pvpToggles);
                y = toggleRow(y, "Hit Markers", () -> cfg.hitMarkersEnabled, v -> cfg.hitMarkersEnabled = v, null, pvpToggles);
                y = toggleRow(y, "Hit Marker Sound", () -> cfg.hitMarkerSoundEnabled, v -> cfg.hitMarkerSoundEnabled = v, null, pvpToggles);
                y = toggleRow(y, "Attack Particles", () -> cfg.attackParticlesEnabled, v -> cfg.attackParticlesEnabled = v, null, pvpToggles);
                y = particleColorRow(y, "Attack Particle Color", () -> cfg.attackParticleType, t -> cfg.attackParticleType = t);
                y = stepperRow(y, "Particles Per Attack", () -> cfg.attackParticleCount, v -> cfg.attackParticleCount = v,
                        MIN_ATTACK_PARTICLE_COUNT, MAX_ATTACK_PARTICLE_COUNT);
            }
            case STATS -> {
                y = buttonRow(y, statsResetArmed ? "Click Again to Confirm Reset" : "Reset Stats", () -> {
                    if (statsResetArmed) {
                        StatsManager.INSTANCE.resetAll();
                        statsResetArmed = false;
                    } else {
                        statsResetArmed = true;
                    }
                    rebuildContent();
                });
                y = buttonRow(y, streakResetArmed ? "Click Again to Confirm Reset" : "Reset Best Streak", () -> {
                    if (streakResetArmed) {
                        KillStreakManager.INSTANCE.resetBest();
                        streakResetArmed = false;
                    } else {
                        streakResetArmed = true;
                    }
                    rebuildContent();
                });
                statsTableY = y + 4;
                // drawStatsTable/the streak line below it are drawn directly
                // in render() rather than as widgets, but their height still
                // needs to count toward this tab's scroll range.
                y = statsTableY + 16 + StatCategory.values().length * 16 + 6 + 14;
            }
            case WAYPOINTS -> {
                y = buttonRow(y, waypointsClearArmed ? "Click Again to Confirm Clear" : "Clear Waypoints", () -> {
                    if (waypointsClearArmed) {
                        WaypointManager.INSTANCE.clearUserWaypoints();
                        waypointsClearArmed = false;
                    } else {
                        waypointsClearArmed = true;
                    }
                    rebuildContent();
                });
                y = buttonRow(y, deathsClearArmed ? "Click Again to Confirm Clear" : "Clear Death History", () -> {
                    if (deathsClearArmed) {
                        WaypointManager.INSTANCE.clearDeathMarkers();
                        deathsClearArmed = false;
                    } else {
                        deathsClearArmed = true;
                    }
                    rebuildContent();
                });
                y = toggleRow(y, "Waypoint HUD", () -> cfg.showWaypointHud, v -> cfg.showWaypointHud = v, null);
                y = toggleRow(y, "Waypoint Navigator", () -> cfg.showWaypointNavigator, v -> cfg.showWaypointNavigator = v, null);
                waypointsListY = y + 4;
                y = buildWaypointActionButtons(waypointsListY);
            }
            case PERFORMANCE -> {
                // NOTE: same rule as hudToggles above - any new
                // Performance-tab toggle added below MUST also pass
                // perfToggles, or On All/Close All silently won't cover it.
                // If the new toggle also needs a live-apply side effect
                // beyond its own onChange (like PerformanceManager.apply()
                // or FullBrightManager.apply() below), add that call to
                // applyBulkToggleSideEffects() too, since a bulk On
                // All/Close All click bypasses each row's individual
                // onChange callback.
                List<Consumer<Boolean>> perfToggles = new ArrayList<>();
                y = bulkToggleRow(y, perfToggles);
                y = toggleRow(y, "FPS Boost", () -> cfg.fpsBoostEnabled, v -> cfg.fpsBoostEnabled = v, PerformanceManager::apply, perfToggles);
                y = toggleRow(y, "Reduce Particles", () -> cfg.reduceParticles, v -> cfg.reduceParticles = v, PerformanceManager::apply, perfToggles);
                y = toggleRow(y, "Disable Clouds", () -> cfg.disableClouds, v -> cfg.disableClouds = v, PerformanceManager::apply, perfToggles);
                y = toggleRow(y, "Disable Rain", () -> cfg.disableRain, v -> cfg.disableRain = v, null, perfToggles);
                y = toggleRow(y, "Smooth FPS Limiter", () -> cfg.smoothFpsLimiter, v -> cfg.smoothFpsLimiter = v, PerformanceManager::apply, perfToggles);
                y = toggleRow(y, "Zoom (Hold Key)", () -> cfg.zoomEnabled, v -> cfg.zoomEnabled = v, null, perfToggles);
                y = toggleRow(y, "Full Bright", () -> cfg.fullBrightEnabled, v -> cfg.fullBrightEnabled = v,
                        () -> FullBrightManager.apply(this.client, cfg.fullBrightEnabled), perfToggles);
                y = toggleRow(y, "Auto Sprint", () -> cfg.autoSprintEnabled, v -> cfg.autoSprintEnabled = v, null, perfToggles);
                y = toggleRow(y, "Hide Players", () -> cfg.hidePlayers, v -> cfg.hidePlayers = v, null, perfToggles);
            }
            case KEYBINDS -> {
                y = keybindRow(y, "Open Menu", KeyBindings.OPEN_MENU);
                y = keybindRow(y, "Toggle FPS Boost", KeyBindings.TOGGLE_FPS_BOOST);
                y = keybindRow(y, "Toggle PvP Mode", KeyBindings.TOGGLE_PVP_MODE);
                y = keybindRow(y, "Toggle HUD", KeyBindings.TOGGLE_HUD);
                y = keybindRow(y, "Edit HUD Layout", KeyBindings.EDIT_HUD_LAYOUT);
                y = keybindRow(y, "Say GG", KeyBindings.SAY_GG);
                y = keybindRow(y, "Add Waypoint", KeyBindings.ADD_WAYPOINT);
                y = keybindRow(y, "Zoom", KeyBindings.ZOOM);
                y = keybindRow(y, "Toggle Full Bright", KeyBindings.TOGGLE_FULL_BRIGHT);
                y = keybindRow(y, "Toggle Auto Sprint", KeyBindings.TOGGLE_AUTO_SPRINT);
                y = keybindRow(y, "Toggle Rain", KeyBindings.TOGGLE_RAIN);
                y = keybindRow(y, "Toggle Hide Players", KeyBindings.TOGGLE_HIDE_PLAYERS);
            }
            case THEME -> {
                for (ThemePreset preset : ThemePreset.values()) {
                    y = themeRow(y, preset);
                }
                y += 4;
                y = toggleRow(y, "Old Corner Design", () -> cfg.oldCornerDesign, v -> {
                    cfg.oldCornerDesign = v;
                    GlassTheme.legacyCorners = v;
                }, null, null);
                y += 4;
                y = toggleRow(y, "Legacy Config UI", () -> cfg.legacyConfigUi, v -> cfg.legacyConfigUi = v, () -> {
                    // The HUD tab's own layout (list vs. card grid) depends on
                    // this, so if it's the tab currently open, rebuild it now
                    // rather than waiting for the next tab switch.
                    if (current == Category.HUD) {
                        rebuildContent();
                    }
                }, null);
            }
            case SECURITY -> {
                y = toggleRow(y, "Discord Status (RPC)", () -> cfg.discordRpcEnabled, v -> {
                    cfg.discordRpcEnabled = v;
                    if (v) {
                        DiscordPresenceManager.start();
                    } else {
                        DiscordPresenceManager.stop();
                    }
                }, null, null);
                y += 4;
                y = toggleRow(y, "Streamer Mode", () -> cfg.streamerMode, v -> cfg.streamerMode = v, null, null);
            }
            case PROFILES -> {
                y = buildProfilesSection(y);
            }
            case ABOUT -> {
                // drawAboutSection is drawn directly in render() (static,
                // read-only info) rather than as widgets - ABOUT_CONTENT_HEIGHT
                // mirrors its row math so this tab still gets a correct
                // scroll range, even though in practice it's short enough
                // to never need one.
                y += ABOUT_CONTENT_HEIGHT;

                // Real widget (unlike the static info above) since it needs
                // to be clickable - only added at all when an update is
                // actually available, so the tab stays exactly as tall as
                // before otherwise.
                Optional<UpdateChecker.UpdateInfo> update = UpdateChecker.available();
                if (update.isPresent()) {
                    UpdateChecker.UpdateInfo info = update.get();
                    y += 6;
                    y = buttonRow(y, "Update Available: v" + info.version() + " \u2013 Open Modrinth", () -> openUpdateLink(info));
                }
            }
        }

        if (!searchQuery.isBlank() && matchedRowCount == 0) {
            noResultsY = y;
            y += ROW_HEIGHT;
        }

        maxScroll = Math.max(0, y - viewportBottom);
        if (scrollOffset > maxScroll) {
            // Content got shorter while scrolled near the bottom (e.g. a
            // waypoint was just deleted) - snap back up and lay out again
            // at the corrected offset. maxScroll won't change on the retry
            // since it only depends on row counts, so this always settles
            // after a single extra pass.
            scrollOffset = maxScroll;
            for (ButtonWidget w : contentWidgets) {
                this.remove(w);
            }
            contentWidgets.clear();
            clearRenameField();
            buildContent();
        }
    }

    /** Approximate height of {@link #drawAboutSection}'s static content, kept in sync with its row math by hand since that method draws directly rather than through the row helpers above. */
    private static final int ABOUT_CONTENT_HEIGHT = 4 * 14 + 8 + 10 + 14 + (14 - 3) * 2 + 14;

    /**
     * Whether a row of the given height placed at actualY (already
     * scroll-shifted) overlaps the visible content viewport at all. This
     * only gates whether a row can be hovered/clicked while scrolled -
     * actual pixel-level clipping so a half-scrolled row is cut off
     * cleanly at the viewport edge (rather than fully hidden, or bleeding
     * past the panel) is handled separately by the GL scissor in
     * {@link #renderContentWidgets}.
     */
    private boolean isRowVisible(int actualY, int rowHeight) {
        return actualY + rowHeight > viewportTop && actualY < viewportBottom;
    }

    /** Hides+disables a row's widget(s) when scrolled out of view, so they neither render nor intercept clicks. */
    private void applyRowVisibility(ButtonWidget widget, int actualY, int rowHeight) {
        boolean visible = isRowVisible(actualY, rowHeight);
        widget.visible = visible;
        widget.active = visible;
    }

    /**
     * Builds the live search field shown at the top of every tab. Reserves
     * {@link GlassSearchField#ICON_AREA} pixels to its left for the
     * magnifying-glass icon the widget draws itself (see that class) by
     * starting the actual field that far past contentX, while still
     * treating the full contentWidth as this row's footprint for layout
     * purposes.
     */
    private int searchBarRow(int y) {
        int actualY = y - scrollOffset;
        int fieldX = contentX + GlassSearchField.ICON_AREA;
        int fieldWidth = contentWidth - GlassSearchField.ICON_AREA;

        searchField = new GlassSearchField(fieldX, actualY, fieldWidth, ROW_HEIGHT);
        searchField.setText(searchQuery);
        searchField.setChangedListener(text -> {
            // Vanilla TextFieldWidget can fire the changed-listener on plain
            // clicks/focus/cursor-placement, not just real edits - with no
            // change in the text at all. Since rebuildContent() tears down
            // and reconstructs this very field, reacting to a no-op
            // notification here re-enters this listener and recurses
            // without ever bottoming out (StackOverflowError). Only rebuild
            // when the text actually changed.
            if (text.equals(searchQuery)) {
                return;
            }
            searchQuery = text;
            searchFieldActive = true;
            rebuildContent();
        });
        boolean visible = isRowVisible(actualY, ROW_HEIGHT);
        searchField.visible = visible;
        searchField.active = visible;
        this.addSelectableChild(searchField);
        if (visible && searchFieldActive) {
            // Only steal focus back while actively filtering - otherwise
            // opening a tab (or clicking elsewhere) would keep yanking
            // keyboard focus into the search box for no reason.
            //
            // searchField.setFocused(true) only flips this widget's own
            // internal flag - it does NOT tell the Screen that THIS
            // (newly rebuilt) field is the one that should receive
            // keyboard events. Since rebuildContent() discards and
            // recreates searchField on every keystroke, the Screen's own
            // focus target kept pointing at the previous (now-discarded)
            // instance, so only the very first character after
            // (re)focusing ever reached a field - every keystroke after
            // that landed on an orphaned widget and silently did nothing,
            // until the user clicked the bar again to re-target focus.
            // Retargeting the Screen's focus here fixes that.
            this.setFocused(searchField);
            searchField.setFocused(true);
            searchField.setCursorToEnd(false);
        }
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /**
     * Whether a row labeled {@code label} should be built at all under
     * the current {@link #searchQuery} - a plain case-insensitive
     * substring match, same as every other in-game search box. Always
     * true when the query is blank. Bumps {@link #matchedRowCount} on a
     * match so {@link #buildContent()} can tell a tab with zero matches
     * apart from one with nothing to search in the first place.
     */
    private boolean filterMatches(String label) {
        if (searchQuery.isBlank()) {
            matchedRowCount++;
            return true;
        }
        boolean matches = label.toLowerCase(java.util.Locale.ROOT).contains(searchQuery.toLowerCase(java.util.Locale.ROOT));
        if (matches) {
            matchedRowCount++;
        }
        return matches;
    }

    private int toggleRow(int y, String label, BooleanSupplier getter, Consumer<Boolean> setter, Runnable onChange) {
        return toggleRow(y, label, getter, setter, onChange, null);
    }

    /** Same as above, but also registers this row's setter with a per-tab collector so a "On All"/"Close All" pair (see {@link #bulkToggleRow}) can drive every toggle on the tab at once. */
    private int toggleRow(int y, String label, BooleanSupplier getter, Consumer<Boolean> setter, Runnable onChange, List<Consumer<Boolean>> collector) {
        if (collector != null) {
            collector.add(setter);
        }
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassToggleRow row = new GlassToggleRow(contentX, actualY, contentWidth, ROW_HEIGHT, label, getter, setter, onChange);
        applyRowVisibility(row, actualY, ROW_HEIGHT);
        contentWidgets.add(row);
        this.addSelectableChild(row);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /**
     * A single row holding two side-by-side buttons - "On All" and "Close
     * All" - that flip every toggle registered in {@code collector} (via
     * {@link #toggleRow}/{@link #hudRow}/{@link #bannerRow}'s collector
     * overloads) to true/false in one click, then saves and rebuilds the
     * tab so every switch and HUD preview reflects the change immediately.
     * {@code collector} is populated as the rest of the tab's rows are
     * built below this one; that's fine because these buttons only read
     * it later, when clicked, by which point buildContent() has already
     * finished adding every row for the tab.
     *
     * IMPORTANT for future features: coverage here is opt-in per row, not
     * automatic. Adding a new toggle/hudRow/bannerRow to the HUD, PvP, or
     * Performance tab means it must also be passed the same tab's
     * collector list (see the "hudToggles"/"pvpToggles"/"perfToggles"
     * NOTE comments at the top of each case in buildContent) - otherwise
     * On All/Close All will quietly skip it. If the new toggle needs a
     * live-apply side effect beyond its own onChange, add it to
     * {@link #applyBulkToggleSideEffects()} as well.
     */
    private int bulkToggleRow(int y, List<Consumer<Boolean>> collector) {
        int actualY = y - scrollOffset;
        int gap = 8;
        int halfWidth = (contentWidth - gap) / 2;

        GlassButton onAll = new GlassButton(contentX, actualY, halfWidth, ROW_HEIGHT, "On All", () -> {
            for (Consumer<Boolean> setter : collector) {
                setter.accept(true);
            }
            applyBulkToggleSideEffects();
            ModConfig.INSTANCE.save();
            rebuildContent();
        });
        GlassButton offAll = new GlassButton(contentX + halfWidth + gap, actualY, halfWidth, ROW_HEIGHT, "Close All", () -> {
            for (Consumer<Boolean> setter : collector) {
                setter.accept(false);
            }
            applyBulkToggleSideEffects();
            ModConfig.INSTANCE.save();
            rebuildContent();
        });

        applyRowVisibility(onAll, actualY, ROW_HEIGHT);
        applyRowVisibility(offAll, actualY, ROW_HEIGHT);
        contentWidgets.add(onAll);
        contentWidgets.add(offAll);
        this.addSelectableChild(onAll);
        this.addSelectableChild(offAll);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /**
     * Re-runs the same live-apply hooks the individual PERFORMANCE/HUD
     * toggle rows pass as their {@code onChange} callback, since a bulk
     * "On All"/"Close All" click bypasses those per-row callbacks
     * entirely (it writes straight into the config via each collected
     * setter). Harmless to call from the HUD/PvP tabs too - both
     * managers just no-op reapply their own (unrelated) config fields.
     */
    private void applyBulkToggleSideEffects() {
        PerformanceManager.apply();
        if (this.client != null) {
            FullBrightManager.apply(this.client, ModConfig.INSTANCE.fullBrightEnabled);
        }
    }

    private int keybindRow(int y, String label, net.minecraft.client.option.KeyBinding binding) {
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassKeybindRow row = new GlassKeybindRow(contentX, actualY, contentWidth, ROW_HEIGHT, label, binding);
        applyRowVisibility(row, actualY, ROW_HEIGHT);
        contentWidgets.add(row);
        this.addSelectableChild(row);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private int buttonRow(int y, String label, Runnable action) {
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassButton button = new GlassButton(contentX, actualY, contentWidth, ROW_HEIGHT, label, action);
        applyRowVisibility(button, actualY, ROW_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /**
     * Sends the player to an update's Modrinth page, through vanilla's
     * usual "you're about to leave Minecraft" confirmation - same
     * treatment every other external link in the game gets.
     * NOTE: {@code ConfirmLinkScreen}'s exact constructor has moved a
     * couple of times across recent versions; if this doesn't match your
     * mappings, check its available overloads - one always takes a
     * (BooleanConsumer callback, String url, boolean trusted).
     */
    private void openUpdateLink(UpdateChecker.UpdateInfo info) {
        Screen parent = this;
        this.client.setScreen(new net.minecraft.client.gui.screen.ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                net.minecraft.util.Util.getOperatingSystem().open(info.url());
            }
            this.client.setScreen(parent);
        }, info.url(), true));
    }

    private int themeRow(int y, ThemePreset preset) {
        if (!filterMatches(preset.label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassButton button = new GlassButton(contentX, actualY, contentWidth, ROW_HEIGHT, preset.label, () -> {
            preset.apply();
            ModConfig.INSTANCE.themePreset = preset.ordinal();
            ModConfig.INSTANCE.save();
        }, () -> ModConfig.INSTANCE.themePreset == preset.ordinal());
        applyRowVisibility(button, actualY, ROW_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /** Full-width row for {@link AttackParticleType}: label on the left, swatch + name on the right, click cycles - see {@link GlassParticleColorRow}. */
    private int particleColorRow(int y, String label, Supplier<AttackParticleType> getter, Consumer<AttackParticleType> setter) {
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassParticleColorRow row = new GlassParticleColorRow(contentX, actualY, contentWidth, ROW_HEIGHT, label, getter, setter);
        applyRowVisibility(row, actualY, ROW_HEIGHT);
        contentWidgets.add(row);
        this.addSelectableChild(row);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /**
     * Full-width row for a clamped int value with -/+ click zones - see
     * {@link GlassStepperRow}. Like the plain toggle/HUD rows, changes
     * here are only written to disk when the screen closes (see
     * {@link #removed()}), not on every click.
     */
    private int stepperRow(int y, String label, java.util.function.IntSupplier getter, java.util.function.IntConsumer setter, int min, int max) {
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassStepperRow row = new GlassStepperRow(contentX, actualY, contentWidth, ROW_HEIGHT, label, getter, setter, min, max, null);
        applyRowVisibility(row, actualY, ROW_HEIGHT);
        contentWidgets.add(row);
        this.addSelectableChild(row);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private int hudRow(int y, String label, BooleanSupplier getter, Consumer<Boolean> setter,
                        Supplier<HudPosition> posGetter, Consumer<HudPosition> posSetter) {
        return hudRow(y, label, getter, setter, posGetter, posSetter, null);
    }

    /** Same as above, but also registers this row's setter with a per-tab collector - see {@link #bulkToggleRow}. */
    private int hudRow(int y, String label, BooleanSupplier getter, Consumer<Boolean> setter,
                        Supplier<HudPosition> posGetter, Consumer<HudPosition> posSetter, List<Consumer<Boolean>> collector) {
        if (collector != null) {
            collector.add(setter);
        }
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassToggleRow row = new GlassToggleRow(contentX, actualY, TOGGLE_ROW_WIDTH, ROW_HEIGHT, label + " Display", getter, setter);
        GlassCornerPicker picker = new GlassCornerPicker(contentX + TOGGLE_ROW_WIDTH + 8, actualY, PICKER_WIDTH, ROW_HEIGHT, posGetter, posSetter);
        applyRowVisibility(row, actualY, ROW_HEIGHT);
        applyRowVisibility(picker, actualY, ROW_HEIGHT);
        contentWidgets.add(row);
        contentWidgets.add(picker);
        this.addSelectableChild(row);
        this.addSelectableChild(picker);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /**
     * Same as {@link #hudRow}, but for the Server Banner, which uses
     * {@link BannerPosition} (adds a center option) instead of the plain
     * 4-corner {@link HudPosition} the other HUD rows use.
     */
    private int bannerRow(int y, String label, BooleanSupplier getter, Consumer<Boolean> setter,
                           Supplier<BannerPosition> posGetter, Consumer<BannerPosition> posSetter) {
        return bannerRow(y, label, getter, setter, posGetter, posSetter, null);
    }

    /** Same as above, but also registers this row's setter with a per-tab collector - see {@link #bulkToggleRow}. */
    private int bannerRow(int y, String label, BooleanSupplier getter, Consumer<Boolean> setter,
                           Supplier<BannerPosition> posGetter, Consumer<BannerPosition> posSetter, List<Consumer<Boolean>> collector) {
        if (collector != null) {
            collector.add(setter);
        }
        if (!filterMatches(label)) {
            return y;
        }
        int actualY = y - scrollOffset;
        GlassToggleRow row = new GlassToggleRow(contentX, actualY, TOGGLE_ROW_WIDTH, ROW_HEIGHT, label + " Display", getter, setter);
        GlassBannerPositionPicker picker = new GlassBannerPositionPicker(contentX + TOGGLE_ROW_WIDTH + 8, actualY, PICKER_WIDTH, ROW_HEIGHT, posGetter, posSetter);
        applyRowVisibility(row, actualY, ROW_HEIGHT);
        applyRowVisibility(picker, actualY, ROW_HEIGHT);
        contentWidgets.add(row);
        contentWidgets.add(picker);
        this.addSelectableChild(row);
        this.addSelectableChild(picker);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /** Starts a fresh mod-card grid at logical y {@code y} - call once, then call {@link #hudModCard}/{@link #toggleModCard} for each entry, then {@link #endGrid} to get the y to resume normal rows at. */
    private void startGrid(int y) {
        gridCol = 0;
        gridRowY = y;
        cardWidth = (contentWidth - CARD_GAP * (CARD_GRID_COLUMNS - 1)) / CARD_GRID_COLUMNS;
    }

    /** A grid card for a HUD element that has an on-screen corner it can be anchored to - the card's OPTIONS caption cycles it, same as {@link #hudRow}'s separate position picker used to. */
    private void hudModCard(String label, BooleanSupplier getter, Consumer<Boolean> setter,
                             Supplier<HudPosition> posGetter, Consumer<HudPosition> posSetter,
                             List<Consumer<Boolean>> collector) {
        modCard(label, getter, setter, posGetter, posSetter, collector);
    }

    /** A grid card for a plain on/off setting with no position to place - same tile shape, but without an OPTIONS caption. */
    private void toggleModCard(String label, BooleanSupplier getter, Consumer<Boolean> setter, List<Consumer<Boolean>> collector) {
        modCard(label, getter, setter, null, null, collector);
    }

    private void modCard(String label, BooleanSupplier getter, Consumer<Boolean> setter,
                          Supplier<HudPosition> posGetter, Consumer<HudPosition> posSetter,
                          List<Consumer<Boolean>> collector) {
        if (collector != null) {
            collector.add(setter);
        }
        if (!filterMatches(label)) {
            return;
        }

        int cardX = contentX + gridCol * (cardWidth + CARD_GAP);
        int actualY = gridRowY - scrollOffset;
        boolean visible = isRowVisible(actualY, CARD_HEIGHT);

        GlassModCard top = new GlassModCard(cardX, actualY, cardWidth, CARD_TOP_HEIGHT, label, GlassTheme.ACCENT, posGetter, posSetter);
        GlassEnabledPill pill = new GlassEnabledPill(cardX, actualY + CARD_TOP_HEIGHT + CARD_INNER_GAP, cardWidth, CARD_PILL_HEIGHT, getter, setter);
        top.visible = visible;
        top.active = visible;
        pill.visible = visible;
        pill.active = visible;

        contentWidgets.add(top);
        contentWidgets.add(pill);
        this.addSelectableChild(top);
        this.addSelectableChild(pill);

        gridCol++;
        if (gridCol >= CARD_GRID_COLUMNS) {
            gridCol = 0;
            gridRowY += CARD_HEIGHT + CARD_GAP;
        }
    }

    /** Closes out the current grid (rounding up to a full row if the last one was left half-filled) and returns the logical y normal rows should resume at. */
    private int endGrid() {
        if (gridCol != 0) {
            gridRowY += CARD_HEIGHT + CARD_GAP;
            gridCol = 0;
        }
        return gridRowY;
    }

    /**
     * Adds the small "Hide/Show" + "delete" buttons to the right of each
     * row in the waypoints list. Kept as a separate pass from
     * {@link #drawWaypointsList} (which does the actual text) since that
     * method runs every frame from live data while these buttons only
     * need to be (re)built once per {@link #rebuildContent()} - but the
     * row math below must stay in lock-step with it, since a button here
     * is only in the right place if both use the same row height and the
     * same ordering/truncation rules.
     */
    private int buildWaypointActionButtons(int startY) {
        int rowY = startY;
        int deleteX = contentX + contentWidth - ACTION_DELETE_WIDTH;
        int hideX = deleteX - ACTION_GAP - ACTION_HIDE_WIDTH;
        int renameX = hideX - ACTION_GAP - ACTION_RENAME_WIDTH;

        rowY += WAYPOINT_ROW_HEIGHT; // "Last Death" header, no button

        Waypoint lastDeath = WaypointManager.INSTANCE.getLastDeath();
        if (lastDeath != null) {
            addDeleteButton(deleteX, rowY, lastDeath);
            rowY += WAYPOINT_ROW_HEIGHT;
        } else {
            rowY += WAYPOINT_ROW_HEIGHT; // "None recorded yet" line, no button
        }

        rowY += 4; // gap before the waypoints section
        rowY += WAYPOINT_ROW_HEIGHT; // "Waypoints (N)" header, no button

        List<Waypoint> waypoints = WaypointManager.INSTANCE.getUserWaypoints();
        if (waypoints.isEmpty()) {
            return rowY; // "None yet" line, no button
        }

        int maxShown = 12;
        int shown = 0;
        for (Waypoint wp : waypoints) {
            if (shown >= maxShown) {
                rowY += WAYPOINT_ROW_HEIGHT; // "+ N more" line, no button
                break;
            }
            if (wp == renamingWaypoint) {
                addRenameField(contentX, renameX, rowY, wp);
                addSaveButton(renameX, rowY, wp);
                addCancelButton(deleteX, rowY);
            } else {
                addRenameButton(renameX, rowY, wp);
                addHideButton(hideX, rowY, wp);
                addDeleteButton(deleteX, rowY, wp);
            }
            rowY += WAYPOINT_ROW_HEIGHT;
            shown++;
        }
        return rowY;
    }

    /**
     * Swaps a waypoint row into "edit name" mode: a text field takes over
     * the space the name/coords line normally occupies (see
     * {@link #drawWaypointsList}, which skips drawing that line while
     * this is active) so the field itself is always the thing rendered
     * there, never text underneath it.
     */
    private void addRenameField(int textX, int renameX, int y, Waypoint wp) {
        int actualY = y - scrollOffset;
        int fieldWidth = Math.max(20, renameX - ACTION_GAP - textX);
        renameField = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, textX, actualY,
                fieldWidth, ACTION_BTN_HEIGHT, null, net.minecraft.text.Text.literal("Waypoint name"));
        renameField.setMaxLength(48);
        renameField.setText(wp.name);
        boolean visible = isRowVisible(actualY, ACTION_BTN_HEIGHT);
        renameField.visible = visible;
        renameField.active = visible;
        this.addSelectableChild(renameField);
        if (visible) {
            renameField.setFocused(true);
        }
    }

    /** Confirms the in-progress rename: commits the field's (trimmed, non-blank) text and returns the row to its normal Rename/Hide/Delete state. */
    private void addSaveButton(int x, int y, Waypoint wp) {
        int actualY = y - scrollOffset;
        int width = ACTION_RENAME_WIDTH + ACTION_GAP + ACTION_HIDE_WIDTH;
        MiniButton button = new MiniButton(x, actualY, width, ACTION_BTN_HEIGHT, "Save", GlassTheme.ACCENT, () -> {
            confirmRename(wp);
        });
        applyRowVisibility(button, actualY, ACTION_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /** Discards the in-progress rename and returns the row to its normal Rename/Hide/Delete state. */
    private void addCancelButton(int x, int y) {
        int actualY = y - scrollOffset;
        MiniButton button = new MiniButton(x, actualY, ACTION_DELETE_WIDTH, ACTION_BTN_HEIGHT, "Cancel", GlassTheme.TEXT_DIM, () -> {
            renamingWaypoint = null;
            rebuildContent();
        });
        applyRowVisibility(button, actualY, ACTION_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /** Commits whatever is currently in {@link #renameField} to the given waypoint (unless it's blank, which is treated as a no-op rather than clearing the name), then exits rename mode. */
    private void confirmRename(Waypoint wp) {
        String newName = renameField != null ? renameField.getText().trim() : "";
        if (!newName.isEmpty()) {
            WaypointManager.INSTANCE.renameWaypoint(wp, newName);
        }
        renamingWaypoint = null;
        rebuildContent();
    }

    private void addRenameButton(int x, int y, Waypoint wp) {
        int actualY = y - scrollOffset;
        MiniButton button = new MiniButton(x, actualY, ACTION_RENAME_WIDTH, ACTION_BTN_HEIGHT,
                "Rename", GlassTheme.ACCENT_SECONDARY, () -> {
            renamingWaypoint = wp;
            waypointDeleteArmed.remove(wp);
            rebuildContent();
        });
        applyRowVisibility(button, actualY, ACTION_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    private void addHideButton(int x, int y, Waypoint wp) {
        int actualY = y - scrollOffset;
        MiniButton button = new MiniButton(x, actualY, ACTION_HIDE_WIDTH, ACTION_BTN_HEIGHT,
                wp.hidden ? "Show" : "Hide", GlassTheme.ACCENT_SECONDARY, () -> {
            WaypointManager.INSTANCE.setHidden(wp, !wp.hidden);
            rebuildContent();
        });
        applyRowVisibility(button, actualY, ACTION_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    private void addDeleteButton(int x, int y, Waypoint wp) {
        int actualY = y - scrollOffset;
        boolean armed = waypointDeleteArmed.contains(wp);
        MiniButton button = new MiniButton(x, actualY, ACTION_DELETE_WIDTH, ACTION_BTN_HEIGHT,
                armed ? "Sure?" : "x", GlassTheme.OFF_COLOR, () -> {
            if (armed) {
                WaypointManager.INSTANCE.removeWaypoint(wp);
                waypointDeleteArmed.remove(wp);
            } else {
                waypointDeleteArmed.add(wp);
            }
            rebuildContent();
        });
        applyRowVisibility(button, actualY, ACTION_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /**
     * Builds the Profiles tab: a persistent "New profile name" field +
     * Save button up top (see {@link #newProfileDraft}), then the list of
     * existing profiles below it (see {@link #buildProfileActionButtons}
     * for their Load/Rename buttons and {@link #drawProfilesList} for the
     * name text itself, drawn directly in render() the same way the
     * Waypoints tab splits its buttons from its text).
     */
    private int buildProfilesSection(int y) {
        int actualY = y - scrollOffset;
        int fieldWidth = Math.max(20, contentWidth - PROFILE_SAVE_BTN_WIDTH - ACTION_GAP);

        newProfileField = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, contentX, actualY,
                fieldWidth, ROW_HEIGHT, null, net.minecraft.text.Text.literal("New profile name"));
        newProfileField.setMaxLength(48);
        newProfileField.setText(newProfileDraft);
        newProfileField.setPlaceholder(net.minecraft.text.Text.literal("New profile name..."));
        newProfileField.setChangedListener(text -> newProfileDraft = text);
        boolean fieldVisible = isRowVisible(actualY, ROW_HEIGHT);
        newProfileField.visible = fieldVisible;
        newProfileField.active = fieldVisible;
        this.addSelectableChild(newProfileField);

        GlassButton saveButton = new GlassButton(contentX + fieldWidth + ACTION_GAP, actualY,
                PROFILE_SAVE_BTN_WIDTH, ROW_HEIGHT, "Save", () -> {
            String name = newProfileDraft.trim();
            if (name.isEmpty()) {
                profileMessage = "Enter a name first";
            } else {
                // Persist the live config to the main file too, so Save
                // here and the settings a fresh launch reads back are
                // never out of sync with each other.
                ModConfig.INSTANCE.save();
                ProfileManager.save(name);
                newProfileDraft = "";
                profileMessage = "Saved \"" + name + "\"";
            }
            rebuildContent();
        });
        applyRowVisibility(saveButton, actualY, ROW_HEIGHT);
        contentWidgets.add(saveButton);
        this.addSelectableChild(saveButton);

        y += ROW_HEIGHT + ROW_GAP;

        if (profileMessage != null) {
            // Reserve a line for the inline status message drawn in
            // render() (see drawProfilesList), so the profile list below
            // never overlaps it.
            profileMessageY = y;
            y += 12;
        }

        y += 4;
        profilesListY = y;
        y = buildProfileActionButtons(profilesListY);
        return y;
    }

    /**
     * Adds the "Load"/"Rename" buttons (or, for the one row being
     * renamed, the inline text field + "Save"/"Cancel") to the right of
     * each row in the profile list. Kept as a separate pass from
     * {@link #drawProfilesList} (which does the actual name text) the
     * same way {@link #buildWaypointActionButtons} splits from
     * {@link #drawWaypointsList} - but the row math below must stay in
     * lock-step with it, since a button here is only in the right place
     * if both use the same row height and ordering.
     */
    private int buildProfileActionButtons(int startY) {
        int rowY = startY;
        List<ProfileManager.ProfileEntry> profiles = ProfileManager.list();

        if (profiles.isEmpty()) {
            return rowY + PROFILE_ROW_HEIGHT; // "No profiles saved yet" line, no buttons
        }

        int deleteX = contentX + contentWidth - PROFILE_DELETE_WIDTH;
        int renameX = deleteX - ACTION_GAP - PROFILE_RENAME_WIDTH;
        int loadX = renameX - ACTION_GAP - PROFILE_LOAD_WIDTH;

        for (ProfileManager.ProfileEntry entry : profiles) {
            if (renamingProfile != null && renamingProfile.file().equals(entry.file())) {
                addProfileRenameField(contentX, loadX, rowY, entry);
                addProfileConfirmRenameButton(loadX, rowY, entry);
                addProfileCancelRenameButton(renameX, rowY);
            } else {
                addProfileLoadButton(loadX, rowY, entry);
                addProfileRenameButton(renameX, rowY, entry);
                addProfileDeleteButton(deleteX, rowY, entry);
            }
            rowY += PROFILE_ROW_HEIGHT;
        }
        return rowY;
    }

    private void addProfileLoadButton(int x, int y, ProfileManager.ProfileEntry entry) {
        int actualY = y - scrollOffset;
        MiniButton button = new MiniButton(x, actualY, PROFILE_LOAD_WIDTH, PROFILE_BTN_HEIGHT,
                "Load", GlassTheme.ACCENT, () -> {
            // Persist whatever's currently live before switching away from
            // it, so loading a different profile can't silently discard
            // unsaved edits to the one being replaced.
            ModConfig.INSTANCE.save();
            if (ProfileManager.load(entry)) {
                ModConfig.INSTANCE.save();
                // A profile swap can change far more settings at once than
                // any single row's own onChange callback would - re-run the
                // same live-apply hooks a bulk toggle uses so every module
                // picks up the new values immediately.
                applyBulkToggleSideEffects();
                profileMessage = "Loaded \"" + entry.name() + "\"";
            } else {
                profileMessage = "Couldn't load that profile";
            }
            rebuildContent();
        });
        applyRowVisibility(button, actualY, PROFILE_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    private void addProfileRenameButton(int x, int y, ProfileManager.ProfileEntry entry) {
        int actualY = y - scrollOffset;
        MiniButton button = new MiniButton(x, actualY, PROFILE_RENAME_WIDTH, PROFILE_BTN_HEIGHT,
                "Rename", GlassTheme.ACCENT_SECONDARY, () -> {
            renamingProfile = entry;
            profileDeleteArmed.remove(entry.file());
            profileMessage = null;
            rebuildContent();
        });
        applyRowVisibility(button, actualY, PROFILE_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /**
     * Two-step "x" -> "Sure?" confirm delete for a saved profile, mirroring
     * {@link #addDeleteButton} for waypoints. Deleting the profile currently
     * being renamed isn't reachable through the UI (Rename hides the
     * Delete button for that row), so no extra guard is needed there.
     */
    private void addProfileDeleteButton(int x, int y, ProfileManager.ProfileEntry entry) {
        int actualY = y - scrollOffset;
        boolean armed = profileDeleteArmed.contains(entry.file());
        MiniButton button = new MiniButton(x, actualY, PROFILE_DELETE_WIDTH, PROFILE_BTN_HEIGHT,
                armed ? "Sure?" : "x", GlassTheme.OFF_COLOR, () -> {
            if (armed) {
                ProfileManager.delete(entry);
                profileDeleteArmed.remove(entry.file());
                profileMessage = "Deleted \"" + entry.name() + "\"";
            } else {
                profileDeleteArmed.add(entry.file());
            }
            rebuildContent();
        });
        applyRowVisibility(button, actualY, PROFILE_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /**
     * Swaps a profile row into "edit name" mode: a text field takes over
     * the space its name normally occupies (see {@link #drawProfilesList},
     * which skips drawing that line while this is active) so the field
     * itself is always the thing rendered there, never text underneath it
     * - the same approach {@link #addRenameField} uses for waypoints.
     */
    private void addProfileRenameField(int textX, int loadX, int y, ProfileManager.ProfileEntry entry) {
        int actualY = y - scrollOffset;
        int fieldWidth = Math.max(20, loadX - ACTION_GAP - textX);
        profileRenameField = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, textX, actualY,
                fieldWidth, PROFILE_BTN_HEIGHT, null, net.minecraft.text.Text.literal("Profile name"));
        profileRenameField.setMaxLength(48);
        profileRenameField.setText(entry.name());
        boolean visible = isRowVisible(actualY, PROFILE_BTN_HEIGHT);
        profileRenameField.visible = visible;
        profileRenameField.active = visible;
        this.addSelectableChild(profileRenameField);
        if (visible) {
            profileRenameField.setFocused(true);
        }
    }

    /** Confirms the in-progress profile rename: commits the field's (trimmed, non-blank, non-duplicate) text and returns the row to its normal Load/Rename state. */
    private void addProfileConfirmRenameButton(int x, int y, ProfileManager.ProfileEntry entry) {
        int actualY = y - scrollOffset;
        MiniButton button = new MiniButton(x, actualY, PROFILE_LOAD_WIDTH, PROFILE_BTN_HEIGHT,
                "Save", GlassTheme.ACCENT, () -> confirmProfileRename(entry));
        applyRowVisibility(button, actualY, PROFILE_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /** Discards the in-progress profile rename and returns the row to its normal Load/Rename/Delete state. Spans the combined width of the Rename + Delete slots it's standing in for. */
    private void addProfileCancelRenameButton(int x, int y) {
        int actualY = y - scrollOffset;
        int width = PROFILE_RENAME_WIDTH + ACTION_GAP + PROFILE_DELETE_WIDTH;
        MiniButton button = new MiniButton(x, actualY, width, PROFILE_BTN_HEIGHT,
                "Cancel", GlassTheme.TEXT_DIM, () -> {
            renamingProfile = null;
            rebuildContent();
        });
        applyRowVisibility(button, actualY, PROFILE_BTN_HEIGHT);
        contentWidgets.add(button);
        this.addSelectableChild(button);
    }

    /** Commits whatever is currently in {@link #profileRenameField} to the given profile, unless it's blank or already used by a different profile (both surfaced via {@link #profileMessage} instead of silently discarding the edit), then exits rename mode either way. */
    private void confirmProfileRename(ProfileManager.ProfileEntry entry) {
        String newName = profileRenameField != null ? profileRenameField.getText().trim() : "";
        if (newName.isEmpty()) {
            profileMessage = "Enter a name first";
        } else if (ProfileManager.rename(entry, newName)) {
            profileMessage = "Renamed to \"" + newName + "\"";
            renamingProfile = null;
        } else {
            profileMessage = "That name is already used";
        }
        rebuildContent();
    }

    /**
     * Draws text at (x, y), truncating it with a trailing "..." if it's
     * wider than maxWidth. Category subtitles are free-form strings and
     * some (e.g. Keybinds') are long enough to run past the edge of the
     * content pane, so this keeps every subtitle on one line inside the
     * panel instead of spilling over its edge.
     */
    private void drawFittedText(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        String display = text;
        if (this.textRenderer.getWidth(display) > maxWidth) {
            String ellipsis = "...";
            int ellipsisWidth = this.textRenderer.getWidth(ellipsis);
            String trimmed = this.textRenderer.trimToWidth(display, Math.max(0, maxWidth - ellipsisWidth));
            display = trimmed + ellipsis;
        }
        context.drawTextWithShadow(this.textRenderer, display, x, y, color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Note: we intentionally don't call this.renderBackground(...) here.
        // As of 1.21.11, the game engine already blurs/dims the background
        // once per frame for any open screen when the "Menu Background
        // Blurriness" accessibility option is enabled. Calling
        // Screen.renderBackground() ourselves on top of that triggers a
        // second blur pass in the same frame, which throws
        // "Can only blur once per frame". Our own glass panel below is
        // already the visual background for this screen, so this is safe
        // to skip.
        //
        // That means the actual blur amount is whatever the player has
        // that accessibility slider set to - we don't control its radius
        // from here, and forcing our own additional blur pass isn't safe
        // (see above). What we *can* control is a light, flat dim over the
        // world behind the panel, so the menu still reads as "glass over
        // a darkened game" even for a player who has that setting off or
        // low. Kept deliberately subtle (about 20% black) rather than a
        // near-opaque wash, so it darkens without flattening the scene.
        context.fill(0, 0, this.width, this.height, 0x33000000);

        openProgress = Math.min(1f, openProgress + delta / 8f);
        // Ease-out cubic: fast start, gentle settle - the panel slides down
        // into place over its first ~8 frames instead of just popping in.
        float eased = 1f - (1f - openProgress) * (1f - openProgress) * (1f - openProgress);
        int slide = (int) ((1f - eased) * 10);

        int drawY = panelY - slide;

        // Everything below - our own chrome draws and the widget tree via
        // super.render() - is authored in virtual-panel pixels. A single
        // matrix scale here maps all of it onto the real screen at once,
        // so nothing else in this class needs to know uiScale exists.
        // mouseX/mouseY get the same treatment so widget hover state
        // (computed against these virtual-space bounds) lines up with
        // where things are actually drawn.
        boolean scaled = uiScale != 1f;
        if (scaled) {
            mouseX = (int) Math.round(toVirtualX(mouseX));
            mouseY = (int) Math.round(toVirtualY(mouseY));
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(uiScale, uiScale);
        }

        GlassTheme.menuPanel(context, panelX, drawY, PANEL_WIDTH, PANEL_HEIGHT);
        GlassTheme.sidebar(context, panelX, drawY, SIDEBAR_WIDTH, PANEL_HEIGHT);

        GlassTheme.chip(context, panelX + 14, drawY + 13, 7, GlassTheme.ACCENT);
        context.drawTextWithShadow(this.textRenderer, "CRAFTCLOUD", panelX + 26, drawY + 14, GlassTheme.ACCENT);
        context.drawTextWithShadow(this.textRenderer, "CLIENT", panelX + 26, drawY + 24, GlassTheme.TEXT_DIM);
        if (sidebarDividerY >= 0) {
            GlassTheme.divider(context, panelX + 10, sidebarDividerY, SIDEBAR_WIDTH - 20);
        }
        context.drawTextWithShadow(this.textRenderer, "v" + BuildInfo.VERSION, panelX + 14, drawY + PANEL_HEIGHT - 18, 0x556B7688);

        int headerX = contentX;
        GlassTheme.chip(context, headerX, drawY + 22, 7, current.accent);
        context.drawTextWithShadow(this.textRenderer, current.title, headerX + 12, drawY + 22, GlassTheme.TEXT_MAIN);
        drawFittedText(context, current.subtitle, headerX, drawY + 33, contentWidth, GlassTheme.TEXT_DIM);
        GlassTheme.accentLine(context, headerX, drawY + 46, contentWidth);

        if (current == Category.ABOUT) {
            drawAboutSection(context, contentX, contentY + 26 + ROW_HEIGHT + ROW_GAP, contentWidth);
        }
        if (current == Category.STATS) {
            int tableBottom = drawStatsTable(context, contentX, statsTableY, contentWidth);
            String streakLine = "Kill Streak - Current: " + KillStreakManager.INSTANCE.getCurrentStreak()
                    + "   Best: " + KillStreakManager.INSTANCE.getBestStreak();
            drawTextIfVisible(context, streakLine, contentX, tableBottom + 6, GlassTheme.TEXT_DIM, 10);
        }
        if (current == Category.WAYPOINTS) {
            drawWaypointsList(context, contentX, waypointsListY, contentWidth);
        }
        if (current == Category.PROFILES) {
            if (profileMessage != null) {
                drawTextIfVisible(context, profileMessage, contentX, profileMessageY - scrollOffset, GlassTheme.ACCENT_SECONDARY, 10);
            }
            drawProfilesList(context, contentX, profilesListY, contentWidth);
        }
        if (noResultsY >= 0) {
            String message = "No settings match \"" + searchQuery + "\"";
            int textY = (noResultsY - scrollOffset) + (ROW_HEIGHT - 8) / 2;
            drawTextIfVisible(context, message, contentX, textY, GlassTheme.TEXT_DIM, 8);
        }

        drawScrollbar(context, drawY);

        // Content rows/cards are rendered here, manually, inside a real
        // GL scissor clipped to the content viewport - not via the
        // default super.render() pass below. That's what lets a card
        // scroll smoothly half on/half off screen (properly cut off at
        // the pixel where the viewport ends) instead of the previous
        // all-or-nothing "hidden until fully in view" behavior, which
        // made the list feel like it was popping in blocks rather than
        // scrolling.
        renderContentWidgets(context, mouseX, mouseY, delta);

        // The widgets below are drawn at their fixed target position - only
        // the panel chrome above actually slides. That's enough to sell the
        // "open" motion without fighting per-row hover/click state. These
        // are the non-scrolling widgets only (sidebar tabs, close/Discord
        // buttons) - content rows were just rendered above, separately,
        // since they're registered with addSelectableChild rather than
        // addDrawableChild specifically so this default pass skips them.
        super.render(context, mouseX, mouseY, delta);

        if (scaled) {
            context.getMatrices().popMatrix();
        }
    }

    /**
     * Renders every scrollable content widget (search field, toggle rows,
     * mod cards, etc.) clipped to the content viewport with a real GL
     * scissor, so a row half-scrolled past the top or bottom edge is cut
     * off cleanly at that pixel rather than either bleeding past the
     * panel edge or popping away entirely. Content widgets are registered
     * with addSelectableChild (not addDrawableChild) specifically so the
     * default super.render() pass doesn't also draw them unclipped.
     */
    private void renderContentWidgets(DrawContext context, int mouseX, int mouseY, float delta) {
        // A couple px of slack on the sides so left/right edge highlights
        // and the scrollbar itself aren't clipped, matching how the
        // viewport was visually sized before this scissor existed.
        int scissorLeft = contentX - 2;
        int scissorRight = contentX + contentWidth + 8;
        context.enableScissor(scissorLeft, viewportTop, scissorRight, viewportBottom);
        for (ButtonWidget w : contentWidgets) {
            if (w.visible) {
                w.render(context, mouseX, mouseY, delta);
            }
        }
        if (searchField != null && searchField.visible) {
            searchField.render(context, mouseX, mouseY, delta);
        }
        if (renameField != null && renameField.visible) {
            renameField.render(context, mouseX, mouseY, delta);
        }
        if (newProfileField != null && newProfileField.visible) {
            newProfileField.render(context, mouseX, mouseY, delta);
        }
        if (profileRenameField != null && profileRenameField.visible) {
            profileRenameField.render(context, mouseX, mouseY, delta);
        }
        context.disableScissor();
    }

    /**
     * Thin vertical scrollbar track along the right edge of the content
     * pane, only drawn once a tab actually overflows (maxScroll > 0) - a
     * short tab like PvP or Performance never shows one. The thumb's
     * height/position track scrollOffset the same way a normal OS
     * scrollbar would, purely as a visual cue; scrolling itself is done
     * with the mouse wheel via {@link #mouseScrolled}.
     */
    private void drawScrollbar(DrawContext context, int drawY) {
        if (maxScroll <= 0) return;

        int trackX = contentX + contentWidth + 4;
        int trackTop = viewportTop;
        int trackBottom = viewportBottom;
        int trackHeight = trackBottom - trackTop;

        context.fill(trackX, trackTop, trackX + 3, trackBottom, 0x33FFFFFF);

        int totalContentHeight = trackHeight + maxScroll;
        int thumbHeight = Math.max(16, trackHeight * trackHeight / totalContentHeight);
        int thumbY = trackTop + (int) ((long) scrollOffset * (trackHeight - thumbHeight) / Math.max(1, maxScroll));
        context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, GlassTheme.ACCENT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double vMouseX = toVirtualX(mouseX);
        double vMouseY = toVirtualY(mouseY);
        if (maxScroll > 0 && vMouseX >= contentX && vMouseX <= contentX + contentWidth
                && vMouseY >= viewportTop && vMouseY <= viewportBottom) {
            int newOffset = scrollOffset - (int) Math.round(verticalAmount * SCROLL_SPEED);
            newOffset = Math.max(0, Math.min(maxScroll, newOffset));
            if (newOffset != scrollOffset) {
                scrollOffset = newOffset;
                rebuildContent();
            }
            return true;
        }
        return super.mouseScrolled(vMouseX, vMouseY, horizontalAmount, verticalAmount);
    }

    // --- Auto-scale: mouse coordinate translation ---
    // Every one of these takes a real-screen-space input event and hands
    // vanilla's own click/drag/hover handling the equivalent event in
    // virtual-panel coordinates instead, so hit-testing against widget
    // bounds (all authored in virtual space, see init()) stays correct no
    // matter how much uiScale has shrunk the panel to fit the window.
    // This build's ParentElement uses the newer Click-record input API
    // (same one GlassKeybindRow/HudLayoutScreen already override), not
    // the older raw mouseX/mouseY/button triples.
    //
    // Click's non-position field turned out to be a MouseInput object,
    // not a plain int, and we have no source for that class to build one
    // by hand - so instead of guessing its constructor, this copies the
    // record generically via reflection: read every component off the
    // original Click, swap in the virtual x/y, and re-invoke Click's own
    // canonical constructor with the rest unchanged. That works no matter
    // what Click's other fields turn out to be.

    private net.minecraft.client.gui.Click toVirtualClick(net.minecraft.client.gui.Click click) {
        double vx = toVirtualX(click.x());
        double vy = toVirtualY(click.y());
        try {
            RecordComponent[] components = net.minecraft.client.gui.Click.class.getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                String name = components[i].getName();
                if (name.equals("x") && types[i] == double.class) {
                    args[i] = vx;
                } else if (name.equals("y") && types[i] == double.class) {
                    args[i] = vy;
                } else {
                    args[i] = components[i].getAccessor().invoke(click);
                }
            }
            Constructor<net.minecraft.client.gui.Click> ctor =
                    net.minecraft.client.gui.Click.class.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            // Should never happen - Click is a record and this reads its
            // own declared shape back at it - but if some future MC
            // version changes that, fail soft (unscaled click) instead of
            // crashing the whole menu.
            return click;
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        return super.mouseClicked(toVirtualClick(click), doubled);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        return super.mouseReleased(toVirtualClick(click));
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double offsetX, double offsetY) {
        return super.mouseDragged(toVirtualClick(click), offsetX / uiScale, offsetY / uiScale);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(toVirtualX(mouseX), toVirtualY(mouseY));
    }

    /**
     * A simple category-by-period grid: rows are hits/crystals/anchors/
     * obsidian/kills, columns are today/week/month/year/all-time. Drawn
     * directly with text rather than as widgets since it's just live
     * read-only numbers, refreshed every frame from {@link StatsManager}.
     */
    private int drawStatsTable(DrawContext context, int x, int y, int width) {
        StatCategory[] categories = StatCategory.values();
        StatPeriod[] periods = StatPeriod.values();

        int labelColWidth = 62;
        int colWidth = (width - labelColWidth) / periods.length;
        int rowHeight = 16;
        int headerY = y - scrollOffset;

        if (isRowVisible(headerY, rowHeight)) {
            for (int i = 0; i < periods.length; i++) {
                int colX = x + labelColWidth + i * colWidth;
                String header = periods[i].label;
                int tw = this.textRenderer.getWidth(header);
                context.drawTextWithShadow(this.textRenderer, header, colX + (colWidth - tw) / 2, headerY, GlassTheme.TEXT_DIM);
            }
        }

        int rowY = headerY + rowHeight;
        for (StatCategory category : categories) {
            if (isRowVisible(rowY, rowHeight)) {
                context.drawTextWithShadow(this.textRenderer, category.label, x, rowY + 2, GlassTheme.TEXT_MAIN);
                for (int i = 0; i < periods.length; i++) {
                    int colX = x + labelColWidth + i * colWidth;
                    int count = StatsManager.INSTANCE.getCount(category, periods[i]);
                    String text = String.valueOf(count);
                    int tw = this.textRenderer.getWidth(text);
                    context.drawTextWithShadow(this.textRenderer, text, colX + (colWidth - tw) / 2, rowY + 2, GlassTheme.ACCENT);
                }
            }
            rowY += rowHeight;
        }
        return rowY;
    }

    /**
     * Static info panel for the About tab: mod version + build (see
     * {@link BuildInfo}), the Minecraft version this build targets, and
     * basic credits pulled from {@code fabric.mod.json}. Read-only, so
     * it's drawn directly every frame rather than built as widgets - the
     * same approach used by {@link #drawStatsTable} above.
     */
    private void drawAboutSection(DrawContext context, int x, int y, int width) {
        int rowHeight = 14;
        int labelWidth = 70;
        int rowY = y - scrollOffset;

        rowY = drawAboutRow(context, x, rowY, labelWidth, width, "Version", BuildInfo.VERSION, rowHeight);
        rowY = drawAboutRow(context, x, rowY, labelWidth, width, "Build", BuildInfo.BUILD, rowHeight);
        rowY = drawAboutRow(context, x, rowY, labelWidth, width, "Minecraft", "1.21.11", rowHeight);
        rowY = drawAboutRow(context, x, rowY, labelWidth, width, "License", "Apache-2.0", rowHeight);
        rowY += 8;

        if (isRowVisible(rowY, 1)) {
            GlassTheme.divider(context, x, rowY, width);
        }
        rowY += 10;

        rowY = drawTextIfVisible(context, "Credits", x, rowY, GlassTheme.TEXT_DIM, rowHeight);
        rowY = drawTextIfVisible(context, "CraftcloudNet.com", x, rowY, GlassTheme.TEXT_MAIN, rowHeight - 3);
        drawTextIfVisible(context, "Prach (owner)", x, rowY, GlassTheme.TEXT_MAIN, rowHeight);
    }

    private int drawAboutRow(DrawContext context, int x, int y, int labelWidth, int width, String label, String value, int rowHeight) {
        if (isRowVisible(y, rowHeight)) {
            context.drawTextWithShadow(this.textRenderer, label, x, y, GlassTheme.TEXT_DIM);
            drawFittedText(context, value, x + labelWidth, y, width - labelWidth, GlassTheme.TEXT_MAIN);
        }
        return y + rowHeight;
    }

    /** Draws a single line of text only if it falls inside the scroll viewport, advancing by rowHeight either way. */
    private int drawTextIfVisible(DrawContext context, String text, int x, int y, int color, int rowHeight) {
        if (isRowVisible(y, rowHeight)) {
            context.drawTextWithShadow(this.textRenderer, text, x, y, color);
        }
        return y + rowHeight;
    }

    /**
     * Read-only name list for the Profiles tab, mirroring how
     * {@link #drawWaypointsList} splits the text half of each row from
     * the buttons {@link #buildProfileActionButtons} adds beside it.
     */
    private void drawProfilesList(DrawContext context, int x, int y, int width) {
        final int rowHeight = PROFILE_ROW_HEIGHT;
        int rowY = y - scrollOffset;

        List<ProfileManager.ProfileEntry> profiles = ProfileManager.list();
        if (profiles.isEmpty()) {
            drawTextIfVisible(context, "No profiles saved yet", x, rowY, GlassTheme.TEXT_DIM, rowHeight);
            return;
        }

        // Text must never run under the Load/Rename/Delete buttons pinned
        // to the right edge of the row (see buildProfileActionButtons) - a
        // long profile name is truncated with "..." instead.
        int deleteX = contentX + contentWidth - PROFILE_DELETE_WIDTH;
        int renameX = deleteX - ACTION_GAP - PROFILE_RENAME_WIDTH;
        int loadX = renameX - ACTION_GAP - PROFILE_LOAD_WIDTH;
        int nameMaxWidth = Math.max(20, loadX - ACTION_GAP - x);

        for (ProfileManager.ProfileEntry entry : profiles) {
            if (renamingProfile != null && renamingProfile.file().equals(entry.file())) {
                // The rename text field (a real widget, added in
                // buildProfileActionButtons) is drawn over this row
                // instead - just reserve its height here.
                rowY += rowHeight;
            } else {
                if (isRowVisible(rowY, rowHeight)) {
                    drawFittedText(context, entry.name(), x, rowY, nameMaxWidth, GlassTheme.TEXT_MAIN);
                }
                rowY += rowHeight;
            }
        }
    }

    /**
     * Read-only list of saved locations: the most recent death marker up
     * top (if any), then up to a handful of the player's own waypoints.
     * Deliberately not a scrollable/interactive list - "Clear Waypoints"
     * / "Clear Death History" above cover the only bulk actions this
     * needs, and a fixed-size text dump keeps this consistent with how
     * the Stats tab renders its own live numbers directly rather than as
     * widgets.
     */
    private void drawWaypointsList(DrawContext context, int x, int y, int width) {
        final int rowHeight = WAYPOINT_ROW_HEIGHT;
        int rowY = y - scrollOffset;

        // Text must never run under the action buttons pinned to the right
        // edge of the row (see buildWaypointActionButtons) - a long
        // waypoint name is truncated with "..." instead. The "Last Death"
        // row only ever has a delete button, but user waypoint rows also
        // reserve room for Rename + Hide, so they get a narrower budget.
        int deleteX = contentX + contentWidth - ACTION_DELETE_WIDTH;
        int hideX = deleteX - ACTION_GAP - ACTION_HIDE_WIDTH;
        int renameX = hideX - ACTION_GAP - ACTION_RENAME_WIDTH;
        int deathTextMaxWidth = Math.max(20, deleteX - ACTION_GAP - x);
        int waypointTextMaxWidth = Math.max(20, renameX - ACTION_GAP - x);

        Waypoint lastDeath = WaypointManager.INSTANCE.getLastDeath();
        rowY = drawTextIfVisible(context, "Last Death", x, rowY, GlassTheme.TEXT_DIM, rowHeight);
        if (lastDeath == null) {
            rowY = drawTextIfVisible(context, "None recorded yet", x, rowY, GlassTheme.TEXT_MAIN, rowHeight);
        } else {
            rowY = drawWaypointLine(context, lastDeath, x, rowY, deathTextMaxWidth);
        }

        rowY += 4;
        List<Waypoint> waypoints = WaypointManager.INSTANCE.getUserWaypoints();
        rowY = drawTextIfVisible(context, "Waypoints (" + waypoints.size() + ")", x, rowY, GlassTheme.TEXT_DIM, rowHeight);

        if (waypoints.isEmpty()) {
            drawTextIfVisible(context, "None yet - press your Add Waypoint key", x, rowY, GlassTheme.TEXT_MAIN, rowHeight);
        } else {
            int maxShown = 12;
            int shown = 0;
            for (Waypoint wp : waypoints) {
                if (shown >= maxShown) {
                    int remaining = waypoints.size() - maxShown;
                    drawTextIfVisible(context, "+ " + remaining + " more", x, rowY, GlassTheme.TEXT_DIM, rowHeight);
                    break;
                }
                if (wp == renamingWaypoint) {
                    // The rename text field (a real widget, added in
                    // buildWaypointActionButtons) is drawn over this row
                    // instead - just reserve its height here.
                    rowY += rowHeight;
                } else {
                    rowY = drawWaypointLine(context, wp, x, rowY, waypointTextMaxWidth);
                }
                shown++;
            }
        }
    }

    private int drawWaypointLine(DrawContext context, Waypoint wp, int x, int y, int maxWidth) {
        String coords = (int) wp.x + ", " + (int) wp.y + ", " + (int) wp.z;
        String dim = DimensionUtil.label(wp.dimension);
        String distance = distanceLabelFor(wp);

        String line = wp.name + "  " + coords + "  " + dim + (distance.isEmpty() ? "" : "  " + distance) + (wp.hidden ? "  (hidden)" : "");
        int color = wp.death ? 0xFFFF9E9E : (wp.hidden ? GlassTheme.TEXT_DIM : GlassTheme.TEXT_MAIN);
        if (isRowVisible(y, WAYPOINT_ROW_HEIGHT)) {
            drawFittedText(context, line, x, y, maxWidth, color);
        }
        return y + WAYPOINT_ROW_HEIGHT;
    }

    /** Distance to a waypoint from the player's current position, only when standing in the same dimension it was saved in. */
    private String distanceLabelFor(Waypoint wp) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "";
        if (!DimensionUtil.currentDimensionKey(client).equals(wp.dimension)) return "";

        double dx = client.player.getX() - wp.x;
        double dy = client.player.getY() - wp.y;
        double dz = client.player.getZ() - wp.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.round(distance) + "m";
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        // While a waypoint's name is being edited, Enter confirms and
        // Escape cancels rather than falling through to Screen's normal
        // Escape-closes-the-menu handling (which would otherwise discard
        // the in-progress edit) or the keybind-listening loop below.
        if (renamingWaypoint != null && renameField != null) {
            int keyCode = input.key();
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                renamingWaypoint = null;
                rebuildContent();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                confirmRename(renamingWaypoint);
                return true;
            }
        }
        if (renamingProfile != null && profileRenameField != null) {
            int keyCode = input.key();
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                renamingProfile = null;
                rebuildContent();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                confirmProfileRename(renamingProfile);
                return true;
            }
        }
        for (ButtonWidget w : contentWidgets) {
            if (w instanceof GlassKeybindRow row && row.isListening()) {
                row.applyKey(input.key());
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        ModConfig.INSTANCE.save();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Small reusable button used for the per-waypoint "Hide/Show" and delete actions - a compact version of {@link GlassButton} sized to sit at the end of one list row instead of spanning the full content width. */
    private static class MiniButton extends ButtonWidget {
        private final String label;
        private final int accent;

        MiniButton(int x, int y, int width, int height, String label, int accent, Runnable action) {
            super(x, y, width, height, net.minecraft.text.Text.literal(label), b -> action.run(), DEFAULT_NARRATION_SUPPLIER);
            this.label = label;
            this.accent = accent;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean hovered = this.isHovered();
            GlassTheme.card(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), hovered);

            var tr = MinecraftClient.getInstance().textRenderer;
            int tw = tr.getWidth(label);
            int textX = this.getX() + (this.getWidth() - tw) / 2;
            int textY = this.getY() + (this.getHeight() - 8) / 2;
            int color = hovered ? accent : GlassTheme.TEXT_DIM;
            context.drawTextWithShadow(tr, label, textX, textY, color);
        }
    }

    /**
     * Quiet icon-only button that opens the Discord invite. Drawn at a
     * fixed small size with no background card of its own (unlike
     * {@link GlassButton}/{@link MiniButton}) so it reads as a footer
     * icon, not another setting row - it only brightens slightly on
     * hover, and the tooltip is the only thing that appears without a
     * click.
     */
    private static class DiscordButton extends ButtonWidget {
        DiscordButton(int x, int y, Runnable action) {
            super(x, y, DISCORD_ICON_DRAW_SIZE, DISCORD_ICON_DRAW_SIZE, net.minecraft.text.Text.literal("Discord"), b -> action.run(), DEFAULT_NARRATION_SUPPLIER);
            this.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Join our Discord")));
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            // Same 12-arg drawTexture overload GameMenuLogoMixin uses for
            // the menu_logo texture (see its NOTE comment on why the
            // narrower "width/height doubles as sample region" overload
            // isn't used here): sampled region stays the icon's full
            // native size while independently scaling to the small drawn
            // size on screen.
            context.drawTexture(RenderPipelines.GUI_TEXTURED, DISCORD_ICON, this.getX(), this.getY(), 0, 0,
                    DISCORD_ICON_DRAW_SIZE, DISCORD_ICON_DRAW_SIZE, DISCORD_ICON_TEX_SIZE, DISCORD_ICON_TEX_SIZE,
                    DISCORD_ICON_TEX_SIZE, DISCORD_ICON_TEX_SIZE);

            // Subtle hover feedback (a soft ring) instead of any tint
            // overload, since drawTexture here has no color parameter in
            // this codebase's confirmed-working call shape.
            if (this.isHovered()) {
                int x = this.getX(), y = this.getY(), s = DISCORD_ICON_DRAW_SIZE;
                context.fill(x - 1, y - 1, x + s + 1, y, 0x33FFFFFF);
                context.fill(x - 1, y + s, x + s + 1, y + s + 1, 0x33FFFFFF);
                context.fill(x - 1, y, x, y + s, 0x33FFFFFF);
                context.fill(x + s, y, x + s + 1, y + s, 0x33FFFFFF);
            }
        }
    }

    /** Small circular-feeling "x" button in the corner, used to close the menu. */
    private static class CloseButton extends ButtonWidget {
        CloseButton(int x, int y, Runnable action) {
            super(x, y, 18, 18, net.minecraft.text.Text.literal("x"), button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean hovered = this.isHovered();
            int fill = hovered ? 0xAAE05555 : 0x552A2E38;
            GlassTheme.fillRounded(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), fill, this.getWidth() / 2);

            int color = hovered ? GlassTheme.TEXT_MAIN : GlassTheme.TEXT_DIM;
            var tr = MinecraftClient.getInstance().textRenderer;
            int tw = tr.getWidth("x");
            context.drawTextWithShadow(tr, "x", this.getX() + (this.getWidth() - tw) / 2, this.getY() + 5, color);
        }
    }
}