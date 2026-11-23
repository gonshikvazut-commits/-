package com.example.pulsewatermark;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PulseWatermarkMod implements ClientModInitializer {

    private DynamicTexture dynamicTexture;
    private Identifier textureId;
    private String lastKey = "";
    private int texWidth = 220;
    private int texHeight = 32;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::renderWatermark);
    }

    private void renderWatermark(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getDebugOverlay().showDebugScreen()) {
            return;
        }

        int ping = getPing(client);
        int fps = client.getFps();
        String key = ping + "_" + fps;

        // Перерисовываем текстуру в HD-качестве только при изменении FPS или пинга
        if (!key.equals(lastKey) || textureId == null) {
            lastKey = key;
            rebuildTexture(client, "pulsevisuals.pro", ping + " ms", fps + " FPS");
        }

        if (textureId != null) {
            int guiScale = Math.max(1, client.getWindow().getGuiScale());
            int drawW = Math.round((float) texWidth / guiScale);
            int drawH = Math.round((float) texHeight / guiScale);

            int screenWidth = client.getWindow().getGuiScaledWidth();
            int x = (screenWidth - drawW) / 2;
            int y = 7;

            graphics.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                textureId,
                x, y,
                0.0f, 0.0f,
                drawW, drawH,
                drawW, drawH
            );
        }
    }

    private void rebuildTexture(Minecraft client, String brand, String pingStr, String fpsStr) {
        int scale = Math.max(2, client.getWindow().getGuiScale());
        int h = 26 * scale; // Точная высота капсулы как на скриншоте

        // Измеряем ширину текста настоящим гладким шрифтом
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMeasure = dummy.createGraphics();
        Font fontMain = getSmoothFont(Font.PLAIN, 12.5f * scale);
        gMeasure.setFont(fontMain);
        FontMetrics fm = gMeasure.getFontMetrics();

        int padX = 14 * scale;
        int iconW = 14 * scale;
        int gapIcon = 8 * scale;
        int gapSep = 10 * scale;
        int sepW = Math.max(1, scale);

        int brandW = fm.stringWidth(brand);
        int pingW = fm.stringWidth(pingStr);
        int fpsW = fm.stringWidth(fpsStr);

        int w = padX * 2 + iconW + gapIcon + brandW + gapSep * 2 + sepW + pingW + gapSep * 2 + sepW + fpsW;
        gMeasure.dispose();

        // Создаем HD-холст с максимальным сглаживанием (Anti-Aliasing)
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 1. Глубокий черный фон капсулы 1-в-1 как на оригинальном скриншоте
        g.setColor(new Color(8, 8, 11, 245));
        g.fillRoundRect(0, 0, w, h, h, h);

        // 2. Тончайший контур по краю капсулы
        g.setColor(new Color(255, 255, 255, 22));
        g.setStroke(new BasicStroke(Math.max(1f, scale * 0.5f)));
        g.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        // 3. Плавная векторная иконка пульса слева
        g.setColor(new Color(123, 104, 238, 255));
        g.setStroke(new BasicStroke(1.6f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ix = padX;
        int cy = h / 2;
        int[] px = {
            ix,
            ix + 3 * scale,
            ix + 5 * scale,
            ix + 7 * scale,
            ix + 9 * scale,
            ix + 11 * scale,
            ix + 14 * scale
        };
        int[] py = {
            cy,
            cy,
            cy - 5 * scale,
            cy + 6 * scale,
            cy - 3 * scale,
            cy,
            cy
        };
        g.drawPolyline(px, py, px.length);

        // 4. Гладкий текст без единого пикселя
        g.setFont(fontMain);
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        int curX = ix + iconW + gapIcon;

        g.setColor(new Color(250, 250, 252, 255));
        g.drawString(brand, curX, textY);
        curX += brandW + gapSep;

        // Вертикальный разделитель №1
        g.setColor(new Color(45, 45, 54, 255));
        g.fillRect(curX, 5 * scale, sepW, h - 10 * scale);
        curX += sepW + gapSep;

        g.setColor(new Color(235, 235, 240, 255));
        g.drawString(pingStr, curX, textY);
        curX += pingW + gapSep;

        // Вертикальный разделитель №2
        g.setColor(new Color(45, 45, 54, 255));
        g.fillRect(curX, 5 * scale, sepW, h - 10 * scale);
        curX += sepW + gapSep;

        g.setColor(new Color(235, 235, 240, 255));
        g.drawString(fpsStr, curX, textY);

        g.dispose();

        // Переносим картинку в текстуру Minecraft
        NativeImage nativeImg = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                nativeImg.setPixel(x, y, img.getRGB(x, y));
            }
        }

        if (dynamicTexture != null) {
            dynamicTexture.close();
        }
        dynamicTexture = new DynamicTexture(() -> "pulse_watermark", nativeImg);
        textureId = Identifier.fromNamespaceAndPath("pulsewatermark", "hud_pill");
        client.getTextureManager().register(textureId, dynamicTexture);

        texWidth = w;
        texHeight = h;
    }

    private Font getSmoothFont(int style, float size) {
        String[] candidates = {"Segoe UI", "Inter", "Arial", "SansSerif"};
        for (String name : candidates) {
            Font f = new Font(name, style, Math.round(size));
            if (!f.getFamily().equals("Dialog")) {
                return f.deriveFont(size);
            }
        }
        return new Font(Font.SANS_SERIF, style, Math.round(size)).deriveFont(size);
    }

    private int getPing(Minecraft client) {
        if (client.getConnection() == null || client.player == null) return 0;
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }
}
