package com.restonic4.inconvenienttemperature.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.restonic4.inconvenienttemperature.InconvenientTemperature;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.restonic4.inconvenienttemperature.InconvenientTemperature.MOD_ID;

public final class HeatSourceLoader implements SimpleSynchronousResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "/sources");
    private static final String FOLDER = "heat_sources";

    public static void register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new HeatSourceLoader());
    }

    @Override
    public ResourceLocation getFabricId() {
        return InconvenientTemperature.id(FOLDER);
    }

    @Override
    public Collection<ResourceLocation> getFabricDependencies() {
        return List.of(ResourceReloadListenerKeys.TAGS);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<HeatSourceRegistry.Entry> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> file : manager.listResources(FOLDER, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                parseFile(file.getKey(), JsonParser.parseReader(reader).getAsJsonObject(), loaded);
            } catch (Exception problem) {
                LOGGER.warn("Skipping heat source file {}: {}", file.getKey(), problem.toString());
            }
        }

        HeatSourceRegistry.replace(loaded);
        LevelRoomWorld.clearCache();
        LOGGER.info("Loaded {} heat source entries", loaded.size());
    }

    static void parseFile(ResourceLocation file, JsonObject root, List<HeatSourceRegistry.Entry> out) {
        JsonArray array = root.getAsJsonArray("entries");
        for (JsonElement element : array) {
            parse(file, element.getAsJsonObject(), out);
        }
    }

    private static void parse(ResourceLocation file, JsonObject json, List<HeatSourceRegistry.Entry> out) {
        Set<Block> blocks = new HashSet<>();
        List<TagKey<Block>> blockTags = new ArrayList<>();
        Set<Fluid> fluids = new HashSet<>();
        List<TagKey<Fluid>> fluidTags = new ArrayList<>();

        readIds(json, "blocks", Registries.BLOCK, BuiltInRegistries.BLOCK, blocks, blockTags);
        readIds(json, "fluids", Registries.FLUID, BuiltInRegistries.FLUID, fluids, fluidTags);

        if (blocks.isEmpty() && blockTags.isEmpty() && fluids.isEmpty() && fluidTags.isEmpty()) return;

        float strength = number(file, json, "strength");
        float roomPower = number(file, json, "room_power");
        float cap = Math.max(0f, number(file, json, "cap"));

        Map<String, String> state = new LinkedHashMap<>();
        if (json.has("state")) {
            for (Map.Entry<String, JsonElement> property : json.getAsJsonObject("state").entrySet()) {
                state.put(property.getKey(), property.getValue().getAsString());
            }
        }

        out.add(
			new HeatSourceRegistry.Entry(
				blocks, blockTags, fluids, fluidTags, state,
				new HeatSourceRegistry.Value(strength, roomPower, cap, -1)
			)
		);
    }

    private static float number(ResourceLocation file, JsonObject json, String key) {
        float value = json.has(key) ? json.get(key).getAsFloat() : 0f;
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(key + " is not a finite number in " + file);
        }
        return value;
    }

    private static <T> void readIds(
		JsonObject json,
		String key,
		ResourceKey<? extends Registry<T>> registryKey,
		Registry<T> registry,
		Set<T> direct,
		List<TagKey<T>> tags
	) {
        if (!json.has(key)) return;

        for (JsonElement element : json.getAsJsonArray(key)) {
            String text = element.getAsString();
            boolean tag = text.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? text.substring(1) : text);

            if (id == null) throw new IllegalArgumentException("Invalid id: " + text);

            if (tag) {
				tags.add(TagKey.create(registryKey, id));
            } else {
                registry.getOptional(id).ifPresent(direct::add);
            }
        }
    }
}
