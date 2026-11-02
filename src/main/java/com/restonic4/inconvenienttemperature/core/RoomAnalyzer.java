package com.restonic4.inconvenienttemperature.core;

import com.restonic4.inconvenienttemperature.config.Tuning;

public final class RoomAnalyzer {
    private static final int[][] DIRECTIONS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private RoomAnalyzer() {}

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    private static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    private static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    public static Room analyze(RoomWorld world, int startX, int startY, int startZ) {
        return analyze(world, startX, startY, startZ, RoomListener.NONE);
    }

    public static Room analyze(RoomWorld world, int startX, int startY, int startZ, RoomListener listener) {
        RoomWorld.Sample sample = new RoomWorld.Sample();
        RoomWorld.Sample scratch = new RoomWorld.Sample();
        world.sample(startX, startY, startZ, sample);
        if (sample.cell != RoomWorld.Cell.OPEN) return Room.NONE;

        int capacity = Tuning.ROOM_MAX_CELLS;
        long[] queue = new long[capacity];
        float[] queuePower = new float[capacity];
        LongHashSet visited = new LongHashSet(capacity);
        LongHashSet counted = new LongHashSet(64);
        int head = 0;
        int tail = 0;
        long start = pack(startX, startY, startZ);
        visited.add(start);
        queuePower[tail] = sample.power;
        queue[tail++] = start;

        int open = 0;
        int leaky = 0;
        int walls = 0;
        float[] power = new float[2];
        boolean truncated = false;

        while (head < tail) {
            long current = queue[head];
            int cx = unpackX(current);
            int cy = unpackY(current);
            int cz = unpackZ(current);
            addPower(power, queuePower[head]);
            head++;
            listener.cell(cx, cy, cz);
            if (queuePower[head - 1] != 0f) {
                listener.source(cx, cy, cz, queuePower[head - 1]);
            }

            for (int[] d : DIRECTIONS) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                int nz = cz + d[2];
                world.sample(nx, ny, nz, sample);
                switch (sample.cell) {
                    case OPEN -> {
                        long key = pack(nx, ny, nz);
                        if (visited.contains(key)) continue;

                        boolean outOfRange = Math.abs(nx - startX) > Tuning.ROOM_MAX_RANGE
                                || Math.abs(ny - startY) > Tuning.ROOM_MAX_RANGE
                                || Math.abs(nz - startZ) > Tuning.ROOM_MAX_RANGE;
                        if (tail >= capacity) {
                            truncated = true;
                            open++;
                            listener.face(cx, cy, cz, d[0], d[1], d[2], RoomListener.Face.OUTLET);
                        } else if (outOfRange || isOutdoor(world, scratch, nx, ny, nz)) {
                            open++;
                            listener.face(cx, cy, cz, d[0], d[1], d[2], RoomListener.Face.OUTLET);
                        } else {
                            visited.add(key);
                            queuePower[tail] = sample.power;
                            queue[tail++] = key;
                        }
                    }
                    case SEALED -> {
                        walls++;
                        listener.face(cx, cy, cz, d[0], d[1], d[2], RoomListener.Face.WALL);
                        countSolidSource(power, counted, sample, nx, ny, nz, listener);
                    }
                    case LEAKY -> {
                        leaky++;
                        listener.face(cx, cy, cz, d[0], d[1], d[2], RoomListener.Face.LEAKY);
                        countSolidSource(power, counted, sample, nx, ny, nz, listener);
                    }
                    case UNLOADED -> {
                        open++;
                        listener.face(cx, cy, cz, d[0], d[1], d[2], RoomListener.Face.OUTLET);
                    }
                }
            }
        }
        return new Room(head, open, leaky, walls, power[0], power[1], truncated);
    }

    private static boolean isOutdoor(RoomWorld world, RoomWorld.Sample scratch, int x, int y, int z) {
        if (!world.skyExposed(x, y, z)) return false;

        int radius = Tuning.ROOM_OUTDOOR_RADIUS;
        int side = 2 * radius + 1;
        int open = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.sample(x + dx, y, z + dz, scratch);
                if (scratch.cell == RoomWorld.Cell.OPEN && world.skyExposed(x + dx, y, z + dz)) {
                    open++;
                }
            }
        }
        return open >= side * side * Tuning.ROOM_OUTDOOR_FRACTION;
    }

    private static void countSolidSource(
		float[] power, LongHashSet counted, RoomWorld.Sample sample,
		int x, int y, int z, RoomListener listener
	) {
        if (sample.power != 0f && counted.add(pack(x, y, z))) {
            addPower(power, sample.power);
            listener.source(x, y, z, sample.power);
        }
    }

    private static void addPower(float[] power, float value) {
        if (value > 0f) {
            power[0] += value;
        } else {
            power[1] += value;
        }
    }
}
