package com.craftcloudclient.client.config;

/**
 * The color options for the Attack Particles feature (Settings > PvP) -
 * a small spark burst spawned at the point of impact whenever the
 * player's attack connects with an entity. See
 * {@link com.craftcloudclient.client.combat.AttackParticleModule} for
 * why these are colored dust sparks styled after firework sparks rather
 * than vanilla's actual (uncolorable) firework particle.
 */
public enum AttackParticleType {
    RED("Red Firework", 0xFF3B30),
    YELLOW("Yellow Firework", 0xFFD60A),
    ORANGE("Orange Firework", 0xFF8C1A);

    /** Shown in the config screen. */
    private final String displayName;
    /** 0xRRGGBB tint used for the spawned particles. */
    private final int rgb;

    AttackParticleType(String displayName, int rgb) {
        this.displayName = displayName;
        this.rgb = rgb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRgb() {
        return rgb;
    }

    public AttackParticleType next() {
        AttackParticleType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
