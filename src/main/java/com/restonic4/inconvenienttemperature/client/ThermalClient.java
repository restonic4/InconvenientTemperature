package com.restonic4.inconvenienttemperature.client;

import com.restonic4.inconvenienttemperature.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ThermalClient {
    private static final int XP_BAR_VISIBLE_TICKS = 60;
    private static final float SMOOTHING = 0.2f;

    private static LocalPlayer trackedPlayer;
    private static int lastLevel;
    private static int lastTotal;
    private static float lastProgress;
    private static int xpTicksLeft;
    private static float displayed;

    private ThermalClient() {}

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            trackedPlayer = null;
            return;
        }

        float actual = player.getAttachedOrElse(ModAttachments.BODY_TEMPERATURE, 0f);
        if (player != trackedPlayer) {
            trackedPlayer = player;
            displayed = actual;
            xpTicksLeft = 0;
        } else {
            float gap = actual - displayed;
            displayed = Math.abs(gap) < 0.02f ? actual : displayed + gap * SMOOTHING;
            if (player.experienceLevel != lastLevel
				|| player.totalExperience != lastTotal
				|| player.experienceProgress != lastProgress
			) {
                xpTicksLeft = XP_BAR_VISIBLE_TICKS;
            }
        }
        lastLevel = player.experienceLevel;
        lastTotal = player.totalExperience;
        lastProgress = player.experienceProgress;
        if (xpTicksLeft > 0) xpTicksLeft--;
    }

    public static boolean showsTemperatureBar() { return xpTicksLeft == 0; }
    public static float displayedTemperature() { return displayed; }
}
