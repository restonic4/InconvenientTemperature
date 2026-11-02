package com.restonic4.inconvenienttemperature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.restonic4.inconvenienttemperature.core.WoolInsulation;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
    public record WoolWarmup(float head, float chest, float legs, float feet) {
        public static final WoolWarmup EMPTY = new WoolWarmup(0f, 0f, 0f, 0f);
        public static final Codec<WoolWarmup> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("head").forGetter(WoolWarmup::head),
                Codec.FLOAT.fieldOf("chest").forGetter(WoolWarmup::chest),
                Codec.FLOAT.fieldOf("legs").forGetter(WoolWarmup::legs),
                Codec.FLOAT.fieldOf("feet").forGetter(WoolWarmup::feet)).apply(i, WoolWarmup::new));

        public float[] toArray() {
            float[] values = new float[WoolInsulation.SLOTS];
            values[0] = head;
            values[1] = chest;
            values[2] = legs;
            values[3] = feet;
            return values;
        }

        public static WoolWarmup of(float[] v) {
            return new WoolWarmup(v[0], v[1], v[2], v[3]);
        }
    }

    public static final AttachmentType<Float> BODY_TEMPERATURE = AttachmentRegistry.create(
		InconvenientTemperature.id("body_temperature"),
		builder -> builder
			.initializer(() -> 0f)
			.persistent(Codec.FLOAT)
			.syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.targetOnly())
	);

    public static final AttachmentType<WoolWarmup> WOOL_WARMUP = AttachmentRegistry.createPersistent(
		InconvenientTemperature.id("wool_warmup"),
		WoolWarmup.CODEC
	);

    private ModAttachments() {}
    public static void register() {}
}
