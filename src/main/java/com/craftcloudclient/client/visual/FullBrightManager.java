package com.craftcloudclient.client.visual;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Quick toggle for vanilla's own Brightness slider (Options > Video > Gamma)
 * instead of digging through menus every time. Enabling it pushes the
 * slider to its maximum ("Bright") value and remembers whatever it was
 * set to before, so disabling restores it exactly - it never sets gamma
 * past the range the vanilla slider itself allows.
 */
public final class FullBrightManager {

    private static double savedGamma = -1;

    private FullBrightManager() {
    }

    public static void apply(MinecraftClient client, boolean enabled) {
        SimpleOption<Double> gamma = client.options.getGamma();
        if (enabled) {
            if (savedGamma < 0) {
                savedGamma = gamma.getValue();
            }
            gamma.setValue(1.0);
        } else if (savedGamma >= 0) {
            gamma.setValue(savedGamma);
            savedGamma = -1;
        }
    }

    /** Re-applies whatever {@link ModConfig#fullBrightEnabled} currently says - call once after options finish loading so a saved "on" state takes effect at startup too. */
    public static void applyFromConfig(MinecraftClient client) {
        apply(client, ModConfig.INSTANCE.fullBrightEnabled);
    }
}
