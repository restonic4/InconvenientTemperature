package com.restonic4.inconvenienttemperature.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ViewModeTest {
    @Test
    void cyclesThroughEveryModeAndWraps() {
        ViewMode mode = ViewMode.OFF;
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < ViewMode.values().length; i++) {
            seen.append(mode).append(' ');
            mode = mode.next();
        }

        assertEquals("OFF ROOM HEAT BOTH ", seen.toString());
        assertEquals(ViewMode.OFF, mode);
    }

    @Test
    void roomsAndHeatLayersMatchTheModeName() {
        assertFalse(ViewMode.OFF.rooms() || ViewMode.OFF.heat());
        assertTrue(ViewMode.ROOM.rooms() && !ViewMode.ROOM.heat());
        assertTrue(ViewMode.HEAT.heat() && !ViewMode.HEAT.rooms());
        assertTrue(ViewMode.BOTH.rooms() && ViewMode.BOTH.heat());
    }

    @Test
    void commandNamesRoundTripAndRejectUnknown() {
        for (ViewMode mode : ViewMode.values()) {
            assertEquals(Optional.of(mode), ViewMode.byCommandName(mode.commandName()));
        }

        assertEquals(Optional.empty(), ViewMode.byCommandName("lava"));
        assertEquals(Optional.empty(), ViewMode.byCommandName("ROOM"));
    }
}
