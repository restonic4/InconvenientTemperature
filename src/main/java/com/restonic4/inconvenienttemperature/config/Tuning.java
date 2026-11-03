package com.restonic4.inconvenienttemperature.config;

public final class Tuning {
    public static final float BAR_LIMIT = 100f;

    public static final float DEAD_ZONE = 20f;
    public static final float VISUAL_START = 25f;
    public static final float DAMAGE_START = 70f;
    public static final float SLOW_START = DEAD_ZONE;
    public static final float SLOWEST_SPEED_FRACTION = 1f / 3f;

    public static final int DAMAGE_SLOWEST_INTERVAL_TICKS = 200;
    public static final int DAMAGE_FASTEST_INTERVAL_TICKS = 40;
    public static final float DAMAGE_AMOUNT = 1f;

    public static final float BIOME_COMFORT = 0.8f;
    public static final float BIOME_COLD_SCALE = 80f;
    public static final float BIOME_HOT_SCALE = 60f;
    public static final float BIOME_MIN = -1f;
    public static final float BIOME_MAX = 2.5f;
    public static final float ULTRAWARM_BASE = 55f;
    public static final float NIGHT_OFFSET = -20f;
    public static final float RAIN_OFFSET = -12f;
    public static final float COLD_ALTITUDE_START_Y = 100f;
    public static final float COLD_ALTITUDE_PER_BLOCK = 0.5f;
    public static final float COLD_ALTITUDE_MAX = 40f;
    public static final float DEPTH_FULL_Y = 50f;
    public static final float DEPTH_FADE_BLOCKS = 60f;
    public static final float DEPTH_FADE_BOTTOM_Y = -10f;
    public static final float DEPTH_MIN = 0.15f;

    public static final int[] BIOME_BLEND_RINGS = {16, 32, 64};
    public static final int BIOME_BLEND_DIRECTIONS = 8;
    public static final float BIOME_BLEND_CENTER_WEIGHT = 3f;
    public static final float BIOME_BLEND_FALLOFF = 24f;

    public static final int ROOM_MAX_CELLS = 2500;
    public static final int ROOM_MAX_RANGE = 24;
    public static final int ROOM_SCAN_INTERVAL_TICKS = 20;
    public static final float ROOM_OPEN_CONDUCTANCE = 1f;
    public static final float ROOM_LEAKY_CONDUCTANCE = 0.4f;
    public static final float ROOM_WALL_CONDUCTANCE = 0.04f;
    public static final float ROOM_REFERENCE_VOLUME = 100f;
    public static final float ROOM_AMBIENT_MIN = 0.25f;
    public static final int ROOM_OUTDOOR_RADIUS = 2;
    public static final float ROOM_OUTDOOR_FRACTION = 0.5f;
    public static final float ROOM_SOLID_BOUNDS_VOLUME = 0.45f;

    public static final float SPRINT_WARMTH = 8f;

    public static final float SNOWY_BIOME_TEMPERATURE = 0f;
    public static final int SNOW_RADIUS = 8;
    public static final float SNOW_FULL_WEIGHT = 35f;
    public static final float SNOW_MAX_INFLUENCE = 0.7f;

    public static final float WET_WATER_RISE_PER_TICK = 1f / 60f;
    public static final int WET_RAIN_EXPOSURE_TICKS = 100;
    public static final float WET_RAIN_RISE_PER_TICK = 1f / 400f;
    public static final int WET_RAIN_EXPOSURE_DECAY_PER_TICK = 2;
    public static final float WET_DRY_PER_TICK = 1f / 1800f;
    public static final float WET_DRY_WARMTH_SCALE = 20f;
    public static final float WET_DRY_MIN_MULTIPLIER = 0.3f;
    public static final float WET_DRY_MAX_MULTIPLIER = 8f;
    public static final float WET_COLD_BASE = 20f;
    public static final float WET_COLD_EXTRA = 25f;
    public static final float WET_COLD_FULL_AT = 50f;
    public static final int WET_EFFECT_MAX_TICKS = 20 * 60 * 30;
    public static final int WET_EFFECT_REFRESH_TOLERANCE_TICKS = 60;
    public static final int WET_EFFECT_REFRESH_INTERVAL_TICKS = 10;

    public static final float RATE_MIN_PER_SECOND = 0.8f;
    public static final float RATE_PER_WEATHER_GAP = 0.02f;
    public static final float RATE_PER_SOURCE_UNIT = 0.04f;
    public static final float RATE_MAX_PER_SECOND = 14f;

    public static final int SCAN_RADIUS = 20;
    public static final int SCAN_INTERVAL_TICKS = 5;
    public static final float SOURCE_FALLOFF_CORE = 3f;
    public static final double OCCLUSION_STEP = 0.6;
    public static final float OCCLUSION_LENGTH = 1.5f;
    public static final float OCCLUSION_FLOOR = 0.1f;
    public static final float HEAT_SOURCE_CAP = 600f;
    public static final float ON_FIRE_HEAT = 300f;
    public static final int LAVA_CARRY_BURN_TICKS = 61;
    public static final int LAVA_CARRY_RELIGHT_AT_TICKS = 1;
    public static final float COLD_SOURCE_CAP = 150f;
    public static final float MIN_SIGNIFICANT_CONTRIBUTION = 0.25f;

    public static final float WOOL_WARMTH = 65f;
    public static final float WOOL_COLD_BONUS = 10f;
    public static final float WOOL_COLD_BONUS_FULL_AT = 40f;
    public static final float HELD_SOURCE_DISTANCE = 0.6f;
    public static final float WOOL_HEATING_RATE_BONUS = 0.75f;
    public static final int WOOL_WARMUP_TICKS = 1200;
    public static final int WOOL_WEAR_INTERVAL_TICKS = 800;
    public static final float[] WOOL_SLOT_WEIGHTS = {0.2f, 0.35f, 0.3f, 0.15f};

    private Tuning() {}
}
