package com.restonic4.inconvenienttemperature.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SourceDataTest {
    private static JsonObject entryContaining(String file, String id) throws Exception {
        try (InputStream in = SourceDataTest.class.getResourceAsStream(
                "/data/inconvenient_temperature/heat_sources/" + file)) {
            assertNotNull(in, file);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("entries")) {
                JsonObject entry = element.getAsJsonObject();
                for (String key : new String[] {"blocks", "fluids"}) {
                    if (entry.has(key)) {
                        for (JsonElement listed : entry.getAsJsonArray(key)) {
                            if (listed.getAsString().equals(id)) {
                                return entry;
                            }
                        }
                    }
                }
            }
        }
        throw new AssertionError(id + " not found in " + file);
    }

    @Test
    void shippedLavaMatchesTheValuesTheModelTestsAssume() throws Exception {
        JsonObject lava = entryContaining("lava.json", "#minecraft:lava");
        assertEquals(16f, lava.get("strength").getAsFloat(), 0f);
        assertEquals(60f, lava.get("room_power").getAsFloat(), 0f);
    }

    @Test
    void shippedCampfireMatchesTheValuesTheModelTestsAssume() throws Exception {
        JsonObject campfire = entryContaining("fire.json", "minecraft:campfire");
        assertEquals(75f, campfire.get("strength").getAsFloat(), 0f);
        assertEquals(40f, campfire.get("room_power").getAsFloat(), 0f);
        assertEquals("true", campfire.getAsJsonObject("state").get("lit").getAsString());
    }

    @Test
    void everyShippedEntryHasATargetAndFiniteNumbers() throws Exception {
        for (String file : new String[] {"lava.json", "fire.json", "appliances.json", "cold.json", "burnt.json"}) {
            try (InputStream in = SourceDataTest.class.getResourceAsStream(
                    "/data/inconvenient_temperature/heat_sources/" + file)) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement element : root.getAsJsonArray("entries")) {
                    JsonObject entry = element.getAsJsonObject();
                    assertEquals(true, entry.has("blocks") || entry.has("fluids"), file);
                    assertEquals(true, Float.isFinite(entry.get("strength").getAsFloat()), file);
                    assertEquals(true, Float.isFinite(entry.get("room_power").getAsFloat()), file);
                }
            }
        }
    }

    @Test
    void everyColdEntryIsCappedSoWideFieldsCannotRunAway() throws Exception {
        try (InputStream in = SourceDataTest.class.getResourceAsStream(
                "/data/inconvenient_temperature/heat_sources/cold.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("entries")) {
                JsonObject entry = element.getAsJsonObject();
                assertEquals(true, entry.has("cap") && entry.get("cap").getAsFloat() > 0f, entry.toString());
                assertEquals(true, entry.get("strength").getAsFloat() < 0f, entry.toString());
            }
        }
    }
}
