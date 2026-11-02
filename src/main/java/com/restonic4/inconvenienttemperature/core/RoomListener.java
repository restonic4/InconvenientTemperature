package com.restonic4.inconvenienttemperature.core;

public interface RoomListener {
    enum Face {
        OUTLET,
        LEAKY,
        WALL
    }

    RoomListener NONE = new RoomListener() {};

    default void cell(int x, int y, int z) {}
    default void face(int x, int y, int z, int dx, int dy, int dz, Face kind) {}
    default void source(int x, int y, int z, float power) {}
}
