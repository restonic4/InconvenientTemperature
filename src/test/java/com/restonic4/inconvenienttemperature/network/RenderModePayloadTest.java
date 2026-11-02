package com.restonic4.inconvenienttemperature.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.restonic4.inconvenienttemperature.core.ViewMode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RenderModePayloadTest {
    private static RenderModePayload roundTrip(RenderModePayload payload) {
        ByteBuf buffer = Unpooled.buffer();
        RenderModePayload.CODEC.encode(buffer, payload);

        return RenderModePayload.CODEC.decode(buffer);
    }

    @Test
    void everyModeSurvivesTheWire() {
        for (ViewMode mode : ViewMode.values()) {
            RenderModePayload sent = new RenderModePayload(Optional.of(mode));

            assertEquals(sent, roundTrip(sent));
        }
    }

    @Test
    void cycleRequestSurvivesTheWire() {
        RenderModePayload cycle = new RenderModePayload(Optional.empty());

        assertEquals(cycle, roundTrip(cycle));
    }

    @Test
    void payloadIdIsNamespaced() {
        assertEquals("inconvenient_temperature:render_mode", RenderModePayload.TYPE.id().toString());
    }
}
