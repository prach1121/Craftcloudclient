package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.stats.StatCategory;
import com.craftcloudclient.client.stats.StatPeriod;
import com.craftcloudclient.client.stats.StatsManager;
import net.minecraft.item.ItemStack;

/**
 * One HUD row for a single tracked stat (hits, crystals placed, kills, ...).
 * Always shows today's running count - a live overlay only needs "how am I
 * doing right now"; the full today/week/month/year/all-time breakdown lives
 * in the Stats tab of the config menu instead.
 */
public class StatModule implements HudModule {

    private final StatCategory category;
    private final ItemStack icon;
    private int count = 0;

    public StatModule(StatCategory category) {
        this.category = category;
        this.icon = new ItemStack(category.icon);
    }

    @Override
    public void tick() {
        count = StatsManager.INSTANCE.getCount(category, StatPeriod.TODAY);
    }

    @Override
    public String getId() {
        return "stat_" + category.key;
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showStatsHud;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.statsPosition;
    }

    @Override
    public String getText() {
        return category.label + ": " + count;
    }

    @Override
    public ItemStack getIcon() {
        return icon;
    }

    @Override
    public int getAccentColor() {
        // Plain counters, not health-style warnings - no red/yellow/green thresholds.
        return 0xFFF2F5FA;
    }
}
