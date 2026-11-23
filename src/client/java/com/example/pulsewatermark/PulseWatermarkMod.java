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
    private int texWidth = 160;
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
        int pillH = 16 * scale; // Низкая компактная капсула как на видео

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMeasure = dummy.createGraphics();
        Font fontMain = getSmoothFont(Font.PLAIN, 9.2f * scale);
        gMeasure.setFont(fontMain);
        FontMetrics fm = gMeasure.getFontMetrics();

        String dot = " • ";
        int padX = 8 * scale;
        int iconW = 9 * scale;
        int gapIcon = 5 * scale;

        int brandW = fm.stringWidth(brand);
        int dotW = fm.stringWidth(dot);
        int pingW = fm.stringWidth(pingStr);
        int fpsW = fm.stringWidth(fpsStr);

        int pillW = padX * 2 + iconW + gapIcon + brandW + dotW + pingW + dotW + fpsW;
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

        // 1. Мягкая тень под капсулой
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRoundRect(ox - scale, oy + Math.max(1, scale / 2), pillW + scale * 2, pillH + scale, pillH, pillH);

        // 2. Глубокий черный фон капсулы
        g.setColor(new Color(8, 8, 11, 245));
        g.fillRoundRect(ox, oy, pillW, pillH, pillH, pillH);

        // 3. Тонкий блик-контур
        g.setColor(new Color(255, 255, 255, 20));
        g.setStroke(new BasicStroke(Math.max(1f, scale * 0.4f)));
        g.drawRoundRect(ox, oy, pillW - 1, pillH - 1, pillH, pillH);

        // 4. Фиолетово-синяя иконка пульса слева
        g.setColor(new Color(126, 108, 255, 255));
        g.setStroke(new BasicStroke(1.25f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ix = ox + padX;
        int cy = oy + pillH / 2;
        int[] px = {
            ix,
            ix + 2 * scale,
            ix + 3 * scale,
            ix + 5 * scale,
            ix + 7 * scale,
            ix + 8 * scale,
            ix + 9 * scale
        };
        int[] py = {
            cy,
            cy,
            cy - 3 * scale,
            cy + 4 * scale,
            cy - 2 * scale,
            cy,
            cy
        };
        g.drawPolyline(px, py, px.length);

        // 5. Текст с точками-разделителями " • "
        g.setFont(fontMain);
        int textY = oy + (pillH - fm.getHeight()) / 2 + fm.getAscent();
        int curX = ix + iconW + gapIcon;

        g.setColor(new Color(250, 250, 255, 255));
        g.drawString(brand, curX, textY);
        curX += brandW;

        g.setColor(new Color(105, 105, 115, 255));
        g.drawString(dot, curX, textY);
        curX += dotW;

        g.setColor(new Color(235, 235, 240, 255));
        g.drawString(pingStr, curX, textY);
        curX += pingW;

        g.setColor(new Color(105, 105, 115, 255));
        g.drawString(dot, curX, textY);
        curX += dotW;

        g.setColor(new Color(235, 235, 240, 255));
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
