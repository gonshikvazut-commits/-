package com.example.pulsewatermark;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PulseWatermarkMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::renderWatermark);
    }

    private void renderWatermark(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getDebugOverlay().showDebugScreen()) {
            return;
        }

        String brand = "pulsevisuals.pro";
        String pingStr = getPing(client) + " ms";
        String fpsStr = client.getFps() + " FPS";
        String sep = "  |  ";

        Font font = client.font;
        int iconSpace = 14;
        int contentWidth = iconSpace + font.width(brand + sep + pingStr + sep + fpsStr);

        int padX = 10;
        int padY = 5;
        int width = contentWidth + padX * 2;
        int height = font.lineHeight + padY * 2;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int x = (screenWidth - width) / 2;
        int y = 6;
        int radius = height / 2;

        drawPill(graphics, x - 1, y - 1, width + 2, height + 2, radius + 1, 0x35FFFFFF);
        drawPill(graphics, x, y, width, height, radius, 0xD80D0D12);

        int iconX = x + padX;
        int iconY = y + height / 2;
        int pulseColor = 0xFF7B68EE;
        graphics.fill(iconX, iconY, iconX + 2, iconY + 1, pulseColor);
        graphics.fill(iconX + 2, iconY - 3, iconX + 3, iconY + 1, pulseColor);
        graphics.fill(iconX + 3, iconY - 3, iconX + 4, iconY + 4, pulseColor);
        graphics.fill(iconX + 4, iconY - 1, iconX + 5, iconY + 4, pulseColor);
        graphics.fill(iconX + 5, iconY - 1, iconX + 7, iconY, pulseColor);

        int curX = iconX + iconSpace;
        int textY = y + padY + 1;

        graphics.drawString(font, brand, curX, textY, 0xFFFFFFFF, false);
        curX += font.width(brand);

        graphics.drawString(font, sep, curX, textY, 0xFF555560, false);
        curX += font.width(sep);

        graphics.drawString(font, pingStr, curX, textY, 0xFFE0E0E5, false);
        curX += font.width(pingStr);

        graphics.drawString(font, sep, curX, textY, 0xFF555560, false);
        curX += font.width(sep);

        graphics.drawString(font, fpsStr, curX, textY, 0xFFE0E0E5, false);
    }

    private void drawPill(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        radius = Math.min(radius, Math.min(width / 2, height / 2));
        graphics.fill(x, y + radius, x + width, y + height - radius, color);
        for (int i = 0; i < radius; i++) {
            double dy = radius - i - 0.5;
            int dx = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            graphics.fill(x + dx, y + i, x + width - dx, y + i + 1, color);
            graphics.fill(x + dx, y + height - 1 - i, x + width - dx, y + height - i, color);
        }
    }

    private int getPing(Minecraft client) {
        if (client.getConnection() == null || client.player == null) return 0;
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }
}
