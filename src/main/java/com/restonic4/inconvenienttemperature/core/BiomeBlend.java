package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class BiomeBlend {
    private static final int[][] OFFSETS = buildOffsets();
    private static final float[] WEIGHTS = buildWeights();

    private BiomeBlend() {}

    public static int sampleCount() {
        return OFFSETS.length;
    }

    public static int offsetX(int sample) {
        return OFFSETS[sample][0];
    }

    public static int offsetZ(int sample) {
        return OFFSETS[sample][1];
    }

    public static float blend(float[] temperatures) {
        float weighted = 0f;
        float total = 0f;
        for (int i = 0; i < WEIGHTS.length; i++) {
            weighted += WEIGHTS[i] * temperatures[i];
            total += WEIGHTS[i];
        }
        return weighted / total;
    }

    private static int[][] buildOffsets() {
        int directions = Tuning.BIOME_BLEND_DIRECTIONS;
        int[][] offsets = new int[1 + Tuning.BIOME_BLEND_RINGS.length * directions][];
        offsets[0] = new int[] {0, 0};
        int index = 1;
        for (int ring : Tuning.BIOME_BLEND_RINGS) {
            for (int d = 0; d < directions; d++) {
                double angle = 2 * Math.PI * d / directions;
                offsets[index++] = new int[] {
                    (int) Math.round(Math.cos(angle) * ring), (int) Math.round(Math.sin(angle) * ring)
                };
            }
        }
        return offsets;
    }

    private static float[] buildWeights() {
        float[] weights = new float[OFFSETS.length];
        weights[0] = Tuning.BIOME_BLEND_CENTER_WEIGHT;
        for (int i = 1; i < weights.length; i++) {
            double radius = Math.hypot(OFFSETS[i][0], OFFSETS[i][1]);
            double scaled = radius / Tuning.BIOME_BLEND_FALLOFF;
            weights[i] = (float) (1.0 / (1.0 + scaled * scaled));
        }
        return weights;
    }
}
