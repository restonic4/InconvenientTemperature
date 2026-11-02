package com.restonic4.inconvenienttemperature.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class HeatSources {
    private HeatSources() {}

    public static float strength(BlockState state) {
        return HeatSourceRegistry.value(state).strength();
    }

    public static float roomPower(BlockState state) {
        return HeatSourceRegistry.value(state).roomPower();
    }

    public static boolean isSource(BlockState state) {
        HeatSourceRegistry.Value value = HeatSourceRegistry.value(state);
        return value.strength() != 0f || value.roomPower() != 0f;
    }

    public static boolean isSnowCover(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean isRelevant(BlockState state) {
        return isSource(state) || isSnowCover(state);
    }

    public static boolean isLava(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean isExposed(Level level, BlockPos pos, BlockState state, BlockPos.MutableBlockPos scratch) {
        if (!isLava(state)) return true;
        for (Direction direction : Direction.values()) {
            BlockState next = level.getBlockState(scratch.setWithOffset(pos, direction));
            if (!isLava(next) && !next.canOcclude()) return true;
        }
        return false;
    }
}
