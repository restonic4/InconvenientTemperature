package com.restonic4.inconvenienttemperature.core;

public record SourceMark(int x, int y, int z, float radiant, float delivered, float room) {
    public boolean hasRadiant() { return radiant != 0f; }

    public boolean hasRoom() { return room != 0f; }

    public boolean heating() { return (hasRadiant() ? radiant : room) > 0f; }

    SourceMark plus(SourceMark other) {
        return new SourceMark(
            x, y, z,
            radiant + other.radiant, delivered + other.delivered, room + other.room
        );
    }
}
