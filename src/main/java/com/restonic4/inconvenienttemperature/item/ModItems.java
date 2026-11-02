package com.restonic4.inconvenienttemperature.item;

import com.restonic4.inconvenienttemperature.InconvenientTemperature;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.Ingredient;

public final class ModItems {
    private static final ResourceKey<CreativeModeTab> COMBAT_TAB = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		ResourceLocation.withDefaultNamespace("combat")
	);

    public static final int WOOL_COLOR = 0xEDE8DC;
    private static final int DURABILITY_FACTOR = 7;

    public static final Holder<ArmorMaterial> WOOL_MATERIAL = Registry.registerForHolder(
		BuiltInRegistries.ARMOR_MATERIAL,
		InconvenientTemperature.id("wool"),
		new ArmorMaterial(
			Map.of(
				ArmorItem.Type.BOOTS, 1,
				ArmorItem.Type.LEGGINGS, 2,
				ArmorItem.Type.CHESTPLATE, 3,
				ArmorItem.Type.HELMET, 1),
			15,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			() -> Ingredient.of(ItemTags.WOOL),
			List.of(
				new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"), "", true),
				new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"), "_overlay", false)),
			0f,
			0f
		)
	);

    public static final WoolArmorItem WOOL_HELMET = armor("wool_helmet", ArmorItem.Type.HELMET);
    public static final WoolArmorItem WOOL_CHESTPLATE = armor("wool_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final WoolArmorItem WOOL_LEGGINGS = armor("wool_leggings", ArmorItem.Type.LEGGINGS);
    public static final WoolArmorItem WOOL_BOOTS = armor("wool_boots", ArmorItem.Type.BOOTS);

    private ModItems() {
    }

    private static WoolArmorItem armor(String name, ArmorItem.Type type) {
        return Registry.register(
			BuiltInRegistries.ITEM,
			InconvenientTemperature.id(name),
			new WoolArmorItem(
				WOOL_MATERIAL,
				type,
				new Item.Properties()
					.durability(type.getDurability(DURABILITY_FACTOR))
					.component(DataComponents.DYED_COLOR, new DyedItemColor(WOOL_COLOR, false))
			)
		);
    }

    public static WoolArmorItem[] all() {
        return new WoolArmorItem[] { WOOL_HELMET, WOOL_CHESTPLATE, WOOL_LEGGINGS, WOOL_BOOTS };
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(COMBAT_TAB).register(entries -> {
            for (WoolArmorItem item : all()) {
                entries.accept(item);
            }
        });
    }
}
