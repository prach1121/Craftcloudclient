package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Shows the name of the biome the player is currently standing in, the
 * same info the F3 debug screen buries a few lines down. Resolved through
 * the standard "biome.&lt;namespace&gt;.&lt;path&gt;" translation key -
 * the same lookup vanilla's own debug HUD uses - so datapack/modded
 * biomes with a translation entry are labeled correctly instead of only
 * ever showing raw vanilla names.
 */
public class BiomeModule implements HudModule {

    private String text = "Biome: Unknown";

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            text = "Biome: Unknown";
            return;
        }

        RegistryEntry<Biome> entry = client.world.getBiome(client.player.getBlockPos());
        String name = entry.getKey()
                .map(RegistryKey::getValue)
                .map(this::translate)
                .orElse("Unknown");
        text = "Biome: " + name;
    }

    private String translate(Identifier id) {
        return Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString();
    }

    @Override
    public String getId() {
        return "biome";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showBiome;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.biomePosition;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getAccentColor() {
        return 0xFF8FE0A0;
    }
}
