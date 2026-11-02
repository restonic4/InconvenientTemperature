package com.restonic4.inconvenienttemperature.server;

import com.restonic4.inconvenienttemperature.InconvenientTemperature;
import com.restonic4.inconvenienttemperature.ModAttachments;
import com.restonic4.inconvenienttemperature.ModEffects;
import com.restonic4.inconvenienttemperature.ModAttachments.WoolWarmup;
import com.restonic4.inconvenienttemperature.core.Activity;
import com.restonic4.inconvenienttemperature.core.AmbientModel;
import com.restonic4.inconvenienttemperature.core.BiomeBlend;
import com.restonic4.inconvenienttemperature.core.BodyTemperature;
import com.restonic4.inconvenienttemperature.core.HeatFalloff;
import com.restonic4.inconvenienttemperature.core.Room;
import com.restonic4.inconvenienttemperature.core.RoomAnalyzer;
import com.restonic4.inconvenienttemperature.core.ThermalEffects;
import com.restonic4.inconvenienttemperature.config.Tuning;
import com.restonic4.inconvenienttemperature.core.Wetness;
import com.restonic4.inconvenienttemperature.core.WoolInsulation;
import com.restonic4.inconvenienttemperature.item.WoolArmorItem;
import com.restonic4.inconvenienttemperature.world.HeldSources;
import com.restonic4.inconvenienttemperature.world.SourceField;
import com.restonic4.inconvenienttemperature.core.RoomWorld;
import com.restonic4.inconvenienttemperature.world.HeatSourceRegistry;
import com.restonic4.inconvenienttemperature.world.LevelRoomWorld;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ThermalServer {
    public static final ResourceKey<DamageType> HEATSTROKE = ResourceKey.create(Registries.DAMAGE_TYPE, InconvenientTemperature.id("heatstroke"));

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final ResourceLocation SLOW_ID = InconvenientTemperature.id("thermal_slowness");
    private static final float SYNC_THRESHOLD = 0.05f;
    private static final float SPEED_UPDATE_THRESHOLD = 0.005f;

    private static final Map<UUID, PlayerRuntime> RUNTIMES = new ConcurrentHashMap<>();

    private static final class PlayerRuntime {
        final ServerPlayer owner;
        final ItemStack[] lastStacks = new ItemStack[WoolInsulation.SLOTS];
        final int[] wearTicks = new int[WoolInsulation.SLOTS];
        float weatherTarget;
        float insulation;
        float sprintWarmth;
        float finalTarget;
        float centerBiome;
        float blendedBiome;
        String biomeName = "";
        float sourceDelta;
        float radiantHeat;
        float radiantCold;
        float heatSum;
        HeldSources held = HeldSources.NONE;
        float coldSum;
        Room room = Room.NONE;
        float snowiness;
        Wetness.State wet = Wetness.State.DRY;
        boolean wetEffectApplied;
        int lastWetEffectSync;
        float lastSyncedBody = Float.NaN;
        float lastSpeedMultiplier = 1f;
        float damageProgress;
        boolean sampled;

        PlayerRuntime(ServerPlayer owner) {
            this.owner = owner;
        }
    }

    private ThermalServer() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RUNTIMES.remove(handler.getPlayer().getUUID()));
    }

    private static PlayerRuntime runtimeFor(ServerPlayer player) {
        PlayerRuntime existing = RUNTIMES.get(player.getUUID());
        if (existing != null && existing.owner == player) {
            return existing;
        }
        PlayerRuntime created = new PlayerRuntime(player);
        RUNTIMES.put(player.getUUID(), created);
        return created;
    }

    private static void tick(ServerPlayer player) {
        PlayerRuntime runtime = runtimeFor(player);
        float body = player.getAttachedOrElse(ModAttachments.BODY_TEMPERATURE, 0f);

        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            applyBody(player, runtime, 0f);
            applySlowness(player, runtime, 1f);
            runtime.damageProgress = 0f;
            return;
        }

        float insulation = WoolInsulation.insulation(tickWool(player, runtime));

        int phase = player.tickCount + player.getId();
        boolean scanDue = phase % Tuning.SCAN_INTERVAL_TICKS == 0;
        boolean roomDue = phase % Tuning.ROOM_SCAN_INTERVAL_TICKS == 0;
        boolean first = !runtime.sampled;
        if (scanDue || first) {
            Vec3 center = player.getBoundingBox().getCenter();
            SourceField field = SourceField.collect(player.serverLevel(), center, Tuning.SCAN_RADIUS);
            SourceField.Evaluation scan = field.evaluate(player.serverLevel(), center, player.getEyePosition());
            runtime.radiantHeat = scan.heat();
            runtime.radiantCold = scan.cold();
            runtime.snowiness = field.snowiness();
        }
        if (roomDue || first) {
            runtime.room = analyzeRoom(player);
        }
        if (scanDue || roomDue || first) {
            runtime.heatSum = runtime.radiantHeat + runtime.room.heatGain();
            runtime.coldSum = runtime.radiantCold + runtime.room.coldGain();
            runtime.weatherTarget = sampleWeather(player, runtime);
            runtime.sampled = true;
        }

        float burning = player.isOnFire() ? Tuning.ON_FIRE_HEAT : 0f;
        HeldSources held = HeldSources.of(player);
        float retention = runtime.room.retention();
        runtime.held = held;
        runtime.sourceDelta = HeatFalloff.capped(runtime.heatSum + burning + held.radiantHeat() + held.roomHeat() * retention)
			+ HeatFalloff.capped(runtime.coldSum + held.radiantCold() + held.roomCold() * retention);

        tickWetness(player, runtime);

        float weather = runtime.weatherTarget
                + WoolInsulation.warmth(insulation, runtime.weatherTarget)
                + Wetness.temperatureOffset(runtime.wet.wetness(), runtime.weatherTarget);
        float sprintWarmth = Activity.sprintWarmth(weather + runtime.sourceDelta, player.isSprinting());
        runtime.insulation = insulation;
        runtime.sprintWarmth = sprintWarmth;
        runtime.finalTarget = weather + sprintWarmth + runtime.sourceDelta;
        body = BodyTemperature.step(body, weather + sprintWarmth, runtime.sourceDelta, insulation);
        applyBody(player, runtime, body);
        applySlowness(player, runtime, ThermalEffects.speedMultiplier(body));
        applyDamage(player, runtime, body);
    }

    private static float sampleWeather(ServerPlayer player, PlayerRuntime runtime) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = player.blockPosition();
        boolean cycle = !level.dimensionType().hasFixedTime();
        runtime.centerBiome = level.getBiome(feet).value().getBaseTemperature();
        runtime.blendedBiome = blendedBiomeTemperature(level, feet);
        runtime.biomeName = level.getBiome(feet).unwrapKey().map(key -> key.location().toString()).orElse("?");
        AmbientModel.Inputs inputs = new AmbientModel.Inputs(
			runtime.blendedBiome,
			level.dimensionType().ultraWarm(),
			cycle && level.isNight(),
			level.isRainingAt(feet),
			(float) player.getEyeY(),
			runtime.room.openness(),
			runtime.snowiness
		);
        return AmbientModel.environmentTarget(inputs);
    }

    public static List<String> describeBlock(ServerPlayer player) {
        HitResult hit = player.pick(6.0, 0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return List.of("Look at a block within 6 blocks.");
        }
        BlockPos target = ((BlockHitResult) hit).getBlockPos();
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(target);
        HeatSourceRegistry.Value value = HeatSourceRegistry.value(state);
        RoomWorld.Sample sample = new RoomWorld.Sample();
        new LevelRoomWorld(level).sample(target.getX(), target.getY(), target.getZ(), sample);
        return List.of(
			String.valueOf(state),
			String.format(
				"radiant strength %.1f  room power %.1f (%s)", value.strength(), value.roomPower(),
				value.roomPower() == 0f ? "not a source" : "source"
			),
			"airflow: " + sample.cell
		);
    }

    private static Room analyzeRoom(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().ultraWarm()) {
            return Room.NONE;
        }
        LevelRoomWorld world = new LevelRoomWorld(level);
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        Room room = RoomAnalyzer.analyze(world, eye.getX(), eye.getY(), eye.getZ());
        if (room == Room.NONE) {
            BlockPos feet = player.blockPosition();
            room = RoomAnalyzer.analyze(world, feet.getX(), feet.getY(), feet.getZ());
        }
        return room;
    }

    private static float blendedBiomeTemperature(ServerLevel level, BlockPos feet) {
        float[] temperatures = new float[BiomeBlend.sampleCount()];
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        for (int i = 0; i < temperatures.length; i++) {
            sample.set(feet.getX() + BiomeBlend.offsetX(i), feet.getY(), feet.getZ() + BiomeBlend.offsetZ(i));
            temperatures[i] = level.getBiome(sample).value().getBaseTemperature();
        }
        return BiomeBlend.blend(temperatures);
    }

    public static List<String> describe(ServerPlayer player) {
        PlayerRuntime r = runtimeFor(player);
        float body = player.getAttachedOrElse(ModAttachments.BODY_TEMPERATURE, 0f);
        return List.of(
			String.format(
				"body %.1f  final target %.1f  (weather %.1f + sources %.1f + sprint %.1f)",
				body, r.finalTarget, r.weatherTarget, r.sourceDelta, r.sprintWarmth
			),
			String.format("biome %s  at feet %.2f  blended %.2f", r.biomeName, r.centerBiome, r.blendedBiome),
			String.format(
				"snowiness %.2f  wetness %.2f  wool %.2f  sprinting %s",
				r.snowiness, r.wet.wetness(), r.insulation, player.isSprinting()
			),
			String.format(
				"room: %d cells, openness %.2f (outlets %d, leaky %d, walls %d)%s",
				r.room.cells(), r.room.openness(), r.room.openFaces(), r.room.leakyFaces(),
				r.room.wallFaces(), r.room.truncated() ? " [search truncated]" : ""
			),
			String.format(
				"heat: radiant +%.1f/%.1f  room power +%.1f/%.1f -> gain +%.1f/%.1f",
				r.radiantHeat, r.radiantCold, r.room.heatPower(), r.room.coldPower(),
				r.room.heatGain(), r.room.coldGain()
			),
			String.format(
				"held: radiant +%.1f/%.1f  room power +%.1f/%.1f",
				r.held.radiantHeat(), r.held.radiantCold(), r.held.roomHeat(), r.held.roomCold()
			)
		);
    }

    private static void tickWetness(ServerPlayer player, PlayerRuntime runtime) {
        boolean underRain = player.serverLevel().isRainingAt(BlockPos.containing(player.getEyePosition()));
        float warmth = runtime.weatherTarget + runtime.sourceDelta;
        runtime.wet = Wetness.advance(runtime.wet, player.isInWater(), underRain, warmth);
        syncWetEffect(player, runtime, warmth);
    }

    private static void syncWetEffect(ServerPlayer player, PlayerRuntime runtime, float warmth) {
        MobEffectInstance current = player.getEffect(ModEffects.WET);
        float wetness = runtime.wet.wetness();
        if (wetness <= 0f) {
            if (current != null) {
                player.removeEffect(ModEffects.WET);
            }
            runtime.wetEffectApplied = false;
            return;
        }
        if (current == null) {
            if (runtime.wetEffectApplied) {
                runtime.wet = Wetness.State.DRY;
                runtime.wetEffectApplied = false;
            } else {
                applyWetEffect(player, runtime, warmth);
            }
            return;
        }
        boolean due = player.tickCount - runtime.lastWetEffectSync >= Tuning.WET_EFFECT_REFRESH_INTERVAL_TICKS;
        int gap = Wetness.remainingTicks(wetness, warmth) - current.getDuration();
        if (due && Math.abs(gap) > Tuning.WET_EFFECT_REFRESH_TOLERANCE_TICKS) {
            if (gap < 0) {
                player.removeEffect(ModEffects.WET);
            }
            applyWetEffect(player, runtime, warmth);
        }
    }

    private static void applyWetEffect(ServerPlayer player, PlayerRuntime runtime, float warmth) {
        int duration = Math.max(1, Wetness.remainingTicks(runtime.wet.wetness(), warmth));
        player.addEffect(new MobEffectInstance(ModEffects.WET, duration, 0, false, false, true));
        runtime.wetEffectApplied = true;
        runtime.lastWetEffectSync = player.tickCount;
    }

    private static float[] tickWool(ServerPlayer player, PlayerRuntime runtime) {
        float[] warmup = player.getAttachedOrElse(ModAttachments.WOOL_WARMUP, WoolWarmup.EMPTY).toArray();
        for (int i = 0; i < WoolInsulation.SLOTS; i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = player.getItemBySlot(slot);
            boolean wool = stack.getItem() instanceof WoolArmorItem;
            ItemStack previous = runtime.lastStacks[i];
            if (wool && previous != null && previous != stack) {
                warmup[i] = 0f;
                runtime.wearTicks[i] = 0;
            }
            warmup[i] = WoolInsulation.advanceWarmup(warmup[i], wool);
            runtime.lastStacks[i] = stack;
            if (wool && ++runtime.wearTicks[i] >= Tuning.WOOL_WEAR_INTERVAL_TICKS) {
                runtime.wearTicks[i] = 0;
                stack.hurtAndBreak(1, player, slot);
            } else if (!wool) {
                runtime.wearTicks[i] = 0;
            }
        }
        player.setAttached(ModAttachments.WOOL_WARMUP, WoolWarmup.of(warmup));
        return warmup;
    }

    private static void applyBody(ServerPlayer player, PlayerRuntime runtime, float body) {
        if (Float.isNaN(runtime.lastSyncedBody) || Math.abs(body - runtime.lastSyncedBody) >= SYNC_THRESHOLD) {
            player.setAttached(ModAttachments.BODY_TEMPERATURE, body);
            runtime.lastSyncedBody = body;
        }
    }

    private static void applySlowness(ServerPlayer player, PlayerRuntime runtime, float multiplier) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (multiplier >= 0.999f) {
            if (runtime.lastSpeedMultiplier < 0.999f || speed.hasModifier(SLOW_ID)) {
                speed.removeModifier(SLOW_ID);
            }
            runtime.lastSpeedMultiplier = 1f;
            return;
        }
        boolean present = speed.hasModifier(SLOW_ID);
        if (!present || Math.abs(multiplier - runtime.lastSpeedMultiplier) >= SPEED_UPDATE_THRESHOLD) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            runtime.lastSpeedMultiplier = multiplier;
        }
    }

    private static void applyDamage(ServerPlayer player, PlayerRuntime runtime, float body) {
        float perTick = ThermalEffects.damageProgressPerTick(body);
        if (perTick <= 0f) {
            runtime.damageProgress = 0f;
            return;
        }
        runtime.damageProgress += perTick;
        if (runtime.damageProgress < 1f) {
            return;
        }
        runtime.damageProgress -= 1f;
        player.hurt(damageSourceFor(player, body), Tuning.DAMAGE_AMOUNT);
    }

    private static DamageSource damageSourceFor(ServerPlayer player, float body) {
        if (body < 0f) {
            return player.damageSources().freeze();
        }
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HEATSTROKE));
    }
}
