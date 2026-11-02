package com.restonic4.inconvenienttemperature.client;

import com.restonic4.inconvenienttemperature.config.Tuning;
import net.minecraft.client.gui.GuiGraphics;

public final class TemperatureBar {
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    private static final int INNER_WIDTH = WIDTH - 2;
    private static final int FRAME = 0xFF000000;
    private static final int MARKER = 0xFFFFFFFF;
    private static final int FREEZING = 0xFF4FB6FF;
    private static final int NORMAL = 0xFF7BC74D;
    private static final int BURNING = 0xFFFF3B1F;

    private TemperatureBar() {}

    public static void render(GuiGraphics graphics, int left, float temperature) {
        int top = graphics.guiHeight() - 32 + 3;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, FRAME);
        for (int column = 0; column < INNER_WIDTH; column++) {
            float position = column / (float) (INNER_WIDTH - 1);
            graphics.fill(left + 1 + column, top + 1, left + 2 + column, top + HEIGHT - 1, gradient(position));
        }
        float fraction = (temperature + Tuning.BAR_LIMIT) / (2f * Tuning.BAR_LIMIT);
        int markerX = left + 1 + Math.round(Math.max(0f, Math.min(1f, fraction)) * (INNER_WIDTH - 1));
        graphics.fill(markerX - 1, top - 1, markerX + 2, top + HEIGHT + 1, FRAME);
        graphics.fill(markerX, top - 1, markerX + 1, top + HEIGHT + 1, MARKER);
    }

    private static int gradient(float position) {
        if (position < 0.5f) {
            return mix(FREEZING, NORMAL, position * 2f);
        }
        return mix(NORMAL, BURNING, (position - 0.5f) * 2f);
    }

    private static int mix(int from, int to, float t) {
        int a = 0xFF;
        int r = channel(from, to, t, 16);
        int g = channel(from, to, t, 8);
        int b = channel(from, to, t, 0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, float t, int shift) {
        int start = (from >> shift) & 0xFF;
        int end = (to >> shift) & 0xFF;
        return Math.round(start + (end - start) * t);
    }
}
