package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class ThermalEffects {
    private ThermalEffects() {}

    public static float visualIntensity(float body) {
        return ramp(Math.abs(body), Tuning.VISUAL_START, Tuning.BAR_LIMIT);
    }

    public static float speedMultiplier(float body) {
        float t = ramp(Math.abs(body), Tuning.SLOW_START, Tuning.BAR_LIMIT);
        return 1f - (1f - Tuning.SLOWEST_SPEED_FRACTION) * t * t;
    }

    public static boolean damaging(float body) {
        return Math.abs(body) >= Tuning.DAMAGE_START;
    }

    public static float damageProgressPerTick(float body) {
        if (!damaging(body)) return 0f;
        float t = ramp(Math.abs(body), Tuning.DAMAGE_START, Tuning.BAR_LIMIT);
        float interval = Tuning.DAMAGE_SLOWEST_INTERVAL_TICKS
                + (Tuning.DAMAGE_FASTEST_INTERVAL_TICKS - Tuning.DAMAGE_SLOWEST_INTERVAL_TICKS) * t;
        return 1f / interval;
    }

    public static boolean shouldIgnite(boolean carryingLava, boolean extinguished, int remainingFireTicks) {
        return carryingLava && !extinguished && remainingFireTicks <= Tuning.LAVA_CARRY_RELIGHT_AT_TICKS;
    }

    private static float ramp(float value, float from, float to) {
        return Math.max(0f, Math.min(1f, (value - from) / (to - from)));
    }
}
