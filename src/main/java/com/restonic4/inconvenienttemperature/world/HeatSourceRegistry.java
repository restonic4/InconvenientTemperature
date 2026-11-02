package com.restonic4.inconvenienttemperature.world;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public final class HeatSourceRegistry {
    public record Value(float strength, float roomPower, float cap, int group) {
        public static final Value NONE = new Value(0f, 0f, 0f, -1);

        Value withGroup(int index) {
            return new Value(strength, roomPower, cap, cap > 0f ? index : -1);
        }

        float magnitude() {
            return Math.abs(strength) + Math.abs(roomPower);
        }
    }

    public record Entry(
		Set<Block> blocks,
		List<TagKey<Block>> blockTags,
		Set<Fluid> fluids,
		List<TagKey<Fluid>> fluidTags,
		Map<String, String> state,
		Value value
	) {
        boolean matches(BlockState candidate) {
            return (matchesBlock(candidate) || matchesFluid(candidate)) && matchesState(candidate);
        }

        private boolean matchesBlock(BlockState candidate) {
            if (blocks.contains(candidate.getBlock())) return true;
            for (TagKey<Block> tag : blockTags) {
                if (candidate.is(tag)) return true;
            }
            return false;
        }

        private boolean matchesFluid(BlockState candidate) {
            FluidState fluid = candidate.getFluidState();
            if (fluid.isEmpty()) return false;
            if (fluids.contains(fluid.getType())) return true;
            for (TagKey<Fluid> tag : fluidTags) {
                if (fluid.is(tag)) return true;
            }
            return false;
        }

        private boolean matchesState(BlockState candidate) {
            for (Map.Entry<String, String> wanted : state.entrySet()) {
                Property<?> property = findProperty(candidate, wanted.getKey());
                if (property == null || !wanted.getValue().equals(nameOf(candidate, property))) {
                    return false;
                }
            }
            return true;
        }

        private static Property<?> findProperty(BlockState candidate, String name) {
            for (Property<?> property : candidate.getProperties()) {
                if (property.getName().equals(name)) return property;
            }
            return null;
        }

        private static <T extends Comparable<T>> String nameOf(BlockState candidate, Property<T> property) {
            return property.getName(candidate.getValue(property));
        }
    }

    private static volatile List<Entry> entries = List.of();
    private static volatile float[] groupCaps = new float[0];
    private static final Map<BlockState, Value> CACHE = new ConcurrentHashMap<>();

    private HeatSourceRegistry() {}

    public static void replace(List<Entry> loaded) {
        List<Entry> indexed = new java.util.ArrayList<>(loaded.size());
        float[] caps = new float[loaded.size()];
        for (int i = 0; i < loaded.size(); i++) {
            Entry entry = loaded.get(i);
            indexed.add(new Entry(
				entry.blocks(), entry.blockTags(), entry.fluids(),
				entry.fluidTags(), entry.state(), entry.value().withGroup(i)
			));
            caps[i] = entry.value().cap();
        }
        groupCaps = caps;
        entries = List.copyOf(indexed);
        CACHE.clear();
    }

    public static float[] groupCaps() {
        return groupCaps;
    }

    public static boolean isEmpty() {
        return entries.isEmpty();
    }

    public static Value value(BlockState state) {
        return CACHE.computeIfAbsent(state, HeatSourceRegistry::resolve);
    }

    private static Value resolve(BlockState state) {
        Value best = Value.NONE;
        for (Entry entry : entries) {
            if (entry.matches(state) && entry.value().magnitude() > best.magnitude()) {
                best = entry.value();
            }
        }
        return best;
    }
}
