package com.restonic4.inconvenienttemperature.core;

import java.util.Locale;
import java.util.Optional;

public enum ViewMode {
    OFF,
    ROOM,
    HEAT,
    BOTH;

    public ViewMode next() { return values()[(ordinal() + 1) % values().length]; }

    public boolean rooms() { return this == ROOM || this == BOTH; }

    public boolean heat() { return this == HEAT || this == BOTH; }

    public String commandName() { return name().toLowerCase(Locale.ROOT); }

    public static Optional<ViewMode> byCommandName(String name) {
        for (ViewMode mode : values()) {
            if (mode.commandName().equals(name)) {
                return Optional.of(mode);
            }
        }

        return Optional.empty();
    }
}
