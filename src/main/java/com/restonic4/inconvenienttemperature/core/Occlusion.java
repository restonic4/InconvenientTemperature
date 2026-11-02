package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class Occlusion {
    public interface Solids {
        boolean at(int x, int y, int z);
    }

    private Occlusion() {}

    public static float thickness(
		double fromX, double fromY, double fromZ,
		int targetX, int targetY, int targetZ, Solids solids
	) {
        double dx = targetX + 0.5 - fromX;
        double dy = targetY + 0.5 - fromY;
        double dz = targetZ + 0.5 - fromZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) (length / Tuning.OCCLUSION_STEP);
        if (steps < 2) return 0f;

        double scale = Tuning.OCCLUSION_STEP / length;
        int solid = 0;
        for (int i = 1; i < steps; i++) {
            int x = (int) Math.floor(fromX + dx * scale * i);
            int y = (int) Math.floor(fromY + dy * scale * i);
            int z = (int) Math.floor(fromZ + dz * scale * i);
            if (x == targetX && y == targetY && z == targetZ) continue;

            if (solids.at(x, y, z)) {
                solid++;
            }
        }
        return (float) (solid * Tuning.OCCLUSION_STEP);
    }

    public static float transmission(float thickness) {
        return Math.max(Tuning.OCCLUSION_FLOOR, (float) Math.exp(-thickness / Tuning.OCCLUSION_LENGTH));
    }
}
