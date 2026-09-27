package com.craftcloudclient.client.hud;

import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule implements HudModule {

    private int ping = 0;

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            ping = 0;
            return;
        }
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            ping = 0;
            return;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
        ping = (entry != null) ? entry.getLatency() : 0;
    }

    @Override
    public String getId() {
        return "ping";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.INSTANCE.showPing;
    }

    @Override
    public HudPosition getPosition() {
        return ModConfig.INSTANCE.pingPosition;
    }

    @Override
    public String getText() {
        return "Ping: " + ping + "ms";
    }

    @Override
    public int getAccentColor() {
        if (ping <= 60) return 0xFF55FF7A;
        if (ping <= 150) return 0xFFE8FF55;
        return 0xFFFF5555;
    }
}
