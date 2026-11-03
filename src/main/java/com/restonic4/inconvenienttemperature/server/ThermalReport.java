package com.restonic4.inconvenienttemperature.server;

import com.restonic4.inconvenienttemperature.config.Tuning;
import com.restonic4.inconvenienttemperature.core.AmbientModel;
import com.restonic4.inconvenienttemperature.core.BodyTemperature;
import com.restonic4.inconvenienttemperature.core.Room;
import com.restonic4.inconvenienttemperature.core.ThermalEffects;
import com.restonic4.inconvenienttemperature.core.WoolInsulation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ThermalReport {
	private static final float ZERO_EPSILON = 0.05f;
	private static final String INDENT = "  ";

	private ThermalReport() {}

	public static List<Component> build(ThermalSnapshot s) {
		List<Component> lines = new ArrayList<>();

		summary(lines, s);
		target(lines, s);
		wool(lines, s);
		sources(lines, s);
		room(lines, s);
		wetness(lines, s);

		return lines;
	}

	private static void summary(List<Component> lines, ThermalSnapshot s) {
		lines.add(title("Inconvenient Temperature"));
		lines.add(line(
			label("Body "), signed(s.body()), dim(" / " + (int) Tuning.BAR_LIMIT + "  "), zone(s.body()),
			dim("  target "), signed(s.finalTarget())
		));

		float rate = BodyTemperature.perSecond(s.body(), s.weather() + s.sprintWarmth(), s.sourceDelta(), s.insulation());
		float gap = s.finalTarget() - s.body();
		lines.add(line(
			label("Moving "), signed(Math.signum(gap) * Math.min(rate, Math.abs(gap))), dim("/s"),
			dim("  (max " + number(rate) + "/s, gap " + number(gap) + ")")
		));

		lines.add(line(label("Effects "), effects(s.body())));
	}

	private static Component effects(float body) {
		float speed = ThermalEffects.speedMultiplier(body);
		float visual = ThermalEffects.visualIntensity(body);
		MutableComponent out = text("speed x" + number2(speed), speed < 0.999f ? ChatFormatting.YELLOW : ChatFormatting.GREEN)
			.append(dim("  visuals " + percent(visual) + "  "));

		if (!ThermalEffects.damaging(body)) {
			return out.append(text("no damage", ChatFormatting.GREEN)).append(dim(" (from +-" + (int) Tuning.DAMAGE_START + ")"));
		}

		float seconds = 1f / ThermalEffects.damageProgressPerTick(body) / 20f;

		return out.append(text(number(Tuning.DAMAGE_AMOUNT) + " HP every " + number(seconds) + " s", ChatFormatting.RED));
	}

	private static void target(List<Component> lines, ThermalSnapshot s) {
		AmbientModel.Breakdown a = s.ambient();

		lines.add(section("Target"));
		lines.add(row(0, "Environment", a.total(), ""));
		if (a.ultraWarm()) {
			lines.add(row(1, "Ultra-warm dimension", a.outdoor(), ""));
		} else {
			lines.add(row(1, "Biome", a.biomeBase(), s.biomeName() + "  feet " + number2(s.centerBiome()) + ", blended " + number2(s.blendedBiome())));
			lines.add(row(1, "Snow pull", a.snowShift(), "snowiness " + percent(s.snowiness())));
			lines.add(row(1, "Night", a.night(), ""));
			lines.add(row(1, "Altitude", a.altitude(), "Y " + number(s.eyeY())));
			lines.add(row(1, "Rain", a.rain(), ""));
			lines.add(line(
				dim(INDENT + INDENT + "outdoor "), signed(a.outdoor()),
				dim(" x room " + number2(a.roomFactor()) + " x depth " + number2(a.depthFactor()))
			));
		}
		lines.add(row(0, "Wool armor", s.woolWarmth(), "insulation " + percent(s.insulation())));
		lines.add(row(0, "Wetness", s.wetOffset(), percent(s.wetness()) + " wet"));
		lines.add(row(0, "Sprint", s.sprintWarmth(), s.sprinting() ? "sprinting" : "not sprinting"));
		lines.add(row(0, "Heat sources", s.sourceDelta(), ""));
		lines.add(line(label("= Target "), signed(s.finalTarget())));
	}

	private static void wool(List<Component> lines, ThermalSnapshot s) {
		float perUnit = WoolInsulation.warmthPerInsulation(s.ambient().total());

		lines.add(section("Wool armor"));
		for (ThermalSnapshot.WoolPiece piece : s.wool()) {
			if (!piece.worn()) {
				lines.add(line(dim(INDENT + piece.slot() + ": not wool")));
				continue;
			}
			lines.add(line(
				text(INDENT + piece.slot() + ": ", ChatFormatting.GRAY), text(piece.item(), ChatFormatting.WHITE),
				dim("  warm " + percent(piece.warmup()) + "  "), signed(piece.share() * perUnit),
				dim("  (" + percent(piece.share()) + " of insulation)")
			));
		}

		float withoutWool = s.finalTarget() - s.woolWarmth();

		lines.add(line(
			dim(INDENT + "insulation " + percent(s.insulation()) + " x " + number(perUnit) + " per 100% = "),
			signed(s.woolWarmth())
		));
		lines.add(line(dim(INDENT + "target without wool "), signed(withoutWool), dim(" (wool shifts it " + number(s.woolWarmth()) + ")")));
		lines.add(line(dim(INDENT + "warming speed x" + number2(BodyTemperature.heatingBonus(s.insulation())) + " when the body is below target")));
	}

	private static void sources(List<Component> lines, ThermalSnapshot s) {
		Room room = s.room();

		lines.add(section("Heat sources"));
		lines.add(sourceRow("Blocks radiant", s.radiantHeat(), s.radiantCold(), ""));
		lines.add(sourceRow("Room power", room.heatPower(), room.coldPower(), "x retention " + number2(room.retention())));
		lines.add(sourceRow("Room gain", room.heatGain(), room.coldGain(), ""));
		lines.add(sourceRow("Held items", s.held().radiantHeat() + s.held().roomHeat() * room.retention(), s.held().radiantCold() + s.held().roomCold() * room.retention(), ""));
		lines.add(row(1, "On fire", s.burning(), s.burning() > 0f ? "burning" : ""));
		lines.add(line(
			dim(INDENT + "heat total "), signed(s.heatRaw()), dim(" capped at " + (int) Tuning.HEAT_SOURCE_CAP + "   cold total "),
			signed(s.coldRaw()), dim(" capped at -" + (int) Tuning.COLD_SOURCE_CAP)
		));
		lines.add(line(label("Net sources "), signed(s.sourceDelta())));
	}

	private static MutableComponent sourceRow(String name, float heat, float cold, String note) {
		return line(
			text(INDENT + name, ChatFormatting.GRAY), dim(": "), signed(heat), dim(" / "), signed(cold),
			dim(note.isEmpty() ? "" : "  " + note)
		);
	}

	private static void room(List<Component> lines, ThermalSnapshot s) {
		Room room = s.room();

		lines.add(section("Room"));
		if (room.cells() == 0) {
			lines.add(line(dim(INDENT + "no room analysed (open air, Nether or End)")));
			return;
		}
		lines.add(line(
			dim(INDENT + room.cells() + " cells  openness "), text(percent(room.openness()), ChatFormatting.WHITE),
			dim("  retention " + percent(room.retention()) + " (volume factor " + number2(room.volumeFactor()) + ")")
		));
		lines.add(line(dim(
			INDENT + "outlets " + room.openFaces() + ", leaky " + room.leakyFaces() + ", walls " + room.wallFaces()
				+ (room.truncated() ? "  [search truncated]" : "")
		)));
	}

	private static void wetness(List<Component> lines, ThermalSnapshot s) {
		lines.add(section("Wetness"));
		if (s.wetness() <= 0f) {
			lines.add(line(dim(INDENT + "dry")));
			return;
		}
		lines.add(line(
			dim(INDENT), text(percent(s.wetness()) + " wet", ChatFormatting.AQUA), dim("  dries in " + seconds(s.dryTicks())),
			dim("  offset "), signed(s.wetOffset())
		));
	}

	private static MutableComponent row(int depth, String name, float value, String note) {
		String indent = INDENT.repeat(depth);

		return line(
			text(indent + name, depth == 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY), dim(": "), signed(value),
			dim(note.isEmpty() ? "" : "  " + note)
		);
	}

	private static Component zone(float body) {
		float magnitude = Math.abs(body);
		String kind = body < 0f ? "cold" : "hot";
		if (magnitude < Tuning.DEAD_ZONE) {
			return text("comfortable", ChatFormatting.GREEN);
		}
		if (magnitude < Tuning.DAMAGE_START) {
			return text(kind, ChatFormatting.YELLOW);
		}

		return text("DANGEROUS " + kind, ChatFormatting.DARK_RED);
	}

	private static MutableComponent signed(float value) {
		if (Math.abs(value) < ZERO_EPSILON) {
			return text("0.0", ChatFormatting.GRAY);
		}

		return text(String.format(Locale.ROOT, "%+.1f", value), value > 0f ? ChatFormatting.RED : ChatFormatting.AQUA);
	}

	private static MutableComponent title(String name) {
		return text("=== " + name + " ===", ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
	}

	private static MutableComponent section(String name) {
		return text("-- " + name + " --", ChatFormatting.YELLOW);
	}

	private static MutableComponent label(String value) { return text(value, ChatFormatting.WHITE); }

	private static MutableComponent dim(String value) { return text(value, ChatFormatting.DARK_GRAY); }

	private static MutableComponent text(String value, ChatFormatting color) {
		return Component.literal(value).withStyle(color);
	}

	private static MutableComponent line(Component... parts) {
		MutableComponent out = Component.empty();
		for (Component part : parts) {
			out.append(part);
		}

		return out;
	}

	private static String number(float value) { return String.format(Locale.ROOT, "%.1f", value); }

	private static String number2(float value) { return String.format(Locale.ROOT, "%.2f", value); }

	private static String percent(float fraction) { return Math.round(fraction * 100f) + "%"; }

	private static String seconds(int ticks) { return number(ticks / 20f) + " s"; }
}
