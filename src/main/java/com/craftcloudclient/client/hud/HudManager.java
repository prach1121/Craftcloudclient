package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.gui.GlassTheme;
import com.craftcloudclient.client.stats.StatCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HudManager {

    private final List<HudModule> modules = new ArrayList<>();
    private final FpsModule fpsModule = new FpsModule();
    private final TotemCounterModule totemCounterModule = new TotemCounterModule();
    private final WaypointHudModule waypointHudModule = new WaypointHudModule();
    private final WaypointNavigatorModule waypointNavigatorModule = new WaypointNavigatorModule();
    private final ServerBannerModule serverBannerModule = new ServerBannerModule();

    private static final int MARGIN = 6;
    private static final int LINE_HEIGHT = 12;
    private static final int ICON_SIZE = 16;
    private static final int ICON_ROW_HEIGHT = 18;
    private static final int ICON_TEXT_GAP = 3;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int PANEL_GAP = 3;

    // Totem widget: single hotbar-style slot anchored above the hotbar.
    private static final int TOTEM_SLOT_SIZE = 20;

    public HudManager() {
        modules.add(fpsModule);
        modules.add(new CoordsModule());
        modules.add(new TpsModule());
        modules.add(new PingModule());
        modules.add(new CpsModule());
        modules.add(new ArmorDurabilityModule(EquipmentSlot.HEAD, "Helmet"));
        modules.add(new ArmorDurabilityModule(EquipmentSlot.CHEST, "Chestplate"));
        modules.add(new ArmorDurabilityModule(EquipmentSlot.LEGS, "Leggings"));
        modules.add(new ArmorDurabilityModule(EquipmentSlot.FEET, "Boots"));
        modules.add(new ArmorDurabilityModule(EquipmentSlot.MAINHAND, "Weapon"));
        modules.add(new StatModule(StatCategory.HITS));
        modules.add(new StatModule(StatCategory.END_CRYSTAL));
        modules.add(new StatModule(StatCategory.RESPAWN_ANCHOR));
        modules.add(new StatModule(StatCategory.OBSIDIAN));
        modules.add(new StatModule(StatCategory.KILLS));
        modules.add(new PingGraphModule());
        modules.add(new StatusGraphModule());
        modules.add(new KillStreakModule());
        modules.add(new KeystrokesModule());
        modules.add(new TimeUntilDayModule());
        modules.add(new PlaytimeModule());
        modules.add(new LightLevelModule());
        modules.add(new ClockModule());
        modules.add(new BiomeModule());
        modules.add(new HostileMobModule());
        modules.add(new WorldBorderModule());
        modules.add(new DistanceTraveledModule());
        modules.add(new XpGainedModule());
        modules.add(new CombatTimerModule());
    }

    /** Called once per client tick to refresh values. */
    public void tick() {
        for (HudModule module : modules) {
            module.tick();
        }
        totemCounterModule.tick();
    }

    /** Call once per rendered frame (separate from tick) so FPS reflects real frame rate. */
    public void onFrame() {
        fpsModule.onFrame();
    }

    /**
     * A single rendered HUD panel: the on-screen rectangle for one corner,
     * plus the list of modules currently grouped into it. Shared between
     * the normal render pass and {@link com.craftcloudclient.client.gui.HudLayoutScreen},
     * which needs the exact same rectangles to hit-test right-clicks.
     */
    public static class GroupBox implements DraggablePanel {
        public final HudPosition position;
        public final int x, y, width, height;
        public final List<HudModule> modules;

        GroupBox(HudPosition position, int x, int y, int width, int height, List<HudModule> modules) {
            this.position = position;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.modules = modules;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        @Override
        public HudRect bounds() {
            return new HudRect(x, y, width, height);
        }

        @Override
        public void savePosition(int x, int y) {
            saveGroupPosition(this, x, y);
        }
    }

    /**
     * Computes where every visible HUD panel currently sits, without
     * drawing anything. Uses the client window's scaled size directly
     * (rather than a DrawContext) so it can also be called from mouse
     * click handling, which happens outside of a render pass.
     */
    public List<GroupBox> collectGroupBoxes() {
        List<GroupBox> boxes = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return boxes;

        Map<HudPosition, List<HudModule>> grouped = new EnumMap<>(HudPosition.class);
        for (HudPosition pos : HudPosition.values()) {
            grouped.put(pos, new ArrayList<>());
        }
        for (HudModule module : modules) {
            if (module.isEnabled()) {
                grouped.get(module.getPosition()).add(module);
            }
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        for (HudPosition pos : HudPosition.values()) {
            List<HudModule> list = grouped.get(pos);
            if (list.isEmpty()) continue;

            int contentWidth = 0;
            int panelHeight = PADDING_Y * 2;
            for (HudModule module : list) {
                if (module.getGraphWidth() > 0 && module.getGraphHeight() > 0) {
                    contentWidth = Math.max(contentWidth, module.getGraphWidth());
                    panelHeight += module.getGraphHeight();
                    continue;
                }
                boolean hasIcon = !module.getIcon().isEmpty();
                int textWidth = client.textRenderer.getWidth(module.getText());
                int rowWidth = hasIcon ? ICON_SIZE + ICON_TEXT_GAP + textWidth : textWidth;
                contentWidth = Math.max(contentWidth, rowWidth);
                panelHeight += hasIcon ? ICON_ROW_HEIGHT : LINE_HEIGHT;
            }
            int panelWidth = contentWidth + PADDING_X * 2;

            int x = resolvePanelX(pos, panelWidth, screenWidth);
            int y = resolvePanelY(pos, panelHeight, screenHeight);

            boxes.add(new GroupBox(pos, x, y, panelWidth, panelHeight, list));
        }
        return boxes;
    }

    /**
     * Re-points every module in {@code box} at a new corner in one go, so
     * right-clicking a panel in the HUD layout editor moves the whole
     * panel rather than just one line inside it. Saves immediately.
     */
    public static void moveGroup(GroupBox box, HudPosition newPosition) {
        ModConfig cfg = ModConfig.INSTANCE;
        for (HudModule module : box.modules) {
            String id = module.getId();
            if (id.equals("fps")) cfg.fpsPosition = newPosition;
            else if (id.equals("tps")) cfg.tpsPosition = newPosition;
            else if (id.equals("ping")) cfg.pingPosition = newPosition;
            else if (id.equals("cps")) cfg.cpsPosition = newPosition;
            else if (id.startsWith("armor_")) cfg.armorStatusPosition = newPosition;
            else if (id.startsWith("stat_")) cfg.statsPosition = newPosition;
            else if (id.equals("ping_graph")) cfg.pingGraphPosition = newPosition;
            else if (id.equals("status_graph")) cfg.statusGraphPosition = newPosition;
            else if (id.equals("kill_streak")) cfg.killStreakPosition = newPosition;
            else if (id.equals("keystrokes")) cfg.keystrokesPosition = newPosition;
            else if (id.equals("time_until_day")) cfg.timeUntilDayPosition = newPosition;
            else if (id.equals("playtime")) cfg.playtimePosition = newPosition;
            else if (id.equals("lightlevel")) cfg.lightLevelPosition = newPosition;
            else if (id.equals("clock")) cfg.clockPosition = newPosition;
            else if (id.equals("biome")) cfg.biomePosition = newPosition;
            else if (id.equals("hostile_mobs")) cfg.hostileMobsPosition = newPosition;
            else if (id.equals("world_border")) cfg.worldBorderPosition = newPosition;
            else if (id.equals("distance_traveled")) cfg.distanceTraveledPosition = newPosition;
            else if (id.equals("xp_gained")) cfg.xpGainedPosition = newPosition;
            else if (id.equals("combat_timer")) cfg.combatTimerPosition = newPosition;
        }
        cfg.setPanelPosition(newPosition, box.x, box.y);
        cfg.save();
    }

    public static void saveGroupPosition(GroupBox box, int x, int y) {
        ModConfig cfg = ModConfig.INSTANCE;
        cfg.setPanelPosition(box.position, x, y);
        cfg.save();
    }

    /**
     * Every panel the HUD layout editor can offer to drag this frame: the
     * corner-grouped module panels from {@link #collectGroupBoxes()}, plus
     * the server banner and waypoint tracker cards - which used to be
     * fixed, always-centered elements with nothing to hook the editor
     * into - whenever they currently have something to show.
     */
    public List<DraggablePanel> collectDraggables() {
        List<DraggablePanel> list = new ArrayList<>(collectGroupBoxes());

        HudRect bannerBounds = serverBannerModule.currentBounds();
        if (bannerBounds != null) {
            list.add(new DraggablePanel() {
                @Override
                public HudRect bounds() {
                    return bannerBounds;
                }

                @Override
                public void savePosition(int x, int y) {
                    serverBannerModule.savePosition(x, y);
                }

                @Override
                public boolean supportsCornerSnap() {
                    return false;
                }
            });
        }

        HudRect waypointBounds = waypointHudModule.currentBounds();
        if (waypointBounds != null) {
            list.add(new DraggablePanel() {
                @Override
                public HudRect bounds() {
                    return waypointBounds;
                }

                @Override
                public void savePosition(int x, int y) {
                    waypointHudModule.savePosition(x, y);
                }

                @Override
                public boolean supportsCornerSnap() {
                    return false;
                }
            });
        }

        HudRect navigatorBounds = waypointNavigatorModule.currentBounds();
        if (navigatorBounds != null) {
            list.add(new DraggablePanel() {
                @Override
                public HudRect bounds() {
                    return navigatorBounds;
                }

                @Override
                public void savePosition(int x, int y) {
                    waypointNavigatorModule.savePosition(x, y);
                }

                @Override
                public boolean supportsCornerSnap() {
                    return false;
                }
            });
        }

        return list;
    }

    private int resolvePanelX(HudPosition position, int panelWidth, int screenWidth) {
        int fallbackX = (position == HudPosition.TOP_RIGHT || position == HudPosition.BOTTOM_RIGHT)
                ? screenWidth - MARGIN - panelWidth
                : MARGIN;
        int x = ModConfig.INSTANCE.getPanelX(position, fallbackX);
        int maxX = Math.max(MARGIN, screenWidth - panelWidth - MARGIN);
        return Math.max(MARGIN, Math.min(x, maxX));
    }

    private int resolvePanelY(HudPosition position, int panelHeight, int screenHeight) {
        int fallbackY = (position == HudPosition.BOTTOM_LEFT || position == HudPosition.BOTTOM_RIGHT)
                ? screenHeight - MARGIN - panelHeight
                : MARGIN;
        int y = ModConfig.INSTANCE.getPanelY(position, fallbackY);
        int maxY = Math.max(MARGIN, screenHeight - panelHeight - MARGIN);
        return Math.max(MARGIN, Math.min(y, maxY));
    }

    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;

        if (ModConfig.INSTANCE.hudModulesEnabled) {
            for (GroupBox box : collectGroupBoxes()) {
                drawGroup(context, box);
            }

            if (totemCounterModule.isEnabled()) {
                renderTotemCounter(context, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            }
        }

        waypointHudModule.render(context);
        waypointNavigatorModule.render(context);
        serverBannerModule.render(context);
    }

    private void drawGroup(DrawContext context, GroupBox box) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (ModConfig.INSTANCE.glassBackground) {
            GlassTheme.panel(context, box.x, box.y, box.width, box.height);
        }

        int textX = box.x + PADDING_X;
        int rowY = box.y + PADDING_Y;
        for (HudModule module : box.modules) {
            if (module.getGraphWidth() > 0 && module.getGraphHeight() > 0) {
                module.drawGraph(context, textX, rowY, module.getGraphWidth(), module.getGraphHeight());
                rowY += module.getGraphHeight();
                continue;
            }
            ItemStack icon = module.getIcon();
            if (!icon.isEmpty()) {
                int iconY = rowY + (ICON_ROW_HEIGHT - ICON_SIZE) / 2;
                context.drawItem(icon, textX, iconY);
                int textY = rowY + (ICON_ROW_HEIGHT - client.textRenderer.fontHeight) / 2 + 1;
                context.drawTextWithShadow(client.textRenderer, module.getText(), textX + ICON_SIZE + ICON_TEXT_GAP, textY, module.getAccentColor());
                rowY += ICON_ROW_HEIGHT;
            } else {
                context.drawTextWithShadow(client.textRenderer, module.getText(), textX, rowY, module.getAccentColor());
                rowY += LINE_HEIGHT;
            }
        }
    }

    /**
     * Small hotbar-slot-style widget: a single Totem of Undying icon with a
     * vanilla-style stack-count badge in the corner, centered just above the
     * hotbar (same rough spot vanilla puts the offhand slot indicator).
     */
    private void renderTotemCounter(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();

        int x = screenWidth / 2 - TOTEM_SLOT_SIZE / 2;
        int y = screenHeight - 22 - MARGIN - TOTEM_SLOT_SIZE;

        int count = totemCounterModule.getCount();

        if (ModConfig.INSTANCE.glassBackground) {
            GlassTheme.panel(context, x, y, TOTEM_SLOT_SIZE, TOTEM_SLOT_SIZE);
        }

        int iconX = x + (TOTEM_SLOT_SIZE - ICON_SIZE) / 2;
        int iconY = y + (TOTEM_SLOT_SIZE - ICON_SIZE) / 2;

        ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        context.drawItem(totem, iconX, iconY);
        context.drawStackOverlay(client.textRenderer, totem, iconX, iconY, String.valueOf(count));

        // Thin accent-colored underline instead of a full colored border,
        // so it still reads as "one small slot" rather than a big HUD panel.
        context.fill(x + 1, y + TOTEM_SLOT_SIZE - 1, x + TOTEM_SLOT_SIZE - 1, y + TOTEM_SLOT_SIZE, totemCounterModule.getAccentColor());
    }
}
