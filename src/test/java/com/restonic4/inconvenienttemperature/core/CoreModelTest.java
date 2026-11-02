package com.restonic4.inconvenienttemperature.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restonic4.inconvenienttemperature.config.Tuning;
import org.junit.jupiter.api.Test;

class CoreModelTest {
    private static AmbientModel.Inputs outdoors(float biome, boolean night, float y) {
        return new AmbientModel.Inputs(biome, false, night, false, y, 1f, 0f);
    }

    private static int secondsToReach(float start, float weather, float sources, float wool, float goal) {
        float body = start;
        for (int tick = 0; tick < 20 * 600; tick++) {
            body = BodyTemperature.step(body, weather, sources, wool);
            if (goal >= start ? body >= goal : body <= goal) {
                return Math.round(tick / 20f);
            }
        }
        return Integer.MAX_VALUE;
    }

    @Test
    void plainsByDayIsComfortable() {
        assertEquals(0f, AmbientModel.environmentTarget(outdoors(0.8f, false, 70)), 0.001f);
    }

    @Test
    void snowyPlainsByDayIsUncomfortableButNotDamaging() {
        float target = AmbientModel.environmentTarget(outdoors(0f, false, 70));
        assertTrue(target < -Tuning.VISUAL_START);
        assertTrue(Math.abs(target) < Tuning.DAMAGE_START);
    }

    @Test
    void frozenPeakAtNightIsFarPastTheDamageLine() {
        float target = AmbientModel.environmentTarget(outdoors(-0.7f, true, 160));
        assertTrue(target < -Tuning.BAR_LIMIT);
    }

    @Test
    void desertBelowGroundIsMild() {
        AmbientModel.Inputs cave = new AmbientModel.Inputs(2.0f, false, false, false, 0f, 0f, 0f);
        assertTrue(Math.abs(AmbientModel.environmentTarget(cave)) < Tuning.VISUAL_START);
    }

    @Test
    void netherAloneIsHotButBelowDamage() {
        AmbientModel.Inputs nether = new AmbientModel.Inputs(2.0f, true, false, false, 64f, 0f, 0f);
        float target = AmbientModel.environmentTarget(nether);
        assertTrue(target > Tuning.VISUAL_START);
        assertTrue(target < Tuning.DAMAGE_START);
    }

    @Test
    void lavaLakeReachesTheBarInSeconds() {
        int seconds = secondsToReach(0f, 0f, 600f, 0f, 100f);
        assertTrue(seconds >= 5 && seconds <= 12, "seconds=" + seconds);
    }

    @Test
    void mildColdTakesAMinuteOrMoreToBecomeDangerous() {
        int seconds = secondsToReach(0f, -140f, 0f, 0f, -Tuning.DAMAGE_START);
        assertTrue(seconds >= 20 && seconds <= 90, "seconds=" + seconds);
    }

    @Test
    void recoveryByFireIsFasterThanNaturalDrift() {
        int byFire = secondsToReach(-100f, -48f, 60f, 0f, 0f);
        int natural = secondsToReach(-100f, 0f, 0f, 0f, -20f);
        assertTrue(byFire < natural, "fire=" + byFire + " natural=" + natural);
    }

    @Test
    void woolSpeedsUpHeating() {
        int bare = secondsToReach(0f, 140f, 0f, 0f, 70f);
        int wool = secondsToReach(0f, 140f, 0f, 1f, 70f);
        assertTrue(wool < bare, "bare=" + bare + " wool=" + wool);
    }

    @Test
    void woolDoesNotChangeCoolingRate() {
        assertEquals(BodyTemperature.step(50f, -100f, 0f, 0f), BodyTemperature.step(50f, -100f, 0f, 1f), 1e-6f);
    }

    @Test
    void bodyNeverOvershootsTargetOrBar() {
        assertEquals(10f, BodyTemperature.step(9.99f, 10f, 0f, 0f), 1e-6f);
        assertEquals(100f, BodyTemperature.step(99.9f, 0f, 5000f, 1f), 1e-6f);
        assertEquals(-100f, BodyTemperature.step(-99.9f, 0f, -5000f, 0f), 1e-6f);
    }

