package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class Activity {
    private Activity() {}

    public static float sprintWarmth(float target, boolean sprinting) {
        if (!sprinting || target >= 0f) return 0f;
        return Math.min(Tuning.SPRINT_WARMTH, -target);
    }
}
