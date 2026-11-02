package com.restonic4.inconvenienttemperature.client.view;

import java.util.List;

record HeatMap(List<Tile> tiles) {
    record Tile(int x, int y, int z, int argb) {}

    static final HeatMap EMPTY = new HeatMap(List.of());
}
