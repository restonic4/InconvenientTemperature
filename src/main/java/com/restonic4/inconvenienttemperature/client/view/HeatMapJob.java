package com.restonic4.inconvenienttemperature.client.view;

import com.restonic4.inconvenienttemperature.core.HeatFalloff;
import com.restonic4.inconvenienttemperature.core.HeatPalette;
import com.restonic4.inconvenienttemperature.core.RoomAnalyzer;
import com.restonic4.inconvenienttemperature.core.RoomWorld;
import com.restonic4.inconvenienttemperature.config.Tuning;
import com.restonic4.inconvenienttemperature.world.HeldSources;
import com.restonic4.inconvenienttemperature.world.LevelRoomWorld;
import com.restonic4.inconvenienttemperature.world.SourceField;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class HeatMapJob {
    static final int RADIUS = 14;
    private static final int UP = 6;
    private static final int DOWN = 8;
    private static final int SIDE = 2 * RADIUS + 1;

    private final Level level;
    private final BlockPos origin;
    private final RoomSnapshot room;
    private final HeldSources held;
    private final SourceField field;
    private final LevelRoomWorld world;
    private final RoomWorld.Sample sample = new RoomWorld.Sample();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final List<HeatMap.Tile> tiles = new ArrayList<>();
    private int next;

    HeatMapJob(Level level, Vec3 playerPosition, RoomSnapshot room, HeldSources held) {
        this.level = level;
        this.origin = BlockPos.containing(playerPosition);
        this.room = room;
        this.held = held;
        this.world = new LevelRoomWorld(level);
        this.field = SourceField.collect(level, playerPosition, RADIUS + Tuning.SCAN_RADIUS);
    }

    BlockPos origin() {
        return origin;
    }

    boolean step(long budgetNanos) {
        long end = System.nanoTime() + budgetNanos;
        while (next < SIDE * SIDE && System.nanoTime() < end) {
            int dx = next % SIDE - RADIUS;
            int dz = next / SIDE - RADIUS;
            next++;
            if (dx * dx + dz * dz <= RADIUS * RADIUS) {
                column(origin.getX() + dx, origin.getZ() + dz);
            }
        }
        return next >= SIDE * SIDE;
    }

    HeatMap result() {
        return new HeatMap(List.copyOf(tiles));
    }

    private void column(int x, int z) {
        int top = origin.getY() + UP;
        int bottom = origin.getY() - DOWN;
        boolean above = isOpen(x, top + 1, z);
        boolean here = isOpen(x, top, z);
        for (int y = top; y >= bottom; y--) {
            boolean below = isOpen(x, y - 1, z);
            if (here && above && !below && !isFluid(x, y, z)) {
                tile(x, y, z);
            }
            above = here;
            here = below;
        }
    }

    private void tile(int x, int y, int z) {
        Vec3 center = new Vec3(x + 0.5, y + 0.9, z + 0.5);
        Vec3 eye = new Vec3(x + 0.5, y + 1.62, z + 0.5);
        SourceField.Evaluation nearby = field.evaluate(level, center, eye);
        boolean inRoom = room.cells.contains(RoomAnalyzer.pack(x, y, z));
        float retention = room.room.retention();
        float heat = nearby.heat() + held.radiantHeat()
                + (inRoom ? room.room.heatGain() + held.roomHeat() * retention : 0f);
        float cold = nearby.cold() + held.radiantCold()
                + (inRoom ? room.room.coldGain() + held.roomCold() * retention : 0f);
        int argb = HeatPalette.argb(HeatFalloff.capped(heat) + HeatFalloff.capped(cold));
        if (argb != 0) {
            tiles.add(new HeatMap.Tile(x, y, z, argb));
        }
    }

    private boolean isOpen(int x, int y, int z) {
        world.sample(x, y, z, sample);
        return sample.cell == RoomWorld.Cell.OPEN;
    }

    private boolean isFluid(int x, int y, int z) {
        return !level.getBlockState(probe.set(x, y, z)).getFluidState().isEmpty();
    }
}