    @Test
    void speedIsUnchangedInsideTheDeadZoneAndOneThirdAtTheEdge() {
        assertEquals(1f, ThermalEffects.speedMultiplier(Tuning.DEAD_ZONE), 1e-6f);
        assertEquals(1f, ThermalEffects.speedMultiplier(-10f), 1e-6f);
        assertEquals(1f / 3f, ThermalEffects.speedMultiplier(100f), 1e-6f);
        assertEquals(1f / 3f, ThermalEffects.speedMultiplier(-100f), 1e-6f);
        float previous = 1f;
        for (int v = 20; v <= 100; v++) {
            float m = ThermalEffects.speedMultiplier(v);
            assertTrue(m <= previous);
            previous = m;
        }
    }

    @Test
    void visualsStartAtTheConfiguredFractionAndSymmetric() {
        assertEquals(0f, ThermalEffects.visualIntensity(Tuning.VISUAL_START), 1e-6f);
        assertEquals(1f, ThermalEffects.visualIntensity(100f), 1e-6f);
        assertEquals(ThermalEffects.visualIntensity(60f), ThermalEffects.visualIntensity(-60f), 1e-6f);
    }

    @Test
    void damageStartsSlowAndEndsAtVanillaFreezeRate() {
        assertEquals(0f, ThermalEffects.damageProgressPerTick(69.9f), 0f);
        assertEquals(1f / 200f, ThermalEffects.damageProgressPerTick(70f), 1e-6f);
        assertEquals(1f / 40f, ThermalEffects.damageProgressPerTick(100f), 1e-6f);
        assertEquals(ThermalEffects.damageProgressPerTick(85f), ThermalEffects.damageProgressPerTick(-85f), 1e-6f);
    }

    @Test
    void sourceTotalsAreCappedOnBothSides() {
        assertEquals(Tuning.HEAT_SOURCE_CAP, HeatFalloff.capped(99999f), 0f);
        assertEquals(-Tuning.COLD_SOURCE_CAP, HeatFalloff.capped(-99999f), 0f);
        assertEquals(12f, HeatFalloff.capped(12f), 0f);
    }

    @Test
    void woolWarmsUpGraduallyAndDropsInstantly() {
        float w = 0f;
        for (int i = 0; i < Tuning.WOOL_WARMUP_TICKS / 2; i++) {
            w = WoolInsulation.advanceWarmup(w, true);
        }
        assertEquals(0.5f, w, 0.01f);
        for (int i = 0; i < Tuning.WOOL_WARMUP_TICKS; i++) {
            w = WoolInsulation.advanceWarmup(w, true);
        }
        assertEquals(1f, w, 0f);
        assertEquals(0f, WoolInsulation.advanceWarmup(w, false), 0f);
    }

    @Test
    void fullWoolSetIsFullInsulation() {
        assertEquals(1f, WoolInsulation.insulation(new float[] {1, 1, 1, 1}), 1e-6f);
        assertEquals(0f, WoolInsulation.insulation(new float[] {0, 0, 0, 0}), 0f);
    }

    private static AmbientModel.Inputs snowy(float biome, float snowiness) {
        return new AmbientModel.Inputs(biome, false, false, false, 70f, 1f, snowiness);
    }

    @Test
    void nearbySnowPullsAMildBiomeTowardSnowyButNotAllTheWay() {
        float none = AmbientModel.environmentTarget(snowy(0.8f, 0f));
        float some = AmbientModel.environmentTarget(snowy(0.8f, 0.3f));
        float all = AmbientModel.environmentTarget(snowy(0.8f, 1f));
        float snowyBiome = AmbientModel.environmentTarget(snowy(0f, 0f));
        assertTrue(none > some && some > all);
        assertTrue(all > snowyBiome, "all=" + all + " snowyBiome=" + snowyBiome);
    }

    @Test
    void snowNeverWarmsAnAlreadyColdBiome() {
        assertEquals(AmbientModel.environmentTarget(snowy(-0.7f, 0f)), AmbientModel.environmentTarget(snowy(-0.7f, 1f)), 1e-4f);
        assertEquals(AmbientModel.environmentTarget(snowy(0f, 0f)), AmbientModel.environmentTarget(snowy(0f, 1f)), 1e-4f);
    }

