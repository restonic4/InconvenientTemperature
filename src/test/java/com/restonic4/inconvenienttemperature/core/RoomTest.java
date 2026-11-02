package com.restonic4.inconvenienttemperature.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restonic4.inconvenienttemperature.config.Tuning;
import com.restonic4.inconvenienttemperature.core.RoomWorld.Cell;
import org.junit.jupiter.api.Test;

class RoomTest {
    private static VoxelWorld hut() {
        return new VoxelWorld(0).hollow(0, 1, 0, 4, 3, 4, Cell.SEALED);
    }

    private static Room inHut(VoxelWorld world) {
        return RoomAnalyzer.analyze(world, 2, 1, 2);
    }

    @Test
    void standingOutdoorsIsOpenAndGainsNothingFromRoomHeat() {
        VoxelWorld field = new VoxelWorld(0).power(5, 1, 5, 40f);
        Room room = RoomAnalyzer.analyze(field, 2, 1, 2);
        assertTrue(room.openness() > 0.95f, "openness=" + room.openness());
        assertEquals(0f, room.heatGain(), 1e-3f);
    }

    @Test
    void aSealedHutIsFullyInsulated() {
        Room room = inHut(hut());
        assertEquals(75, room.cells());
        assertEquals(0f, room.openness(), 0f);
        assertEquals(Tuning.ROOM_AMBIENT_MIN, AmbientModel.insulation(room.openness()), 1e-6f);
    }

    @Test
    void aCampfireInASealedHutWarmsItComfortably() {
        VoxelWorld world = hut().power(2, 1, 2, ShippedSources.CAMPFIRE_POWER);
        Room room = inHut(world);
        assertEquals(ShippedSources.CAMPFIRE_POWER, room.heatPower(), 0f);
        assertTrue(room.heatGain() > 18f && room.heatGain() < 26f, "gain=" + room.heatGain());
    }

    @Test
    void anOpenDoorKeepsMostButNotAllOfTheHeat() {
        VoxelWorld sealed = hut().power(2, 1, 2, ShippedSources.CAMPFIRE_POWER);
        VoxelWorld doorway = hut().power(2, 1, 2, ShippedSources.CAMPFIRE_POWER).set(2, 1, -1, Cell.OPEN).set(2, 2, -1, Cell.OPEN);
        Room closed = inHut(sealed);
        Room opened = inHut(doorway);
        assertTrue(opened.openness() > 0.2f && opened.openness() < 0.45f, "openness=" + opened.openness());
        assertTrue(opened.heatGain() < closed.heatGain());
        assertTrue(opened.heatGain() > closed.heatGain() * 0.55f);
    }

