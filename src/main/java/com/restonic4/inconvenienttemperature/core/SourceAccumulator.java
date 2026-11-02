package com.restonic4.inconvenienttemperature.core;

public final class SourceAccumulator {
    private float directHeat;
    private float directCold;
    private final float[] groupSums;
    private final float[] groupCaps;

    public SourceAccumulator(float[] groupCaps) {
        this.groupCaps = groupCaps;
        this.groupSums = new float[groupCaps.length];
    }

    public void add(int group, float value) {
        if (group >= 0 && group < groupSums.length) {
            groupSums[group] += value;
        } else if (value > 0f) {
            directHeat += value;
        } else {
            directCold += value;
        }
    }

    public float heat() {
        float total = directHeat;
        for (int i = 0; i < groupSums.length; i++) {
            if (groupSums[i] > 0f) {
                total += Math.min(groupSums[i], groupCaps[i]);
            }
        }
        return total;
    }

    public float cold() {
        float total = directCold;
        for (int i = 0; i < groupSums.length; i++) {
            if (groupSums[i] < 0f) {
                total += Math.max(groupSums[i], -groupCaps[i]);
            }
        }
        return total;
    }
}
