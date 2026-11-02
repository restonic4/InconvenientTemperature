package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public record Room(
	int cells,
	int openFaces,
	int leakyFaces,
	int wallFaces,
	float heatPower,
	float coldPower,
	boolean truncated
) {
    public static final Room NONE = new Room(0, 0, 0, 0, 0f, 0f, false);

    public float openness() {
        float leak = openFaces * Tuning.ROOM_OPEN_CONDUCTANCE + leakyFaces * Tuning.ROOM_LEAKY_CONDUCTANCE;
        float total = leak + wallFaces * Tuning.ROOM_WALL_CONDUCTANCE;
        return total <= 0f ? 1f : leak / total;
    }

    public float volumeFactor() {
        return Tuning.ROOM_REFERENCE_VOLUME / (cells + Tuning.ROOM_REFERENCE_VOLUME);
    }

    public float retention() {
        return (1f - openness()) * volumeFactor();
    }

    public float heatGain() {
        return heatPower * retention();
    }

    public float coldGain() {
        return coldPower * retention();
    }
}
