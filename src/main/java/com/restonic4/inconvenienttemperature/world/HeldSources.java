package com.restonic4.inconvenienttemperature.world;

import com.restonic4.inconvenienttemperature.ModTags;
import com.restonic4.inconvenienttemperature.core.HeatFalloff;
import com.restonic4.inconvenienttemperature.config.Tuning;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public record HeldSources(float radiantHeat, float radiantCold, float roomHeat, float roomCold) {
    public static final HeldSources NONE = new HeldSources(0f, 0f, 0f, 0f);

    public static HeldSources of(LivingEntity holder) {
        float radiantHeat = 0f;
        float radiantCold = 0f;
        float roomHeat = 0f;
        float roomCold = 0f;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = holder.getItemInHand(hand);
            if (stack.isEmpty() || !stack.is(ModTags.HELD_HEAT_SOURCES) || !(stack.getItem() instanceof BlockItem item)) {
                continue;
            }
            HeatSourceRegistry.Value value = HeatSourceRegistry.value(item.getBlock().defaultBlockState());
            float radiant = HeatFalloff.contribution(value.strength(), Tuning.HELD_SOURCE_DISTANCE, 1f);
            if (radiant > 0f) {
                radiantHeat += radiant;
            } else {
                radiantCold += radiant;
            }
            if (value.roomPower() > 0f) {
                roomHeat += value.roomPower();
            } else {
                roomCold += value.roomPower();
            }
        }
        return new HeldSources(radiantHeat, radiantCold, roomHeat, roomCold);
    }
}
