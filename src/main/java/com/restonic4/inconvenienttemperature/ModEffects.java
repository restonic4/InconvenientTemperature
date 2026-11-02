package com.restonic4.inconvenienttemperature;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
    private static final int WET_COLOR = 0x3F76E4;

    public static final Holder<MobEffect> WET = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT,
		InconvenientTemperature.id("wet"),
		new WetEffect()
	);

    private static final class WetEffect extends MobEffect {
        WetEffect() {
            super(MobEffectCategory.NEUTRAL, WET_COLOR);
        }
    }

    private ModEffects() {}
    public static void register() {}
}
