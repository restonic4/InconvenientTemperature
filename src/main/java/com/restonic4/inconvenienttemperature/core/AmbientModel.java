package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class AmbientModel {
    public record Inputs(
		float biomeTemperature,
		boolean ultraWarmDimension,
		boolean night,
		boolean raining,
		float eyeY,
		float openness,
		float snowiness
	) {}

    private AmbientModel() {}

	public record Breakdown(
		float biomeBase,
		float snowShift,
		float night,
		float altitude,
		float rain,
		float outdoor,
		float roomFactor,
		float depthFactor,
		boolean ultraWarm,
		float total
	) {}

	public static float environmentTarget(Inputs in) {
		return breakdown(in).total();
	}

	public static Breakdown breakdown(Inputs in) {
		if (in.ultraWarmDimension()) {
			return new Breakdown(0f, 0f, 0f, 0f, 0f, Tuning.ULTRAWARM_BASE, 1f, 1f, true, Tuning.ULTRAWARM_BASE);
		}

		float biomeBase = biomeBase(in);
		float snowShift = climateBase(in) - biomeBase;
		float night = timeOffset(in);
		float altitude = altitudeOffset(in);
		float rain = in.raining() ? Tuning.RAIN_OFFSET : 0f;
		float outdoor = biomeBase + snowShift + night + altitude + rain;
		float roomFactor = insulation(in.openness());
		float depthFactor = depth(in.eyeY());

		return new Breakdown(
			biomeBase, snowShift, night, altitude, rain, outdoor, roomFactor, depthFactor, false,
			outdoor * roomFactor * depthFactor
		);
	}

	private static float biomeBase(Inputs in) {
		return fromComfort(clamp(in.biomeTemperature(), Tuning.BIOME_MIN, Tuning.BIOME_MAX));
	}

	static float climateBase(Inputs in) {
		float base = biomeBase(in);
		float snowy = fromComfort(Tuning.SNOWY_BIOME_TEMPERATURE);
		if (base <= snowy) return base;

		float pull = clamp(in.snowiness(), 0f, 1f) * Tuning.SNOW_MAX_INFLUENCE;
		return base - (base - snowy) * pull;
	}

    private static float fromComfort(float biomeTemperature) {
        float delta = biomeTemperature - Tuning.BIOME_COMFORT;
        return delta * (delta < 0f ? Tuning.BIOME_COLD_SCALE : Tuning.BIOME_HOT_SCALE);
    }

    static float timeOffset(Inputs in) {
        return in.night() ? Tuning.NIGHT_OFFSET : 0f;
    }

    static float altitudeOffset(Inputs in) {
        float over = in.eyeY() - Tuning.COLD_ALTITUDE_START_Y;
        if (over <= 0f) return 0f;
        return -Math.min(Tuning.COLD_ALTITUDE_MAX, over * Tuning.COLD_ALTITUDE_PER_BLOCK);
    }

    static float insulation(float openness) {
        return Tuning.ROOM_AMBIENT_MIN + (1f - Tuning.ROOM_AMBIENT_MIN) * clamp(openness, 0f, 1f);
    }

    static float depth(float eyeY) {
        if (eyeY >= Tuning.DEPTH_FULL_Y) return 1f;
        float t = (eyeY - Tuning.DEPTH_FADE_BOTTOM_Y) / Tuning.DEPTH_FADE_BLOCKS;
        return clamp(t, Tuning.DEPTH_MIN, 1f);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
