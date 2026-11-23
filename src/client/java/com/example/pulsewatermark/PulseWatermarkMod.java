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
    private int texWidth = 200;
    private int texHeight = 28;

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
            int y = 4; // Ближе к верхнему краю экрана, как в оригинале

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
        int padShadow = 3 * scale; // Запас под мягкую тень
        int pillH = 21 * scale;    // Более изящная, приплюснутая высота капсулы

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMeasure = dummy.createGraphics();
        Font fontMain = getSmoothFont(Font.PLAIN, 11.2f * scale);
        gMeasure.setFont(fontMain);
        FontMetrics fm = gMeasure.getFontMetrics();

        int padX = 11 * scale;
        int iconW = 11 * scale;
        int gapIcon = 6 * scale;
        int gapSep = 8 * scale;
        int sepW = Math.max(1, Math.round(scale * 0.8f));

        int brandW = fm.stringWidth(brand);
        int pingW = fm.stringWidth(pingStr);
        int fpsW = fm.stringWidth(fpsStr);

        int pillW = padX * 2 + iconW + gapIcon + brandW + gapSep * 2 + sepW + pingW + gapSep * 2 + sepW + fpsW;
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

        // 1. Мягкая внешняя тень под капсулой
        g.setColor(new Color(0, 0, 0, 55));
        g.fillRoundRect(ox - scale, oy + scale, pillW + scale * 2, pillH + scale, pillH, pillH);

        // 2. Глубокий черный фон капсулы
        g.setColor(new Color(9, 9, 12, 248));
        g.fillRoundRect(ox, oy, pillW, pillH, pillH, pillH);

        // 3. Едва уловимый контур
        g.setColor(new Color(255, 255, 255, 18));
        g.setStroke(new BasicStroke(Math.max(1f, scale * 0.45f)));
        g.drawRoundRect(ox, oy, pillW - 1, pillH - 1, pillH, pillH);

        // 4. Компактная иконка пульса слева
        g.setColor(new Color(134, 114, 255, 255));
        g.setStroke(new BasicStroke(1.35f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ix = ox + padX;
        int cy = oy + pillH / 2;
        int[] px = {
            ix,
            ix + 2 * scale,
            ix + 4 * scale,
            ix + 6 * scale,
            ix + 8 * scale,
            ix + 9 * scale,
            ix + 11 * scale
        };
        int[] py = {
            cy,
            cy,
            cy - 4 * scale,
            cy + 4 * scale,
            cy - 2 * scale,
            cy,
            cy
        };
        g.drawPolyline(px, py, px.length);

        // 5. Изящный текст и тонкие вертикальные разделители
        g.setFont(fontMain);
        int textY = oy + (pillH - fm.getHeight()) / 2 + fm.getAscent();
        int curX = ix + iconW + gapIcon;

        g.setColor(new Color(248, 248, 252, 255));
        g.drawString(brand, curX, textY);
        curX += brandW + gapSep;

        // Вертикальный разделитель №1
        g.setColor(new Color(42, 42, 50, 255));
        g.fillRect(curX, oy + 4 * scale, sepW, pillH - 8 * scale);
        curX += sepW + gapSep;

        g.setColor(new Color(230, 230, 236, 255));
        g.drawString(pingStr, curX, textY);
        curX += pingW + gapSep;

        // Вертикальный разделитель №2
        g.setColor(new Color(42, 42, 50, 255));
        g.fillRect(curX, oy + 4 * scale, sepW, pillH - 8 * scale);
        curX += sepW + gapSep;

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
        String[] candidates = {"Inter", "Arial", "Segoe UI", "SansSerif"};
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
