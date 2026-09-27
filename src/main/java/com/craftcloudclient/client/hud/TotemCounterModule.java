package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Total count of Totems of Undying carried anywhere in the player's
 * inventory (main inventory + hotbar + offhand - whatever the
 * {@link Inventory} interface exposes for the player). Counts stacks by
 * item type rather than by slot index, so it doesn't depend on exactly
 * how many slots {@code PlayerInventory} exposes in this version.
 *
 * Rendered by {@link HudManager} as a single small icon with a stack-count
 * badge (like a hotbar slot) anchored above the hotbar, rather than as a
 * grouped text panel - this class only tracks state, it doesn't implement
 * {@link HudModule} since its layout doesn't fit the corner-panel grouping
 * the other modules share.
 */
public class TotemCounterModule {

    private int count = 0;

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            count = 0;
            return;
        }

        Inventory inv = client.player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                total += stack.getCount();
            }
        }
        count = total;
    }

    public boolean isEnabled() {
        return ModConfig.INSTANCE.showTotemCounter;
    }

    public int getCount() {
        return count;
    }

    /** Border/badge accent: red at 0, yellow at 1, green at 2+. */
    public int getAccentColor() {
        if (count == 0) return 0xFFFF5555;
        if (count == 1) return 0xFFE8FF55;
        return 0xFF55FF7A;
    }
}
