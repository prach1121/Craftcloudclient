package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Countdown to the next day/night transition - "Night in: 4:12" while it's
 * still light out, flipping to "Day in: 2:03" once night falls - so
 * planning a mining trip or a mob-farm-safe build session doesn't require
 * memorizing the raw 0-24000 tick scale from the F3 debug screen.
 *
 * Uses the same day/dusk/night/dawn tick boundaries vanilla's own sky
 * rendering and mob-spawning logic are built around (day 0-12000, dusk
 * 12000-13000, night 13000-23000, dawn 23000-24000), so the label flips
 * at the same moment the sky actually starts changing rather than some
 * approximate/rounded boundary.
 */
public class TimeUntilDayModule implements HudModule {

    private static final long DAY_END = 12000;
    private static final long NIGHT_START = 13000;
    private static final long DAY_LENGTH = 24000;

    private String text = "Day in: --:--";
    private boolean isDaytime = true;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            text = "Day in: --:--";
            isDaytime = true;
            return;
        }

        long time = client.world.getTimeOfDay() % DAY_LENGTH;
        long ticksRemaining;

        if (time < DAY_END) {
            // Still daytime (including dusk run-up) - count down to the
            // moment night actually starts, i.e. when mobs can spawn.
            isDaytime = true;
            ticksRemaining = NIGHT_START - time;
        } else {
            // Night (including dawn run-up) - count down to sunrise.
            isDaytime = false;
            ticksRemaining = DAY_LENGTH - time;
        }

        text = (isDaytime ? "Night in: " : "Day in: ") + formatTicks(ticksRemaining);
    }

    /** Formats a tick count as real-world "m:ss" at the vanilla 20 ticks/second rate. */
    private String formatTicks(long ticks) {
        long totalSeconds = ticks / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public String getId() {
        return "time_until_day";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showTimeUntilDay;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.timeUntilDayPosition;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getAccentColor() {
        // Warm gold while the sun's still up (and dusk is approaching),
        // cool blue once night has fallen and dawn is what's being
        // counted down to instead - same "what's coming" color logic as
        // ClockModule/PlaytimeModule's neutral readouts, just split in two.
        return isDaytime ? 0xFFFFC94D : 0xFF7FB2FF;
    }
}
