package com.restonic4.inconvenienttemperature.client.view;

import com.restonic4.inconvenienttemperature.core.Room;
import com.restonic4.inconvenienttemperature.core.RoomAnalyzer;
import com.restonic4.inconvenienttemperature.core.RoomListener;
import com.restonic4.inconvenienttemperature.core.RoomWorld;
import com.restonic4.inconvenienttemperature.world.LevelRoomWorld;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class RoomSnapshot {
    record Leak(int x, int y, int z, int dx, int dy, int dz, RoomListener.Face kind) { }

    record Source(int x, int y, int z, float power) { }

    static final RoomSnapshot EMPTY = new RoomSnapshot(Room.NONE, Set.of(), List.of(), List.of(), List.of());

    final Room room;
    final Set<Long> cells;
    final List<BlockPos> floors;
    final List<Leak> leaks;
    final List<Source> sources;

    private RoomSnapshot(Room room, Set<Long> cells, List<BlockPos> floors, List<Leak> leaks, List<Source> sources) {
        this.room = room;
        this.cells = cells;
        this.floors = floors;
        this.leaks = leaks;
        this.sources = sources;
    }

    static RoomSnapshot capture(Level level, Vec3 eye, BlockPos feet) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().ultraWarm()) return EMPTY;

        LevelRoomWorld world = new LevelRoomWorld(level);
        Recorder recorder = new Recorder(world);
        BlockPos eyePos = BlockPos.containing(eye);
        Room room = RoomAnalyzer.analyze(world, eyePos.getX(), eyePos.getY(), eyePos.getZ(), recorder);
        if (room == Room.NONE) {
            recorder = new Recorder(world);
            room = RoomAnalyzer.analyze(world, feet.getX(), feet.getY(), feet.getZ(), recorder);
        }
        return new RoomSnapshot(room, recorder.cells, recorder.floors, recorder.leaks, recorder.sources);
    }

    private static final class Recorder implements RoomListener {
        final Set<Long> cells = new HashSet<>();
        final List<BlockPos> floors = new ArrayList<>();
        final List<Leak> leaks = new ArrayList<>();
        final List<Source> sources = new ArrayList<>();
        private final RoomWorld world;
        private final RoomWorld.Sample sample = new RoomWorld.Sample();

        Recorder(RoomWorld world) {
            this.world = world;
        }

        @Override
        public void cell(int x, int y, int z) {
            cells.add(RoomAnalyzer.pack(x, y, z));
            world.sample(x, y - 1, z, sample);
            if (sample.cell != RoomWorld.Cell.OPEN) {
                floors.add(new BlockPos(x, y, z));
            }
        }

        @Override
        public void face(int x, int y, int z, int dx, int dy, int dz, Face kind) {
            if (kind != Face.WALL) {
                leaks.add(new Leak(x, y, z, dx, dy, dz, kind));
            }
        }

        @Override
        public void source(int x, int y, int z, float power) {
            sources.add(new Source(x, y, z, power));
        }
    }
}
