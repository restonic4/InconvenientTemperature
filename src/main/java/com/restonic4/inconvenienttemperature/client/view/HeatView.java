package com.restonic4.inconvenienttemperature.client.view;

import com.restonic4.inconvenienttemperature.config.Tuning;
import com.restonic4.inconvenienttemperature.core.HeatFalloff;
import com.restonic4.inconvenienttemperature.core.SourceMark;
import com.restonic4.inconvenienttemperature.core.SourceMarks;
import com.restonic4.inconvenienttemperature.core.ViewMode;
import com.restonic4.inconvenienttemperature.world.HeatSourceRegistry;
import com.restonic4.inconvenienttemperature.world.HeldSources;
import com.restonic4.inconvenienttemperature.world.SourceField;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class HeatView {
    record Readout(float nearbyHeat, float nearbyCold, float roomHeat, float roomCold, float total) {
        static final Readout NONE = new Readout(0f, 0f, 0f, 0f, 0f);
    }

    private static final long MAP_BUDGET_NANOS = 3_000_000L;
    private static final int MAP_REFRESH_TICKS = 40;
    private static final int ROOM_REFRESH_TICKS = 10;
    private static final double MAP_MOVE_LIMIT = 4.0;
    private static final int RADIANT_MARK_LIMIT = 24;

    private static ViewMode mode = ViewMode.OFF;
    private static boolean unavailable;
    private static RoomSnapshot room = RoomSnapshot.EMPTY;
    private static Readout readout = Readout.NONE;
    private static List<SourceMark> marks = List.of();
    private static HeatMap map = HeatMap.EMPTY;
    private static HeatMapJob job;
    private static int ticks;
    private static int mapStartedAt;

    private HeatView() {}

    public static ViewMode mode() {
        return mode;
    }

    public static void setMode(ViewMode value) {
        mode = value;
        reset();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            String note = value != ViewMode.OFF && HeatSourceRegistry.isEmpty() ? " (no heat source data on this client)" : "";
            player.displayClientMessage(Component.literal("Heat view: " + value.commandName() + note), true);
        }
    }

    public static void cycle() {
        setMode(mode.next());
    }

    static boolean unavailable() {
        return unavailable;
    }

    static RoomSnapshot room() {
        return room;
    }

    static Readout readout() {
        return readout;
    }

    static List<SourceMark> marks() {
        return marks;
    }

    static HeatMap map() {
        return map;
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (mode == ViewMode.OFF || player == null || minecraft.level == null) return;

        unavailable = HeatSourceRegistry.isEmpty();
        if (unavailable) return;

        Level level = minecraft.level;
        ticks++;
        if (ticks % ROOM_REFRESH_TICKS == 1) {
            room = RoomSnapshot.capture(level, player.getEyePosition(), player.blockPosition());
            refresh(level, player, room);
        }
        if (mode.heat()) {
            stepMap(level, player);
        }
    }

    private static void reset() {
        room = RoomSnapshot.EMPTY;
        readout = Readout.NONE;
        marks = List.of();
        map = HeatMap.EMPTY;
        job = null;
        ticks = 0;
        mapStartedAt = 0;
    }

    private static void stepMap(Level level, LocalPlayer player) {
        if (job == null) {
            boolean stale = ticks - mapStartedAt >= MAP_REFRESH_TICKS;
            if (stale || map == HeatMap.EMPTY) {
                job = new HeatMapJob(level, player.position(), room, HeldSources.of(player));
                mapStartedAt = ticks;
            }
        }
        if (job != null && job.step(MAP_BUDGET_NANOS)) {
            map = job.result();
            BlockPos origin = job.origin();
            job = null;
            if (player.position().distanceToSqr(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5) > MAP_MOVE_LIMIT * MAP_MOVE_LIMIT) {
                mapStartedAt = Integer.MIN_VALUE / 2;
            }
        }
    }

    private static void refresh(Level level, LocalPlayer player, RoomSnapshot snapshot) {
        Vec3 center = player.getBoundingBox().getCenter();
        SourceField field = SourceField.collect(level, center, Tuning.SCAN_RADIUS);
        readout = readoutAt(level, player, snapshot, field, center);
        marks = SourceMarks.merge(roomMarks(snapshot), radiantMarks(level, player, field, center), RADIANT_MARK_LIMIT);
    }

    private static List<SourceMark> roomMarks(RoomSnapshot snapshot) {
        if (!mode.rooms() || snapshot.outdoors) return List.of();

        return snapshot.sources.stream()
            .map(source -> new SourceMark(source.x(), source.y(), source.z(), 0f, 0f, source.power()))
            .toList();
    }

    private static List<SourceMark> radiantMarks(Level level, LocalPlayer player, SourceField field, Vec3 center) {
        if (!mode.heat()) return List.of();

        return field.contributions(level, center, player.getEyePosition()).stream()
            .map(c -> new SourceMark(c.x(), c.y(), c.z(), c.strength(), c.delivered(), 0f))
            .toList();
    }

    private static Readout readoutAt(Level level, LocalPlayer player, RoomSnapshot snapshot, SourceField field, Vec3 center) {
        SourceField.Evaluation nearby = field.evaluate(level, center, player.getEyePosition());
        HeldSources held = HeldSources.of(player);
        float retention = snapshot.room.retention();
        float nearbyHeat = nearby.heat() + held.radiantHeat();
        float nearbyCold = nearby.cold() + held.radiantCold();
        float roomHeat = snapshot.room.heatGain() + held.roomHeat() * retention;
        float roomCold = snapshot.room.coldGain() + held.roomCold() * retention;
        float total = HeatFalloff.capped(nearbyHeat + roomHeat) + HeatFalloff.capped(nearbyCold + roomCold);
        return new Readout(nearbyHeat, nearbyCold, roomHeat, roomCold, total);
    }
}
