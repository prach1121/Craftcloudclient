package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.combat.CombatManager;
import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;

/**
 * "In Combat: 6s" countdown, visible only while {@link CombatManager}
 * says the player is currently in combat - like the graph modules, this
 * panel simply isn't part of any group while there's nothing to show,
 * rather than sitting there reading "In Combat: 0s" all the time.
 */
public class CombatTimerModule implements HudModule {

    @Override
    public String getId() {
        return "combat_timer";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showCombatTimer && CombatManager.isInCombat();
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.combatTimerPosition;
    }

    @Override
    public String getText() {
        return "In Combat: " + CombatManager.getSecondsRemaining() + "s";
    }

    @Override
    public int getAccentColor() {
        return 0xFFFF6F91;
    }
}
