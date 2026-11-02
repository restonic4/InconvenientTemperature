package com.restonic4.inconvenienttemperature.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restonic4.inconvenienttemperature.config.Tuning;
import org.junit.jupiter.api.Test;

class IgnitionTest {
    @Test
    void carryingLavaLightsAPlayerWhoIsNotBurning() {
        assertTrue(ThermalEffects.shouldIgnite(true, false, 0));
        assertTrue(ThermalEffects.shouldIgnite(true, false, -1));
    }

    @Test
    void notCarryingLavaNeverLights() {
        assertFalse(ThermalEffects.shouldIgnite(false, false, 0));
    }

    @Test
    void waterOrRainKeepsItOut() {
        assertFalse(ThermalEffects.shouldIgnite(true, true, 0));
    }

    @Test
    void aFlameWithTimeLeftIsLeftAlone() {
        assertTrue(ThermalEffects.shouldIgnite(true, false, Tuning.LAVA_CARRY_RELIGHT_AT_TICKS));
        assertFalse(ThermalEffects.shouldIgnite(true, false, Tuning.LAVA_CARRY_RELIGHT_AT_TICKS + 1));
    }

    @Test
    void carryingLavaBurnsLikeLavaWithoutDoubleDamageOrFlicker() {
        int remaining = 0;
        int hits = 0;
        int ticks = 1200;
        for (int tick = 0; tick < ticks; tick++) {
            if (remaining > 0) {
                if (remaining % 20 == 0) {
                    hits++;
                }
                remaining--;
            }
            if (ThermalEffects.shouldIgnite(true, false, remaining)) {
                remaining = Math.max(remaining, Tuning.LAVA_CARRY_BURN_TICKS);
            }

            assertTrue(remaining > 0, "flame out at tick " + tick);
        }

        assertEquals(ticks / 20, hits, 1);
    }
}
