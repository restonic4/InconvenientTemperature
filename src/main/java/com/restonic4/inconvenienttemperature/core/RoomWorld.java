package com.restonic4.inconvenienttemperature.core;

public interface RoomWorld {
    enum Cell {
        OPEN,
        SEALED,
        LEAKY,
        UNLOADED
    }

    final class Sample {
        public Cell cell;
        public float power;
    }

    void sample(int x, int y, int z, Sample out);

    boolean skyExposed(int x, int y, int z);
}
