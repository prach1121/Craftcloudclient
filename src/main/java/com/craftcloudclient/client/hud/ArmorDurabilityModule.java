package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Shows remaining durability for one equipment slot as an icon + number,
 * e.g. the helmet icon followed by "364" - matching the reference look.
 *
 * {@code DrawContext.drawItem(ItemStack, int, int)} is a plain public
 * overload (no entity/world argument needed) confirmed against the
 * yarn 1.21.8/1.21.11 mappings, so it's safe to use directly here instead
 * of falling back to text-only like the previous version of this file did.
 */
public class ArmorDurabilityModule implements HudModule {

    private final EquipmentSlot slot;
    private final String label;

    private ItemStack stack = ItemStack.EMPTY;
    private int remaining = -1;   // -1 = nothing equipped / not damageable
    private int max = 0;

    public ArmorDurabilityModule(EquipmentSlot slot, String label) {
        this.slot = slot;
        this.label = label;
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            stack = ItemStack.EMPTY;
            remaining = -1;
            return;
        }

        ItemStack equipped = client.player.getEquippedStack(slot);
        if (equipped.isEmpty() || !equipped.isDamageable()) {
            stack = ItemStack.EMPTY;
            remaining = -1;
            return;
        }

        stack = equipped;
        max = equipped.getMaxDamage();
        remaining = max - equipped.getDamage();
    }

    @Override
    public String getId() {
        return "armor_" + slot.name().toLowerCase();
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showArmorStatus && remaining >= 0;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.armorStatusPosition;
    }

    @Override
    public String getText() {
        return String.valueOf(remaining);
    }

    @Override
    public ItemStack getIcon() {
        return stack;
    }

    @Override
    public int getAccentColor() {
        if (max <= 0) return 0xFF55FF7A;
        float ratio = remaining / (float) max;
        if (ratio <= 0.15f) return 0xFFFF5555;
        if (ratio <= 0.4f) return 0xFFE8FF55;
        return 0xFF55FF7A;
    }

    /** Kept for tooltip/accessibility use even though the row no longer prints it. */
    public String getLabel() {
        return label;
    }
}
