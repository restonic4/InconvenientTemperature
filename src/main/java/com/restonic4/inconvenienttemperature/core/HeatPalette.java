package com.restonic4.inconvenienttemperature.core;

public final class HeatPalette {
    public static final float VISIBLE_FROM = 2f;
    public static final int ALPHA = 0x60;

    private static final float[] STOPS = {VISIBLE_FROM, 10f, 25f, 70f, 150f};
    private static final int[] HEAT = {0xFFF59D, 0xFFEB3B, 0xFF9800, 0xF44336, 0xB71C1C};
    private static final int[] COLD = {0xE0F7FA, 0x80DEEA, 0x29B6F6, 0x3F51B5, 0x1A237E};

    private HeatPalette() {}

    public static int argb(float value) {
        float magnitude = Math.abs(value);
        if (magnitude < VISIBLE_FROM) return 0;

        int[] colors = value > 0f ? HEAT : COLD;
        int rgb = colors[colors.length - 1];
        for (int i = 1; i < STOPS.length; i++) {
            if (magnitude <= STOPS[i]) {
                float t = (magnitude - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]);
                rgb = mix(colors[i - 1], colors[i], t);
                break;
            }
        }
        return (ALPHA << 24) | rgb;
    }

    private static int mix(int from, int to, float t) {
        int r = channel(from, to, t, 16);
        int g = channel(from, to, t, 8);
        int b = channel(from, to, t, 0);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, float t, int shift) {
        int start = (from >> shift) & 0xFF;
        int end = (to >> shift) & 0xFF;
        return Math.round(start + (end - start) * t);
    }
}
