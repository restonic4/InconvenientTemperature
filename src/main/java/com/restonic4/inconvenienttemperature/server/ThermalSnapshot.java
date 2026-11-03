package com.restonic4.inconvenienttemperature.server;

import com.restonic4.inconvenienttemperature.core.AmbientModel;
import com.restonic4.inconvenienttemperature.core.Room;
import com.restonic4.inconvenienttemperature.world.HeldSources;
import java.util.List;

public record ThermalSnapshot(
	float body,
	float finalTarget,
	float weather,
	AmbientModel.Breakdown ambient,
	String biomeName,
	float centerBiome,
	float blendedBiome,
	float snowiness,
	float eyeY,
	List<WoolPiece> wool,
	float insulation,
	float woolWarmth,
	float wetness,
	int dryTicks,
	float wetOffset,
	boolean sprinting,
	float sprintWarmth,
	Room room,
	float radiantHeat,
	float radiantCold,
	float burning,
	HeldSources held,
	float heatRaw,
	float coldRaw,
	float sourceDelta
) {
	public record WoolPiece(String slot, String item, float weight, float warmup) {
		public boolean worn() { return item != null; }

		public float share() { return weight * warmup; }
	}
}
