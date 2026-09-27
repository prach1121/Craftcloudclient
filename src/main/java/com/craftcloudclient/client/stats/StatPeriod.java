package com.craftcloudclient.client.stats;

/**
 * The rolling windows {@link StatsManager} can aggregate a category's
 * daily counts into. Weeks follow the ISO week definition (Monday start),
 * months/years follow the calendar - all computed relative to the
 * player's local system clock, same as {@link java.time.LocalDate#now()}.
 */
public enum StatPeriod {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All");

    public final String label;

    StatPeriod(String label) {
        this.label = label;
    }
}
