package com.restonic4.inconvenienttemperature.server;

import com.restonic4.inconvenienttemperature.config.Tuning;
import com.restonic4.inconvenienttemperature.core.ThermalEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public final class CarriedLava {
    private CarriedLava() {}

    public static void tick(ServerPlayer player) {
        boolean carrying = player.getInventory().contains(stack -> stack.is(Items.LAVA_BUCKET));
        if (ThermalEffects.shouldIgnite(carrying, player.isInWaterRainOrBubble(), player.getRemainingFireTicks())) {
            player.igniteForTicks(Tuning.LAVA_CARRY_BURN_TICKS);
        }
    }
}
