package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class WoolInsulation {
    public static final int SLOTS = 4;

    private WoolInsulation() {}

    public static float advanceWarmup(float warmup, boolean wearingWool) {
        if (!wearingWool) return 0f;
        return Math.min(1f, warmup + 1f / Tuning.WOOL_WARMUP_TICKS);
    }

    public static float insulation(float[] warmup) {
        float total = 0f;
        for (int i = 0; i < SLOTS; i++) {
            total += Tuning.WOOL_SLOT_WEIGHTS[i] * warmup[i];
        }
        return total;
    }

	public static float warmthPerInsulation(float weatherTarget) {
		float coldness = Math.max(0f, Math.min(1f, -weatherTarget / Tuning.WOOL_COLD_BONUS_FULL_AT));

		return Tuning.WOOL_WARMTH + Tuning.WOOL_COLD_BONUS * coldness;
	}

	public static float warmth(float insulation, float weatherTarget) {
		return insulation * warmthPerInsulation(weatherTarget);
	}
}
