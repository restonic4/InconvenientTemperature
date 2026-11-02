package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class HeatFalloff {
    private HeatFalloff() {}

    public static float contribution(float strength, float distance, float transmission) {
        float range = Tuning.SCAN_RADIUS;
        if (distance >= range) return 0f;
        float reach = 1f - distance / range;
        return strength * reach / (1f + distance / Tuning.SOURCE_FALLOFF_CORE) * transmission;
    }

    public static float capped(float sum) {
        if (sum >= 0f) return Math.min(sum, Tuning.HEAT_SOURCE_CAP);
        return Math.max(sum, -Tuning.COLD_SOURCE_CAP);
    }
}
