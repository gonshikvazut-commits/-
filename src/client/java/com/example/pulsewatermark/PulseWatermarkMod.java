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
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class PulseWatermarkMod implements ClientModInitializer {

    private DynamicTexture dynamicTexture;
    private Identifier textureId;
    private String lastKey = "";
    private int texWidth = 140;
    private int texHeight = 20;

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
            int y = 4;

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
        int padShadow = 2 * scale;
        int pillH = 16 * scale;

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMeasure = dummy.createGraphics();
        Font fontBrand = getSmoothFont(Font.BOLD, 9.0f * scale);
        Font fontStats = getSmoothFont(Font.PLAIN, 9.0f * scale);

        FontMetrics fmBrand = gMeasure.getFontMetrics(fontBrand);
        FontMetrics fmStats = gMeasure.getFontMetrics(fontStats);

        // Компактные отступы, чтобы плашка была короткой и аккуратной
        int padX = 6 * scale;
        int iconW = 9 * scale;
        int gapIcon = 4 * scale;
        int dotGap = 4 * scale;
        int dotSize = Math.max(2, Math.round(1.6f * scale));

        int brandW = fmBrand.stringWidth(brand);
        int pingW = fmStats.stringWidth(pingStr);
        int fpsW = fmStats.stringWidth(fpsStr);

        int pillW = padX * 2 + iconW + gapIcon + brandW + (dotGap * 2 + dotSize) * 2 + pingW + fpsW;
        int w = pillW + padShadow * 2;
        int h = pillH + padShadow * 2;
        gMeasure.dispose();

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int ox = padShadow;
        int oy = padShadow;

        // 1. Мягкая тень
        g.setColor(new Color(0, 0, 0, 55));
        g.fillRoundRect(ox - scale, oy + Math.max(1, scale / 2), pillW + scale * 2, pillH + scale, pillH, pillH);

        // 2. Глубокий черный фон капсулы
        g.setColor(new Color(9, 9, 12, 246));
        g.fillRoundRect(ox, oy, pillW, pillH, pillH, pillH);

        // 3. Тонкий контур
        g.setColor(new Color(255, 255, 255, 22));
        g.setStroke(new BasicStroke(Math.max(1f, scale * 0.4f)));
        g.drawRoundRect(ox, oy, pillW - 1, pillH - 1, pillH, pillH);

        // 4. Симметричная, ровная иконка пульса (без кривых углов)
        float ix = ox + padX;
        float cy = oy + pillH / 2.0f;
        float step = iconW / 6.0f;

        Path2D.Float pulsePath = new Path2D.Float();
        pulsePath.moveTo(ix, cy);
        pulsePath.lineTo(ix + step * 1.4f, cy);
        pulsePath.lineTo(ix + step * 2.3f, cy - 3.2f * scale);
        pulsePath.lineTo(ix + step * 3.7f, cy + 3.2f * scale);
        pulsePath.lineTo(ix + step * 4.6f, cy);
        pulsePath.lineTo(ix + iconW, cy);

        // Лёгкое свечение вокруг иконки пульса
        g.setColor(new Color(124, 104, 238, 70));
        g.setStroke(new BasicStroke(2.4f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(pulsePath);

        // Сама чёткая линия пульса
        g.setColor(new Color(138, 120, 255, 255));
        g.setStroke(new BasicStroke(1.25f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(pulsePath);

        // 5. Текст и ровные круглые точки-разделители
        int textY = oy + (pillH - fmBrand.getHeight()) / 2 + fmBrand.getAscent();
        int curX = Math.round(ix + iconW + gapIcon);

        g.setFont(fontBrand);
        g.setColor(new Color(250, 250, 255, 255));
        g.drawString(brand, curX, textY);
        curX += brandW + dotGap;

        // Ровная круглая точка №1
        g.setColor(new Color(95, 95, 108, 255));
        g.fillOval(curX, Math.round(cy - dotSize / 2.0f), dotSize, dotSize);
        curX += dotSize + dotGap;

        g.setFont(fontStats);
        g.setColor(new Color(230, 230, 236, 255));
        g.drawString(pingStr, curX, textY);
        curX += pingW + dotGap;

        // Ровная круглая точка №2
        g.setColor(new Color(95, 95, 108, 255));
        g.fillOval(curX, Math.round(cy - dotSize / 2.0f), dotSize, dotSize);
        curX += dotSize + dotGap;

        g.setColor(new Color(230, 230, 236, 255));
        g.drawString(fpsStr, curX, textY);

        g.dispose();

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
        String[] candidates = {"Inter", "Segoe UI", "Arial", "SansSerif"};
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
