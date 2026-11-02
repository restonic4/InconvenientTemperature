package com.restonic4.inconvenienttemperature.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(RenderModePayload.TYPE, RenderModePayload.CODEC);
    }
}
