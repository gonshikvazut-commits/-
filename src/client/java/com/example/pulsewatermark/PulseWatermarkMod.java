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

        Font font = client.font;
        float textScale = 0.85f;

        int iconWidth = 12;
        int gapAfterIcon = 7;
        int sepGap = 8;

        int brandW = Math.round(font.width(brand) * textScale);
        int pingW = Math.round(font.width(pingStr) * textScale);
        int fpsW = Math.round(font.width(fpsStr) * textScale);

        int padX = 12;
        int height = 21;
        int width = padX * 2 + iconWidth + gapAfterIcon + brandW + sepGap * 2 + 1 + pingW + sepGap * 2 + 1 + fpsW;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int x = (screenWidth - width) / 2;
        int y = 7;

        // 1. Мягкая внешняя тень вокруг капсулы
        drawSmoothPill(graphics, x - 2, y - 1, width + 4, height + 3, 0x45000000);

        // 2. Глубокий почти непрозрачный черный фон капсулы, как в оригинале Pulse
        drawSmoothPill(graphics, x, y, width, height, 0xF208080C);

        // 3. Едва заметная тонкая обводка по верхнему и нижнему краю
        int r = height / 2;
        graphics.fill(x + r, y, x + width - r, y + 1, 0x22FFFFFF);
        graphics.fill(x + r, y + height - 1, x + width - r, y + height, 0x18FFFFFF);

        // 4. Фирменная иконка пульса (кардиограмма) слева
        int ix = x + padX;
        int iy = y + height / 2;
        int pulseColor = 0xFF7B68EE;

        graphics.fill(ix, iy, ix + 2, iy + 1, pulseColor);
        graphics.fill(ix + 2, iy - 2, ix + 3, iy + 1, pulseColor);
        graphics.fill(ix + 3, iy - 5, ix + 4, iy - 1, pulseColor);
        graphics.fill(ix + 4, iy - 2, ix + 5, iy + 5, pulseColor);
        graphics.fill(ix + 5, iy + 1, ix + 6, iy + 5, pulseColor);
        graphics.fill(ix + 6, iy - 3, ix + 7, iy + 2, pulseColor);
        graphics.fill(ix + 7, iy - 1, ix + 8, iy + 1, pulseColor);
        graphics.fill(ix + 8, iy, ix + 11, iy + 1, pulseColor);

        // 5. Текст и тонкие вертикальные разделители
        int curX = ix + iconWidth + gapAfterIcon;
        float textY = y + (height - font.lineHeight * textScale) / 2.0f + 0.5f;

        drawScaledText(graphics, font, brand, curX, textY, textScale, 0xFFFFFFFF);
        curX += brandW + sepGap;

        // Тонкий вертикальный разделитель №1
        graphics.fill(curX, y + 6, curX + 1, y + height - 6, 0xFF32323C);
        curX += 1 + sepGap;

        drawScaledText(graphics, font, pingStr, curX, textY, textScale, 0xFFE6E6EC);
        curX += pingW + sepGap;

        // Тонкий вертикальный разделитель №2
        graphics.fill(curX, y + 6, curX + 1, y + height - 6, 0xFF32323C);
        curX += 1 + sepGap;

        drawScaledText(graphics, font, fpsStr, curX, textY, textScale, 0xFFE6E6EC);
    }

    private void drawScaledText(GuiGraphics graphics, Font font, String text, float x, float y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    private void drawSmoothPill(GuiGraphics graphics, int x, int y, int width, int height, int argbColor) {
        int baseAlpha = (argbColor >>> 24) & 0xFF;
        int rgb = argbColor & 0x00FFFFFF;
        float r = height / 2.0f;

        int leftInner = (int) Math.ceil(x + r);
        int rightInner = (int) Math.floor(x + width - r);
        if (rightInner > leftInner) {
            graphics.fill(leftInner, y, rightInner, y + height, argbColor);
        }

        for (int py = 0; py < height; py++) {
            float dy = (py + 0.5f) - r;
            float distSq = r * r - dy * dy;
            if (distSq <= 0) continue;

            float span = (float) Math.sqrt(distSq);
            float exactLeft = (x + r) - span;
            float exactRight = (x + width - r) + span;

            int lx = (int) Math.floor(exactLeft);
            int rx = (int) Math.floor(exactRight);

            float leftCov = 1.0f - (exactLeft - lx);
            int lAlpha = Math.min(255, Math.max(0, Math.round(baseAlpha * leftCov)));
            graphics.fill(lx, y + py, lx + 1, y + py + 1, (lAlpha << 24) | rgb);

            float rightCov = exactRight - rx;
            int rAlpha = Math.min(255, Math.max(0, Math.round(baseAlpha * rightCov)));
            graphics.fill(rx, y + py, rx + 1, y + py + 1, (rAlpha << 24) | rgb);

            if (leftInner > lx + 1) {
                graphics.fill(lx + 1, y + py, leftInner, y + py + 1, argbColor);
            }
            if (rx > rightInner) {
                graphics.fill(rightInner, y + py, rx, y + py + 1, argbColor);
            }
        }
    }

    private int getPing(Minecraft client) {
        if (client.getConnection() == null || client.player == null) return 0;
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }
}
