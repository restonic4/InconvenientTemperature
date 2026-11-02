package com.restonic4.inconvenienttemperature.world;

import com.restonic4.inconvenienttemperature.core.HeatFalloff;
import com.restonic4.inconvenienttemperature.core.Occlusion;
import com.restonic4.inconvenienttemperature.core.SourceAccumulator;
import com.restonic4.inconvenienttemperature.config.Tuning;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

public final class SourceField {
    public record Evaluation(float heat, float cold) {}

    private int[] xs = new int[64];
    private int[] ys = new int[64];
    private int[] zs = new int[64];
    private float[] strengths = new float[64];
    private int[] groups = new int[64];
    private int size;
    private float snowWeight;

    private SourceField() {}

    public int size() {
        return size;
    }

    public int x(int index) { return xs[index]; }
    public int y(int index) { return ys[index]; }
    public int z(int index) { return zs[index]; }

    public float strength(int index) {
        return strengths[index];
    }

    public float snowiness() {
        return Math.min(1f, snowWeight / Tuning.SNOW_FULL_WEIGHT);
    }

    public static SourceField collect(Level level, Vec3 center, int radius) {
        SourceField field = new SourceField();
        BlockPos origin = BlockPos.containing(center);
        int reach = Math.max(radius, Tuning.SNOW_RADIUS);
        int minChunkX = SectionPos.blockToSectionCoord(origin.getX() - reach);
        int maxChunkX = SectionPos.blockToSectionCoord(origin.getX() + reach);
        int minChunkZ = SectionPos.blockToSectionCoord(origin.getZ() - reach);
        int maxChunkZ = SectionPos.blockToSectionCoord(origin.getZ() + reach);
        int minSectionY = SectionPos.blockToSectionCoord(origin.getY() - reach);
        int maxSectionY = SectionPos.blockToSectionCoord(origin.getY() + reach);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;

                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    int index = chunk.getSectionIndexFromSectionY(sectionY);
                    if (index < 0 || index >= chunk.getSectionsCount()) continue;

                    LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir()) continue;

                    boolean nearForSnow = withinRange(center, chunkX, sectionY, chunkZ, Tuning.SNOW_RADIUS);
                    if (!section.maybeHas(nearForSnow ? HeatSources::isRelevant : HeatSources::isSource)) continue;

                    field.collectSection(level, center, radius, section, chunkX, sectionY, chunkZ, pos, scratch);
                }
            }
        }
        return field;
    }

    public Evaluation evaluate(Level level, Vec3 center, Vec3 eye) {
        SourceAccumulator sums = new SourceAccumulator(HeatSourceRegistry.groupCaps());
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
        for (int i = 0; i < size; i++) {
            double dx = xs[i] + 0.5 - center.x;
            double dy = ys[i] + 0.5 - center.y;
            double dz = zs[i] + 0.5 - center.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float open = HeatFalloff.contribution(strengths[i], distance, 1f);
            if (Math.abs(open) < Tuning.MIN_SIGNIFICANT_CONTRIBUTION) continue;

            target.set(xs[i], ys[i], zs[i]);
            float transmission = Occlusion.transmission(thickness(level, eye, target, scratch));
            sums.add(groups[i], open * transmission);
        }
        return new Evaluation(sums.heat(), sums.cold());
    }

    private void collectSection(
		Level level, Vec3 center, int radius, LevelChunkSection section,
		int chunkX, int sectionY, int chunkZ,
		BlockPos.MutableBlockPos pos, BlockPos.MutableBlockPos scratch
	) {
        double rangeSquared = (double) radius * radius;
        double snowSquared = (double) Tuning.SNOW_RADIUS * Tuning.SNOW_RADIUS;
        double reachSquared = Math.max(rangeSquared, snowSquared);
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    int x = (chunkX << 4) + localX;
                    int y = (sectionY << 4) + localY;
                    int z = (chunkZ << 4) + localZ;
                    double dx = x + 0.5 - center.x;
                    double dy = y + 0.5 - center.y;
                    double dz = z + 0.5 - center.z;
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared >= reachSquared) continue;

                    BlockState state = section.getBlockState(localX, localY, localZ);
                    if (distanceSquared < snowSquared && HeatSources.isSnowCover(state)) {
                        snowWeight += 1f - (float) Math.sqrt(distanceSquared) / Tuning.SNOW_RADIUS;
                    }
                    HeatSourceRegistry.Value value = HeatSourceRegistry.value(state);
                    if (value.strength() == 0f || distanceSquared >= rangeSquared) continue;

                    pos.set(x, y, z);
                    if (HeatSources.isExposed(level, pos, state, scratch)) {
                        add(x, y, z, value.strength(), value.group());
                    }
                }
            }
        }
    }

    private void add(int x, int y, int z, float strength, int group) {
        if (size == xs.length) {
            int grown = size * 2;
            xs = Arrays.copyOf(xs, grown);
            ys = Arrays.copyOf(ys, grown);
            zs = Arrays.copyOf(zs, grown);
            strengths = Arrays.copyOf(strengths, grown);
            groups = Arrays.copyOf(groups, grown);
        }
        xs[size] = x;
        ys[size] = y;
        zs[size] = z;
        strengths[size] = strength;
        groups[size] = group;
        size++;
    }

    private static float thickness(Level level, Vec3 eye, BlockPos target, BlockPos.MutableBlockPos scratch) {
        return Occlusion.thickness(
			eye.x, eye.y, eye.z, target.getX(), target.getY(), target.getZ(),
			(x, y, z) -> level.getBlockState(scratch.set(x, y, z)).canOcclude()
		);
    }

    private static boolean withinRange(Vec3 center, int chunkX, int sectionY, int chunkZ, double reach) {
        double dx = axisGap(center.x, chunkX << 4);
        double dy = axisGap(center.y, sectionY << 4);
        double dz = axisGap(center.z, chunkZ << 4);
        return dx * dx + dy * dy + dz * dz <= reach * reach;
    }

    private static double axisGap(double value, int boxMin) {
        if (value < boxMin) return boxMin - value;
        return Math.max(0.0, value - (boxMin + 16));
    }
}