    @Test
    void snowCoveredDesertIsCoolButMilderThanARealSnowyBiome() {
        float covered = AmbientModel.environmentTarget(snowy(2.0f, 1f));
        float snowyBiome = AmbientModel.environmentTarget(snowy(0f, 0f));
        assertTrue(covered < 0f, "covered=" + covered);
        assertTrue(covered > snowyBiome + 25f, "covered=" + covered + " snowyBiome=" + snowyBiome);
    }

    private static int ticksUntilDry(float warmth) {
        Wetness.State s = new Wetness.State(1f, 0);
        for (int i = 0; i < 20 * 3600; i++) {
            s = Wetness.advance(s, false, false, warmth);
            if (s.wetness() <= 0f) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    @Test
    void wetnessDriesFastWhenHotAndSlowlyWhenCold() {
        int hot = ticksUntilDry(72f) / 20;
        int neutral = ticksUntilDry(0f) / 20;
        int cold = ticksUntilDry(-48f) / 20;
        int nearLava = ticksUntilDry(600f) / 20;
        assertTrue(hot <= 30, "hot=" + hot);
        assertTrue(neutral >= 60 && neutral <= 120, "neutral=" + neutral);
        assertTrue(cold >= 180, "cold=" + cold);
        assertTrue(nearLava < hot, "nearLava=" + nearLava);
    }

    @Test
    void waterSoaksQuicklyAndRainNeedsExposureFirst() {
        Wetness.State s = Wetness.State.DRY;
        for (int i = 0; i < 60; i++) {
            s = Wetness.advance(s, true, false, 0f);
        }
        assertEquals(1f, s.wetness(), 1e-4f);

        Wetness.State rain = Wetness.State.DRY;
        for (int i = 0; i < Tuning.WET_RAIN_EXPOSURE_TICKS - 1; i++) {
            rain = Wetness.advance(rain, false, true, 0f);
        }
        assertEquals(0f, rain.wetness(), 0f);
        for (int i = 0; i < 400; i++) {
            rain = Wetness.advance(rain, false, true, 0f);
        }
        assertTrue(rain.wetness() > 0.9f, "rain=" + rain.wetness());
    }

    @Test
    void briefShelterDoesNotResetRainExposureInstantly() {
        Wetness.State s = Wetness.State.DRY;
        for (int i = 0; i < 80; i++) {
            s = Wetness.advance(s, false, true, 0f);
        }
        s = Wetness.advance(s, false, false, 0f);
        assertEquals(78, s.rainExposure());
    }

    @Test
    void wetnessChillsMoreInTheColdAndStillChillsInTheHeat() {
        float neutral = Wetness.temperatureOffset(1f, 0f);
        float snow = Wetness.temperatureOffset(1f, -48f);
        float desert = Wetness.temperatureOffset(1f, 72f);
        assertTrue(snow < neutral && neutral < 0f);
        assertEquals(neutral, desert, 1e-6f);
        assertEquals(0f, Wetness.temperatureOffset(0f, -48f), 0f);
    }

    @Test
    void effectDurationMatchesHowLongTheDryingTakes() {
        int display = Wetness.remainingTicks(1f, 0f);
        assertEquals(ticksUntilDry(0f), display, 2);
    }

    private static float lake(int width, int depth, int lavaY, int bankTop, double standBack, boolean wallBetween) {
        Occlusion.Solids solids = (x, y, z) -> (x < 0 && y <= bankTop) || (wallBetween && x == -1 && y >= 0 && y <= 2);
        double px = -standBack + 0.5;
        double py = 1.62;
        double pz = 0.5;
        float total = 0f;
        for (int x = 0; x < width; x++) {
            for (int z = -(depth / 2); z < depth - depth / 2; z++) {
                double dx = x + 0.5 - px;
                double dy = lavaY + 0.5 - 0.9;
                double dz = z + 0.5 - pz;
                float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                float open = HeatFalloff.contribution(ShippedSources.LAVA_STRENGTH, distance, 1f);
                if (open < Tuning.MIN_SIGNIFICANT_CONTRIBUTION) {
                    continue;
                }
                float thickness = Occlusion.thickness(px, py, pz, x, lavaY, z, solids);
                total += open * Occlusion.transmission(thickness);
            }
        }
        return HeatFalloff.capped(total);
    }

    @Test
    void bigFlushLavaLakeIsFeltFromFarAway() {
        assertEquals(Tuning.HEAT_SOURCE_CAP, lake(24, 24, -1, -1, 0, false), 0f);
        assertTrue(lake(24, 24, -1, -1, 4, false) > 3 * Tuning.DAMAGE_START);
        assertTrue(lake(24, 24, -1, -1, 8, false) > Tuning.DAMAGE_START);
        assertTrue(lake(24, 24, -1, -1, 12, false) > Tuning.VISUAL_START);
        assertTrue(lake(24, 24, -1, -1, 19, false) < Tuning.VISUAL_START);
    }

    @Test
    void aLakeBelowTheBankIsStillFeltFromSixBlocks() {
        assertTrue(lake(24, 24, -2, -1, 6, false) > Tuning.DAMAGE_START);
        assertTrue(lake(24, 24, -3, -1, 6, false) > Tuning.VISUAL_START);
    }

    @Test
    void aSmallPoolWarmsYouFromACoupleOfBlocksNotFromAcrossTheMap() {
        float adjacent = lake(3, 3, -1, -1, 0, false);
        float far = lake(3, 3, -1, -1, 10, false);
        assertTrue(adjacent > Tuning.VISUAL_START, "adjacent=" + adjacent);
        assertTrue(far < Tuning.DEAD_ZONE, "far=" + far);
    }

    @Test
    void aWallCutsHeatButDoesNotRemoveIt() {
        float open = lake(24, 24, -1, -1, 3, false);
        float walled = lake(24, 24, -1, -1, 3, true);
        assertTrue(walled < open * 0.6f, "open=" + open + " walled=" + walled);
        assertTrue(walled > 0f);
    }

    @Test
    void thickerSolidsTransmitLess() {
        assertEquals(1f, Occlusion.transmission(0f), 0f);
        assertTrue(Occlusion.transmission(0.6f) > Occlusion.transmission(1.8f));
        assertEquals(Tuning.OCCLUSION_FLOOR, Occlusion.transmission(50f), 0f);
    }

    @Test
    void campfireWarmsAColdBiomeToComfort() {
        float cold = AmbientModel.environmentTarget(outdoors(0f, false, 70));
        float campfire = HeatFalloff.contribution(ShippedSources.CAMPFIRE_STRENGTH, 1.5f, 1f);
        assertTrue(Math.abs(cold + campfire) < Tuning.DEAD_ZONE, "net=" + (cold + campfire));
    }

    private static float blendOf(float center, float surroundings) {
        float[] temps = new float[BiomeBlend.sampleCount()];
        java.util.Arrays.fill(temps, surroundings);
        temps[0] = center;
        return BiomeBlend.blend(temps);
    }

    @Test
    void uniformSurroundingsBlendToThemselves() {
        assertEquals(0.8f, blendOf(0.8f, 0.8f), 1e-5f);
    }

    @Test
    void aWarmPeakSurroundedByColdBiomesFeelsCold() {
        float blended = blendOf(1.0f, -0.3f);
        assertTrue(blended < 0.2f, "blended=" + blended);
        float target = AmbientModel.environmentTarget(
                new AmbientModel.Inputs(blended, false, false, false, 140f, 1f, 0f));
        assertTrue(target < -Tuning.VISUAL_START, "target=" + target);
        float unblended = AmbientModel.environmentTarget(
                new AmbientModel.Inputs(1.0f, false, false, false, 140f, 1f, 0f));
        assertTrue(unblended > target + 20f);
    }

    @Test
    void ringsReachAFewChunksOut() {
        int farthest = 0;
        for (int i = 0; i < BiomeBlend.sampleCount(); i++) {
            farthest = Math.max(farthest, Math.max(Math.abs(BiomeBlend.offsetX(i)), Math.abs(BiomeBlend.offsetZ(i))));
        }
        assertEquals(64, farthest);
    }

    @Test
    void sprintingOnlyEasesColdAndNeverOverheats() {
        assertEquals(Tuning.SPRINT_WARMTH, Activity.sprintWarmth(-48f, true), 0f);
        assertEquals(3f, Activity.sprintWarmth(-3f, true), 0f);
        assertEquals(0f, Activity.sprintWarmth(-48f, false), 0f);
        assertEquals(0f, Activity.sprintWarmth(30f, true), 0f);
        assertEquals(0f, Activity.sprintWarmth(0f, true), 0f);
    }

    @Test
    void aSnowCoveredPatchOfPlainsFeelsClearlyColder() {
        float none = AmbientModel.environmentTarget(snowy(0.8f, 0f));
        float covered = AmbientModel.environmentTarget(snowy(0.8f, 1f));
        assertTrue(covered < none - 25f, "none=" + none + " covered=" + covered);
    }

    @Test
    void beingOnFireHeatsLikeStandingAtALavaLake() {
        assertTrue(Tuning.ON_FIRE_HEAT > 3 * Tuning.DAMAGE_START);
        assertTrue(Tuning.ON_FIRE_HEAT <= Tuning.HEAT_SOURCE_CAP);
        int seconds = secondsToReach(0f, 0f, HeatFalloff.capped(Tuning.ON_FIRE_HEAT), 0f, Tuning.DAMAGE_START);
        assertTrue(seconds <= 8, "seconds=" + seconds);
    }

    @Test
    void groupCapsBoundManyWeakSourcesWithoutTouchingStrongOnes() {
        SourceAccumulator sums = new SourceAccumulator(new float[] {35f, 25f});
        for (int i = 0; i < 400; i++) {
            sums.add(0, -0.3f);
        }
        for (int i = 0; i < 110; i++) {
            sums.add(1, -1f);
        }
        sums.add(-1, -60f);
        sums.add(-1, 40f);
        assertEquals(-35f - 25f - 60f, sums.cold(), 1e-3f);
        assertEquals(40f, sums.heat(), 1e-3f);
    }

    @Test
    void aGroupBelowItsCapIsNotChanged() {
        SourceAccumulator sums = new SourceAccumulator(new float[] {35f});
        for (int i = 0; i < 10; i++) {
            sums.add(0, -0.3f);
        }
        assertEquals(-3f, sums.cold(), 1e-4f);
        assertEquals(0f, sums.heat(), 0f);
    }

    @Test
    void anUnknownGroupFallsBackToDirectSumming() {
        SourceAccumulator sums = new SourceAccumulator(new float[0]);
        sums.add(7, -5f);
        sums.add(-1, 2f);
        assertEquals(-5f, sums.cold(), 0f);
        assertEquals(2f, sums.heat(), 0f);
    }

    @Test
    void woolProtectsABitMoreAgainstColdAndNothingExtraInHeat() {
        assertEquals(Tuning.WOOL_WARMTH + Tuning.WOOL_COLD_BONUS, WoolInsulation.warmth(1f, -64f), 1e-4f);
        assertEquals(Tuning.WOOL_WARMTH, WoolInsulation.warmth(1f, 0f), 1e-4f);
        assertEquals(Tuning.WOOL_WARMTH, WoolInsulation.warmth(1f, 72f), 1e-4f);
        assertEquals(0f, WoolInsulation.warmth(0f, -64f), 0f);
        float half = WoolInsulation.warmth(1f, -20f);
        assertTrue(half > Tuning.WOOL_WARMTH && half < Tuning.WOOL_WARMTH + Tuning.WOOL_COLD_BONUS);
    }

    @Test
    void aFullWoolSetNowKeepsSnowyPlainsOutOfTheDiscomfortRange() {
        float weather = AmbientModel.environmentTarget(outdoors(0f, false, 70));
        float withWool = weather + WoolInsulation.warmth(1f, weather);
        assertTrue(withWool > -Tuning.VISUAL_START, "withWool=" + withWool);
        assertTrue(weather + WoolInsulation.warmth(0.5f, weather) < -Tuning.VISUAL_START);
    }

    @Test
    void aHeldTorchIsAboutAsWarmAsAPlacedOneNextToYou() {
        float held = HeatFalloff.contribution(5f, Tuning.HELD_SOURCE_DISTANCE, 1f);
        float placed = HeatFalloff.contribution(5f, 1f, 1f);
        assertTrue(held > placed && held < placed * 1.3f, "held=" + held + " placed=" + placed);
    }

    @Test
    void heldTorchRoomPowerCountsOnlyThroughTheRoomsRetention() {
        Room outdoors = Room.NONE;
        Room sealed = new Room(75, 0, 0, 110, 0f, 0f, false);
        assertEquals(0f, 6f * outdoors.retention(), 1e-6f);
        assertTrue(6f * sealed.retention() > 3f);
    }
}
