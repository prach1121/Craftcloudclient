package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Plain real-world wall-clock readout ("Clock: 14:32" / "Clock: 2:32 PM"),
 * for the same reason people run a clock widget on their actual desktop -
 * so alt-tabbing to check the time doesn't interrupt a play session. This
 * is the player's system clock, not anything to do with in-game day/night
 * time (vanilla already exposes that via the F3 screen and the sun/moon).
 */
public class ClockModule implements HudModule {

    private static final DateTimeFormatter FORMAT_24H = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMAT_12H = DateTimeFormatter.ofPattern("h:mm a");

    private String text = "--:--";

    @Override
    public void tick() {
        LocalTime now = LocalTime.now();
        DateTimeFormatter format = ModConfig.INSTANCE.clockUse24Hour ? FORMAT_24H : FORMAT_12H;
        text = "Clock: " + now.format(format);
    }

    @Override
    public String getId() {
        return "clock";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showClock;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.clockPosition;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getAccentColor() {
        // Neutral informational color, same family as Playtime - this
        // isn't a "good/bad" stat, just a readout.
        return 0xFFB98BFF;
    }
}
