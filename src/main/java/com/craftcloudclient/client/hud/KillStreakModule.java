package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import com.craftcloudclient.client.gui.Animator;
import com.craftcloudclient.client.gui.GlassTheme;
import com.craftcloudclient.client.stats.KillStreakManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * "Streak: 4 (Best: 12)" - current consecutive-kill streak plus the
 * all-time best, so a good run is visible without digging into the
 * Stats tab. Text color ramps up the hotter the current streak gets,
 * same idea as a combo counter, and briefly flashes bright white every
 * time the streak extends so a kill actually feels like it landed.
 */
public class KillStreakModule implements HudModule {

    private static final ItemStack ICON = new ItemStack(Items.NETHERITE_SWORD);
    private static final long FLASH_DURATION_MS = 450;
    private static final int FLASH_COLOR = 0xFFFFFFFF;

    private int current = 0;
    private int best = 0;
    private long flashStart = Long.MIN_VALUE;

    @Override
    public void tick() {
        int updated = KillStreakManager.INSTANCE.getCurrentStreak();
        if (updated > current) {
            flashStart = System.currentTimeMillis();
        }
        current = updated;
        best = KillStreakManager.INSTANCE.getBestStreak();
    }

    @Override
    public String getId() {
        return "kill_streak";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showKillStreak;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.killStreakPosition;
    }

    @Override
    public String getText() {
        return "Streak: " + current + " (Best: " + best + ")";
    }

    @Override
    public ItemStack getIcon() {
        return ICON;
    }

    @Override
    public int getAccentColor() {
        int base = baseColor();
        float flash = Animator.flashOut(flashStart, FLASH_DURATION_MS);
        return flash <= 0f ? base : GlassTheme.blend(base, FLASH_COLOR, flash);
    }

    private int baseColor() {
        if (current >= 10) return 0xFFFF5555; // on fire
        if (current >= 5) return 0xFFE8FF55;  // heating up
        return 0xFFF2F5FA;                    // plain
    }
}
