package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class BodyTemperature {
    private BodyTemperature() {}

	public static float perSecond(float body, float weatherTarget, float sourceDelta, float woolInsulation) {
		float rate = Math.min(
			Tuning.RATE_MAX_PER_SECOND,
			Tuning.RATE_MIN_PER_SECOND
				+ Tuning.RATE_PER_WEATHER_GAP * Math.abs(weatherTarget - body)
				+ Tuning.RATE_PER_SOURCE_UNIT * Math.abs(sourceDelta)
		);
		if (weatherTarget + sourceDelta - body > 0f) {
			rate *= heatingBonus(woolInsulation);
		}

		return rate;
	}

	public static float heatingBonus(float woolInsulation) {
		return 1f + Tuning.WOOL_HEATING_RATE_BONUS * woolInsulation;
	}

	public static float step(float body, float weatherTarget, float sourceDelta, float woolInsulation) {
		float gap = weatherTarget + sourceDelta - body;
		float perTick = perSecond(body, weatherTarget, sourceDelta, woolInsulation) / 20f;
		float moved = Math.max(-perTick, Math.min(perTick, gap));

		return clampToBar(body + moved);
	}

    public static float clampToBar(float value) {
        return Math.max(-Tuning.BAR_LIMIT, Math.min(Tuning.BAR_LIMIT, value));
    }
}
