package com.restonic4.inconvenienttemperature.world;

import com.restonic4.inconvenienttemperature.ModTags;
import com.restonic4.inconvenienttemperature.core.RoomWorld;
import com.restonic4.inconvenienttemperature.config.Tuning;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LevelRoomWorld implements RoomWorld {
    private static final Map<BlockState, Cell> CELLS = new ConcurrentHashMap<>();

    private final Level level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    public LevelRoomWorld(Level level) {
        this.level = level;
    }

    public static void clearCache() {
        CELLS.clear();
    }

    @Override
    public void sample(int x, int y, int z, Sample out) {
        if (y < level.getMinBuildHeight()) {
            out.cell = Cell.SEALED;
            out.power = 0f;
            return;
        }
        if (y >= level.getMaxBuildHeight()) {
            out.cell = Cell.OPEN;
            out.power = 0f;
            return;
        }
        pos.set(x, y, z);
        if (!level.isLoaded(pos)) {
            out.cell = Cell.UNLOADED;
            out.power = 0f;
            return;
        }
        BlockState state = level.getBlockState(pos);
        out.cell = CELLS.computeIfAbsent(state, candidate -> classify(candidate, pos));
        out.power = powerOf(state);
    }

    @Override
    public boolean skyExposed(int x, int y, int z) {
        pos.set(x, y, z);
        if (!level.isLoaded(pos)) {
            return false;
        }
        return y >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    private float powerOf(BlockState state) {
        float power = HeatSources.roomPower(state);
        if (power == 0f) {
            return 0f;
        }
        if (HeatSources.isLava(state) && !HeatSources.isExposed(level, pos, state, scratch)) {
            return 0f;
        }
        return power;
    }

    private Cell classify(BlockState state, BlockPos at) {
        if (state.isAir()) {
            return Cell.OPEN;
        }
        boolean openable = state.hasProperty(BlockStateProperties.OPEN);
        if (openable && state.getValue(BlockStateProperties.OPEN)) {
            return Cell.OPEN;
        }
        if (state.is(ModTags.AIRTIGHT)) {
            return Cell.SEALED;
        }
        if (state.is(ModTags.LEAKY)) {
            return Cell.LEAKY;
        }
        if (openable) {
            return Cell.SEALED;
        }
        if (state.is(ModTags.OPEN)) {
            return Cell.OPEN;
        }
        VoxelShape shape = state.getCollisionShape(level, at);
        if (shape.isEmpty()) {
            return Cell.OPEN;
        }
        AABB bounds = shape.bounds();
        double volume = bounds.getXsize() * bounds.getYsize() * bounds.getZsize();
        return volume >= Tuning.ROOM_SOLID_BOUNDS_VOLUME ? Cell.SEALED : Cell.OPEN;
    }
}