    @Test
    void aRoofHoleLeaksLessThanAMissingRoof() {
        Room hole = inHut(hut().set(2, 4, 2, Cell.OPEN));
        VoxelWorld noRoof = hut();
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                noRoof.set(x, 4, z, Cell.OPEN);
            }
        }
        Room none = inHut(noRoof);
        assertTrue(hole.openness() < 0.3f, "hole=" + hole.openness());
        assertTrue(none.openness() > 0.8f, "none=" + none.openness());
    }

    @Test
    void standingUnderTheRoofHoleStillCountsAsInsideTheHut() {
        VoxelWorld world = hut().set(2, 4, 2, Cell.OPEN);
        Room room = RoomAnalyzer.analyze(world, 2, 3, 2);
        assertEquals(76, room.cells());
        assertTrue(room.openness() < 0.3f);
    }

    @Test
    void ironBarsLeakMoreThanGlassButLessThanAnOpening() {
        Room glass = inHut(hut().set(-1, 2, 2, Cell.SEALED));
        Room bars = inHut(hut().set(-1, 2, 2, Cell.LEAKY));
        Room hole = inHut(hut().set(-1, 2, 2, Cell.OPEN));
        assertTrue(glass.openness() < bars.openness());
        assertTrue(bars.openness() < hole.openness());
    }

    @Test
    void aBiggerRoomDilutesTheSameHeat() {
        VoxelWorld small = new VoxelWorld(0).hollow(0, 1, 0, 4, 3, 4, Cell.SEALED).power(2, 1, 2, ShippedSources.CAMPFIRE_POWER);
        VoxelWorld large = new VoxelWorld(0).hollow(0, 1, 0, 14, 4, 14, Cell.SEALED).power(2, 1, 2, ShippedSources.CAMPFIRE_POWER);
        Room a = RoomAnalyzer.analyze(small, 2, 1, 2);
        Room b = RoomAnalyzer.analyze(large, 2, 1, 2);
        assertTrue(b.heatGain() < a.heatGain() * 0.5f, "small=" + a.heatGain() + " large=" + b.heatGain());
    }

    @Test
    void aSolidSourceBesideManyRoomCellsIsCountedOnce() {
        VoxelWorld world = hut().power(5, 2, 2, 20f).set(5, 2, 2, Cell.SEALED);
        Room room = inHut(world);
        assertEquals(20f, room.heatPower(), 0f);
    }

    @Test
    void aSourceOutsideTheWallsIsNotCounted() {
        VoxelWorld world = hut().power(8, 1, 8, 40f);
        assertEquals(0f, inHut(world).heatPower(), 0f);
    }

    @Test
    void coldSourcesAreTrackedSeparately() {
        VoxelWorld world = hut().power(2, 1, 2, ShippedSources.CAMPFIRE_POWER).power(1, 1, 1, -10f);
        Room room = inHut(world);
        assertEquals(ShippedSources.CAMPFIRE_POWER, room.heatPower(), 0f);
        assertEquals(-10f, room.coldPower(), 0f);
        assertTrue(room.coldGain() < 0f);
    }

    @Test
    void aLavaBucketInACarvedCaveIsDeadlyButNotInAHugeCavern() {
        VoxelWorld cave = new VoxelWorld(0).hollow(0, 1, 0, 9, 4, 6, Cell.SEALED);
        VoxelWorld cavern = new VoxelWorld(0).hollow(0, 1, 0, 29, 12, 29, Cell.SEALED);
        for (int i = 0; i < 8; i++) {
            cave.power(i, 1, 0, ShippedSources.LAVA_POWER);
            cavern.power(i, 1, 0, ShippedSources.LAVA_POWER);
        }
        Room carved = RoomAnalyzer.analyze(cave, 5, 2, 3);
        Room large = RoomAnalyzer.analyze(cavern, 5, 2, 15);
        assertTrue(carved.heatGain() > Tuning.DAMAGE_START, "carved=" + carved.heatGain());
        assertTrue(large.heatGain() < Tuning.VISUAL_START, "cavern=" + large.heatGain());
    }

    @Test
    void aNarrowTunnelToTheSurfaceBarelyLeaks() {
        VoxelWorld world = new VoxelWorld(0).hollow(0, 1, 0, 8, 3, 8, Cell.SEALED);
        world.box(9, 0, 3, 41, 2, 5, Cell.SEALED).box(9, 1, 4, 41, 1, 4, Cell.OPEN);
        Room room = RoomAnalyzer.analyze(world, 4, 1, 4);
        assertTrue(room.openness() < 0.45f, "openness=" + room.openness());
        assertTrue(room.cells() > 243);
    }

    @Test
    void startingInsideASolidBlockYieldsNoRoom() {
        assertEquals(Room.NONE, RoomAnalyzer.analyze(hut(), -1, 1, 2));
    }

    @Test
    void theCellBudgetBoundsTheSearch() {
        VoxelWorld bigHall = new VoxelWorld(0).hollow(0, 1, 0, 60, 30, 60, Cell.SEALED);
        Room room = RoomAnalyzer.analyze(bigHall, 30, 1, 30);
        assertTrue(room.cells() <= Tuning.ROOM_MAX_CELLS);
        assertTrue(room.openness() > 0.5f, "openness=" + room.openness());
    }

    @Test
    void aClosedHutInSnowyPlainsIsComfortableWhileOutsideIsNot() {
        float outside = AmbientModel.environmentTarget(
                new AmbientModel.Inputs(0f, false, false, false, 70f, 1f, 0f));
        float inside = AmbientModel.environmentTarget(
                new AmbientModel.Inputs(0f, false, false, false, 70f, 0f, 0f));
        assertTrue(Math.abs(inside) < Tuning.DEAD_ZONE, "inside=" + inside);
        assertTrue(outside < -Tuning.VISUAL_START);
    }

    private static final class Recorder implements RoomListener {
        int cells;
        int outlets;
        int leaky;
        int walls;
        float heat;
        float cold;

        @Override
        public void cell(int x, int y, int z) {
            cells++;
        }

        @Override
        public void face(int x, int y, int z, int dx, int dy, int dz, Face kind) {
            switch (kind) {
                case OUTLET -> outlets++;
                case LEAKY -> leaky++;
                case WALL -> walls++;
            }
        }

        @Override
        public void source(int x, int y, int z, float power) {
            if (power > 0f) {
                heat += power;
            } else {
                cold += power;
            }
        }
    }

    @Test
    void theListenerSeesExactlyWhatTheRoomTotalsReport() {
        VoxelWorld world = hut().power(2, 1, 2, 40f).power(5, 2, 2, 20f).power(1, 1, 1, -10f)
                .set(2, 1, -1, Cell.OPEN).set(2, 2, -1, Cell.OPEN).set(-1, 2, 4, Cell.LEAKY);
        Recorder recorder = new Recorder();
        Room room = RoomAnalyzer.analyze(world, 2, 1, 2, recorder);
        assertEquals(room.cells(), recorder.cells);
        assertEquals(room.openFaces(), recorder.outlets);
        assertEquals(room.leakyFaces(), recorder.leaky);
        assertEquals(room.wallFaces(), recorder.walls);
        assertEquals(room.heatPower(), recorder.heat, 1e-4f);
        assertEquals(room.coldPower(), recorder.cold, 1e-4f);
    }

    @Test
    void analysingWithAListenerGivesTheSameRoomAsWithout() {
        VoxelWorld world = hut().power(2, 1, 2, 40f).set(2, 4, 2, Cell.OPEN);
        assertEquals(RoomAnalyzer.analyze(world, 2, 1, 2), RoomAnalyzer.analyze(world, 2, 1, 2, new Recorder()));
    }

    @Test
    void thePaletteIsInvisibleBelowTheThresholdAndWarmsWithHeat() {
        assertEquals(0, HeatPalette.argb(1.9f));
        assertEquals(0, HeatPalette.argb(-1.9f));
        int warm = HeatPalette.argb(30f);
        int hot = HeatPalette.argb(200f);
        int cool = HeatPalette.argb(-30f);
        assertEquals(HeatPalette.ALPHA, warm >>> 24);
        assertTrue(((warm >> 16) & 0xFF) > ((warm) & 0xFF), "warm should be red-ish");
        assertTrue(((cool) & 0xFF) > ((cool >> 16) & 0xFF), "cold should be blue-ish");
        assertTrue(((hot >> 16) & 0xFF) < ((warm >> 16) & 0xFF), "extreme heat is darker red");
    }
}
