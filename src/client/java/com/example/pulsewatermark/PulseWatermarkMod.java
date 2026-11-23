package com.example.pulsewatermark;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public class PulseWatermarkMod implements ClientModInitializer {

    private static final Identifier WATERMARK_LAYER = Identifier.of("pulsewatermark", "center_pill");

    @Override
    public void onInitializeClient() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(IdentifiedLayer.HOTBAR_AND_BARS, WATERMARK_LAYER, this::renderWatermark)
        );
    }

    private void renderWatermark(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        String brand = "pulsevisuals.pro";
        String pingStr = getPing(client) + " ms";
        String fpsStr = client.getCurrentFps() + " FPS";
        String sep = "  |  ";

        TextRenderer tr = client.textRenderer;
        int iconSpace = 14;
        int contentWidth = iconSpace + tr.getWidth(brand + sep + pingStr + sep + fpsStr);

        int padX = 10;
        int padY = 5;
        int width = contentWidth + padX * 2;
        int height = tr.fontHeight + padY * 2;

        int screenWidth = client.getWindow().getScaledWidth();
        int x = (screenWidth - width) / 2;
        int y = 6;
        int radius = height / 2;

        drawPill(context, x - 1, y - 1, width + 2, height + 2, radius + 1, 0x35FFFFFF);
        drawPill(context, x, y, width, height, radius, 0xD80D0D12);

        int iconX = x + padX;
        int iconY = y + height / 2;
        int pulseColor = 0xFF7B68EE;
        context.fill(iconX, iconY, iconX + 2, iconY + 1, pulseColor);
        context.fill(iconX + 2, iconY - 3, iconX + 3, iconY + 1, pulseColor);
        context.fill(iconX + 3, iconY - 3, iconX + 4, iconY + 4, pulseColor);
        context.fill(iconX + 4, iconY - 1, iconX + 5, iconY + 4, pulseColor);
        context.fill(iconX + 5, iconY - 1, iconX + 7, iconY, pulseColor);

        int curX = iconX + iconSpace;
        int textY = y + padY + 1;

        context.drawText(tr, brand, curX, textY, 0xFFFFFFFF, false);
        curX += tr.getWidth(brand);

        context.drawText(tr, sep, curX, textY, 0xFF555560, false);
        curX += tr.getWidth(sep);

        context.drawText(tr, pingStr, curX, textY, 0xFFE0E0E5, false);
        curX += tr.getWidth(pingStr);

        context.drawText(tr, sep, curX, textY, 0xFF555560, false);
        curX += tr.getWidth(sep);

        context.drawText(tr, fpsStr, curX, textY, 0xFFE0E0E5, false);
    }

    private void drawPill(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        radius = Math.min(radius, Math.min(width / 2, height / 2));
        context.fill(x, y + radius, x + width, y + height - radius, color);
        for (int i = 0; i < radius; i++) {
            double dy = radius - i - 0.5;
            int dx = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            context.fill(x + dx, y + i, x + width - dx, y + i + 1, color);
            context.fill(x + dx, y + height - 1 - i, x + width - dx, y + height - i, color);
        }
    }

    private int getPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}
