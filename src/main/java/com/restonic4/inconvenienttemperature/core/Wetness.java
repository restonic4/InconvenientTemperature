package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class Wetness {
    public record State(float wetness, int rainExposure) {
        public static final State DRY = new State(0f, 0);
    }

    private Wetness() {}

    public static State advance(State state, boolean inWater, boolean underRain, float warmth) {
        int exposure = underRain
                ? Math.min(Tuning.WET_RAIN_EXPOSURE_TICKS * 2, state.rainExposure() + 1)
                : Math.max(0, state.rainExposure() - Tuning.WET_RAIN_EXPOSURE_DECAY_PER_TICK);
        float wetness = state.wetness();
        if (inWater) {
            wetness += Tuning.WET_WATER_RISE_PER_TICK;
        } else if (underRain && exposure >= Tuning.WET_RAIN_EXPOSURE_TICKS) {
            wetness += Tuning.WET_RAIN_RISE_PER_TICK;
        } else {
            wetness -= Tuning.WET_DRY_PER_TICK * dryingMultiplier(warmth);
        }
        return new State(Math.max(0f, Math.min(1f, wetness)), exposure);
    }

    public static float dryingMultiplier(float warmth) {
        float multiplier = 1f + warmth / Tuning.WET_DRY_WARMTH_SCALE;
        return Math.max(Tuning.WET_DRY_MIN_MULTIPLIER, Math.min(Tuning.WET_DRY_MAX_MULTIPLIER, multiplier));
    }

    public static float temperatureOffset(float wetness, float environment) {
        float coldness = Math.max(0f, Math.min(1f, -environment / Tuning.WET_COLD_FULL_AT));
        return -wetness * (Tuning.WET_COLD_BASE + Tuning.WET_COLD_EXTRA * coldness);
    }

    public static int remainingTicks(float wetness, float warmth) {
        float perTick = Tuning.WET_DRY_PER_TICK * dryingMultiplier(warmth);
        return Math.min(Tuning.WET_EFFECT_MAX_TICKS, (int) Math.ceil(wetness / perTick));
    }
}
