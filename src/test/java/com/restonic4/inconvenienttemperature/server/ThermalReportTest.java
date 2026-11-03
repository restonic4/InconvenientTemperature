package com.restonic4.inconvenienttemperature.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restonic4.inconvenienttemperature.core.AmbientModel;
import com.restonic4.inconvenienttemperature.core.Room;
import com.restonic4.inconvenienttemperature.world.HeldSources;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

class ThermalReportTest {
	private static ThermalSnapshot snowyPlainsWithWool(float insulation, float woolWarmth) {
		AmbientModel.Breakdown ambient = AmbientModel.breakdown(new AmbientModel.Inputs(0f, false, false, false, 70f, 1f, 0f));
		List<ThermalSnapshot.WoolPiece> wool = List.of(
			new ThermalSnapshot.WoolPiece("Head", "Wool Cap", 0.2f, 1f),
			new ThermalSnapshot.WoolPiece("Chest", null, 0.35f, 0f),
			new ThermalSnapshot.WoolPiece("Legs", "Wool Pants", 0.3f, 0.5f),
			new ThermalSnapshot.WoolPiece("Feet", null, 0.15f, 0f)
		);

		return new ThermalSnapshot(
			-42f, -19f, ambient.total() + woolWarmth, ambient, "minecraft:snowy_plains", 0f, 0f, 0f, 71f,
			wool, insulation, woolWarmth,
			0f, 0, 0f,
			false, 0f,
			Room.NONE, 0f, 0f, 0f, HeldSources.NONE, 0f, 0f, 0f
		);
	}

	private static String plain(List<Component> lines) {
		return lines.stream().map(Component::getString).collect(Collectors.joining("\n"));
	}

	@Test
	void everySectionIsPresent() {
		String text = plain(ThermalReport.build(snowyPlainsWithWool(0.35f, 15.7f)));

		for (String heading : List.of("Inconvenient Temperature", "Target", "Wool armor", "Heat sources", "Room", "Wetness")) {
			assertTrue(text.contains(heading), heading);
		}
	}

	@Test
	void woolHelpIsShownPerPieceAndInTotal() {
		String text = plain(ThermalReport.build(snowyPlainsWithWool(0.35f, 15.7f)));

		assertTrue(text.contains("Head: Wool Cap"), text);
		assertTrue(text.contains("Chest: not wool"), text);
		assertTrue(text.contains("target without wool -34.7"), text);
		assertTrue(text.contains("+15.7"), text);
	}

	@Test
	void lineBreaksAreSeparateComponents() {
		List<Component> lines = ThermalReport.build(snowyPlainsWithWool(0.35f, 15.7f));

		assertTrue(lines.size() > 20);
		assertTrue(lines.stream().noneMatch(line -> line.getString().contains("\n")));
	}

	@Test
	void coldValuesAreAquaAndHeatValuesAreRed() {
		List<Component> lines = ThermalReport.build(snowyPlainsWithWool(0.35f, 15.7f));
		Component bodyLine = lines.get(1);

		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), bodyLine.getSiblings().get(1).getStyle().getColor());

		Component woolRow = lines.stream().filter(line -> line.getString().startsWith("Wool armor")).findFirst().orElseThrow();

		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), woolRow.getSiblings().get(2).getStyle().getColor());
	}
}
