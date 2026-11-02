package com.restonic4.inconvenienttemperature.network;

import com.restonic4.inconvenienttemperature.InconvenientTemperature;
import com.restonic4.inconvenienttemperature.core.ViewMode;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sets the client's heat view; an empty mode means "cycle to the next one". */
public record RenderModePayload(Optional<ViewMode> mode) implements CustomPacketPayload {
    public static final Type<RenderModePayload> TYPE = new Type<>(InconvenientTemperature.id("render_mode"));

    public static final StreamCodec<ByteBuf, RenderModePayload> CODEC = ByteBufCodecs
        .optional(ByteBufCodecs.idMapper(id -> ViewMode.values()[id], ViewMode::ordinal))
        .map(RenderModePayload::new, RenderModePayload::mode);

    @Override
    public Type<RenderModePayload> type() { return TYPE; }
}
