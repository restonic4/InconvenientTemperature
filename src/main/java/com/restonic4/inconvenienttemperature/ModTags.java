package com.restonic4.inconvenienttemperature;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> AIRTIGHT = block("airtight");
    public static final TagKey<Block> LEAKY = block("leaky");
    public static final TagKey<Block> OPEN = block("open");
    public static final TagKey<Item> HELD_HEAT_SOURCES = TagKey.create(Registries.ITEM, InconvenientTemperature.id("held_heat_sources"));

    private ModTags() {}

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, InconvenientTemperature.id(name));
    }
}
