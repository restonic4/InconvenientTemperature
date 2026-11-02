package com.restonic4.inconvenienttemperature.core;

import java.util.HashMap;
import java.util.Map;

final class VoxelWorld implements RoomWorld {
    private static final int SKY_SCAN = 40;

    private final Map<Long, Cell> cells = new HashMap<>();
    private final Map<Long, Float> powers = new HashMap<>();
    private final int groundY;

    VoxelWorld(int groundY) {
        this.groundY = groundY;
    }

    VoxelWorld set(int x, int y, int z, Cell cell) {
        cells.put(RoomAnalyzer.pack(x, y, z), cell);
        return this;
    }

    VoxelWorld power(int x, int y, int z, float power) {
        powers.put(RoomAnalyzer.pack(x, y, z), power);
        return this;
    }

    VoxelWorld box(int x0, int y0, int z0, int x1, int y1, int z1, Cell cell) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(x, y, z, cell);
                }
            }
        }
        return this;
    }

    VoxelWorld hollow(int x0, int y0, int z0, int x1, int y1, int z1, Cell shell) {
        box(x0 - 1, y0 - 1, z0 - 1, x1 + 1, y1 + 1, z1 + 1, shell);
        return box(x0, y0, z0, x1, y1, z1, Cell.OPEN);
    }

    Cell at(int x, int y, int z) {
        Cell explicit = cells.get(RoomAnalyzer.pack(x, y, z));
        if (explicit != null) {
            return explicit;
        }
        return y <= groundY ? Cell.SEALED : Cell.OPEN;
    }

    @Override
    public void sample(int x, int y, int z, Sample out) {
        out.cell = at(x, y, z);
        out.power = powers.getOrDefault(RoomAnalyzer.pack(x, y, z), 0f);
    }

    @Override
    public boolean skyExposed(int x, int y, int z) {
        for (int up = y + 1; up <= y + SKY_SCAN; up++) {
            Cell cell = at(x, up, z);
            if (cell == Cell.SEALED || cell == Cell.LEAKY) {
                return false;
            }
        }
        return true;
    }
}
