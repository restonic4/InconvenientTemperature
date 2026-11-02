package com.restonic4.inconvenienttemperature.gametest;

import com.restonic4.inconvenienttemperature.core.Room;
import com.restonic4.inconvenienttemperature.core.RoomAnalyzer;
import com.restonic4.inconvenienttemperature.world.HeatSourceRegistry;
import com.restonic4.inconvenienttemperature.world.HeldSources;
import com.restonic4.inconvenienttemperature.world.LevelRoomWorld;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class WorldGameTests implements FabricGameTest {
    private static final BlockState WALL = Blocks.STONE_BRICKS.defaultBlockState();
    private static final int SIZE = 14;

    private final List<String> failures = new ArrayList<>();

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void sources(GameTestHelper helper) {
        expectSource("lit campfire", Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true), 75f, 40f);
        expectSource("unlit campfire", Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), 0f, 0f);
        expectSource("torch", Blocks.TORCH.defaultBlockState(), 5f, 6f);
        expectSource("wall torch", Blocks.WALL_TORCH.defaultBlockState(), 5f, 6f);
        expectSource("lit furnace", Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true), 14f, 20f);
        expectSource("unlit furnace", Blocks.FURNACE.defaultBlockState(), 0f, 0f);
        expectSource("lit candle (tag entry)", Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true), 2f, 2.5f);
        expectSource("unlit candle", Blocks.CANDLE.defaultBlockState(), 0f, 0f);
        expectSource("lava source (fluid entry)", Blocks.LAVA.defaultBlockState(), 16f, 60f);
        expectSource("magma", Blocks.MAGMA_BLOCK.defaultBlockState(), 6f, 30f);
        expectSource("ice", Blocks.ICE.defaultBlockState(), -1.5f, -1.5f);
        expectSource("packed ice", Blocks.PACKED_ICE.defaultBlockState(), -2.5f, -2.5f);
        expectSource("blue ice", Blocks.BLUE_ICE.defaultBlockState(), -4f, -4f);
        expectSource("snow block", Blocks.SNOW_BLOCK.defaultBlockState(), -1f, 0f);
        expectSource("snow layer", Blocks.SNOW.defaultBlockState(), -0.3f, 0f);
        expectSource("snow layer, 8 layers", Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 8), -0.3f, 0f);
        expectCap("snow layer", Blocks.SNOW.defaultBlockState(), 35f);
        expectCap("snow block", Blocks.SNOW_BLOCK.defaultBlockState(), 25f);
        expectCap("ice", Blocks.ICE.defaultBlockState(), 80f);
        expectCap("blue ice", Blocks.BLUE_ICE.defaultBlockState(), 100f);
        expect("lava has no cap", HeatSourceRegistry.value(Blocks.LAVA.defaultBlockState()).group() == -1, "lava is grouped");
        expectSource("stone is nothing", Blocks.STONE.defaultBlockState(), 0f, 0f);
        finish(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void heldTorches(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        HeldSources none = HeldSources.of(player);
        expect("empty hands give nothing", none.radiantHeat() == 0f && none.roomHeat() == 0f, none.toString());

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH, 64));
        HeldSources one = HeldSources.of(player);
        expect("one torch counts once, not per item", one.roomHeat() == 6f, one.toString());
        expect("one torch radiates", one.radiantHeat() > 3f && one.radiantHeat() < 5f, one.toString());

        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));
        HeldSources two = HeldSources.of(player);
        expect("two hands count as two torches", two.roomHeat() == 12f && Math.abs(two.radiantHeat() - 2f * one.radiantHeat()) < 1e-4f, two.toString());

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SOUL_TORCH));
        HeldSources mixed = HeldSources.of(player);
        expect("soul torch is weaker than a torch", mixed.roomHeat() == 9f, mixed.toString());

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LANTERN));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        HeldSources lantern = HeldSources.of(player);
        expect("a held lantern is not in the held tag", lantern.roomHeat() == 0f, lantern.toString());
        finish(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void rooms(GameTestHelper helper) {
        BlockState litCampfire = Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true);

        buildHut(helper, true);
        helper.setBlock(new BlockPos(3, 1, 3), litCampfire);
        Room sealed = analyze(helper, 3, 2, 3);
        expect("sealed hut cell count", sealed.cells() == 125, sealed.cells() + " cells");
        expect("sealed hut openness ~0", sealed.openness() < 0.02f, "openness " + sealed.openness());
        expect("campfire room power", sealed.heatPower() == 40f, "power " + sealed.heatPower());

        BlockState door = Blocks.OAK_DOOR.defaultBlockState();
        helper.setBlock(new BlockPos(3, 1, 0), door);
        helper.setBlock(new BlockPos(3, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        Room closed = analyze(helper, 3, 2, 3);
        expect("closed door keeps the room sealed", closed.openness() < 0.02f, "openness " + closed.openness());

        BlockState openDoor = door.setValue(BlockStateProperties.OPEN, true);
        helper.setBlock(new BlockPos(3, 1, 0), openDoor);
        helper.setBlock(new BlockPos(3, 2, 0), openDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        Room opened = analyze(helper, 3, 2, 3);
        expect("open door leaks moderately", opened.openness() > 0.15f && opened.openness() < 0.5f, "openness " + opened.openness());

        buildHut(helper, true);
        helper.setBlock(new BlockPos(0, 2, 3), Blocks.GLASS_PANE);
        Room pane = analyze(helper, 3, 2, 3);
        helper.setBlock(new BlockPos(0, 2, 3), Blocks.IRON_BARS);
        Room bars = analyze(helper, 3, 2, 3);
        helper.setBlock(new BlockPos(0, 2, 3), Blocks.GLASS);
        Room glass = analyze(helper, 3, 2, 3);
        expect("glass pane window is airtight", pane.openness() < 0.02f, "openness " + pane.openness());
        expect("glass block window is airtight", glass.openness() < 0.02f, "openness " + glass.openness());
        expect("iron bars leak a little", bars.openness() > 0.03f && bars.openness() < 0.25f, "openness " + bars.openness());

        buildHut(helper, true);
        helper.setBlock(new BlockPos(3, 6, 3), Blocks.AIR);
        Room hole = analyze(helper, 3, 2, 3);
        expect("roof hole counts as a small leak", hole.openness() > 0.08f && hole.openness() < 0.35f, "openness " + hole.openness());

        buildHut(helper, false);
        Room roofless = analyze(helper, 3, 2, 3);
        expect("roofless yard is outdoors", roofless.openness() > 0.9f, "openness " + roofless.openness());

        buildHut(helper, true);
        for (int i = 1; i <= 4; i++) {
            helper.setBlock(new BlockPos(i, 1, 1), Blocks.LAVA);
        }
        Room lava = analyze(helper, 3, 2, 4);
        expect("four exposed lava blocks add their room power", lava.heatPower() == 240f, "power " + lava.heatPower());
        expect("a lava room is deadly hot", lava.heatGain() > 70f, "gain " + lava.heatGain());
        finish(helper);
    }

    private void buildHut(GameTestHelper helper, boolean roof) {
        for (int x = -2; x <= SIZE; x++) {
            for (int y = 0; y <= 9; y++) {
                for (int z = -2; z <= SIZE; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), WALL);
                if (roof) {
                    helper.setBlock(new BlockPos(x, 6, z), WALL);
                }
                for (int y = 1; y <= 5; y++) {
                    boolean edge = x == 0 || x == 6 || z == 0 || z == 6;
                    helper.setBlock(new BlockPos(x, y, z), edge ? WALL : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private Room analyze(GameTestHelper helper, int x, int y, int z) {
        BlockPos start = helper.absolutePos(new BlockPos(x, y, z));
        return RoomAnalyzer.analyze(new LevelRoomWorld(helper.getLevel()), start.getX(), start.getY(), start.getZ());
    }

    private void expectSource(String label, BlockState state, float strength, float roomPower) {
        HeatSourceRegistry.Value value = HeatSourceRegistry.value(state);
        expect(label, value.strength() == strength && value.roomPower() == roomPower,
                "got " + value.strength() + "/" + value.roomPower() + " wanted " + strength + "/" + roomPower);
    }

    private void expectCap(String label, BlockState state, float cap) {
        HeatSourceRegistry.Value value = HeatSourceRegistry.value(state);
        float[] caps = HeatSourceRegistry.groupCaps();
        boolean ok = value.group() >= 0 && value.group() < caps.length && caps[value.group()] == cap;
        expect(label + " cap", ok, "group " + value.group() + " wanted cap " + cap);
    }

    private void expect(String label, boolean ok, String detail) {
        if (!ok) {
            failures.add(label + " -> " + detail);
        }
    }

    private void finish(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(failures);
        failures.clear();
        if (problems.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(String.join("; ", problems));
        }
    }
}
