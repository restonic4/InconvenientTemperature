package com.restonic4.inconvenienttemperature.client.view;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.restonic4.inconvenienttemperature.core.RoomListener;
import com.restonic4.inconvenienttemperature.core.ViewMode;
import java.util.Comparator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.Vec3;

final class HeatViewRenderer {
    private static final double PLATE = 0.03;
    private static final double INSET = 0.04;
    private static final float FEET_TEXT_SCALE = 0.035f;
    private static final float DETAIL_TEXT_SCALE = 0.016f;
    private static final int TEXT_LIMIT = 24;
    private static final double TEXT_RANGE = 12.0;
    private static final int ROOM_FLOOR = 0x40_26A69A;
    private static final int OUTLET = 0x80_E53935;
    private static final int LEAKY = 0x70_FFB300;

    private HeatViewRenderer() {}

    static void render(WorldRenderContext context) {
        ViewMode mode = HeatView.mode();
        if (mode == ViewMode.OFF || HeatView.unavailable() || context.consumers() == null) return;

        PoseStack pose = context.matrixStack();
        MultiBufferSource buffers = context.consumers();
        Vec3 camera = context.camera().getPosition();
        RoomSnapshot room = HeatView.room();

        if (mode.heat()) {
            for (HeatMap.Tile tile : HeatView.map().tiles()) {
                floorTile(pose, buffers, camera, tile.x(), tile.y(), tile.z(), tile.argb());
            }
        } else {
            for (var floor : room.floors) {
                floorTile(pose, buffers, camera, floor.getX(), floor.getY(), floor.getZ(), ROOM_FLOOR);
            }
        }
        if (mode.rooms()) {
            for (RoomSnapshot.Leak leak : room.leaks) {
                plate(pose, buffers, camera, leak, leak.kind() == RoomListener.Face.OUTLET ? OUTLET : LEAKY);
            }
        }
        sources(pose, buffers, camera, room, mode.rooms());
        feetLabel(context, pose, buffers);
    }

    private static void feetLabel(WorldRenderContext context, PoseStack pose, MultiBufferSource buffers) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Vec3 at = player.getPosition(context.tickCounter().getGameTimeDeltaPartialTick(false));
        HeatView.Readout r = HeatView.readout();
        double floor = Math.floor(at.y);
        int color = r.total() > 2f ? 0xFF8A65 : r.total() < -2f ? 0x80D8FF : 0xFFFFFF;
        DebugRenderer.renderFloatingText(
			pose, buffers, String.format("%+.1f", r.total()),
			at.x, floor + 0.5, at.z, color, FEET_TEXT_SCALE
		);
        DebugRenderer.renderFloatingText(
			pose, buffers,
			String.format(
				"near %+.1f/%+.1f  room %+.1f/%+.1f",
				r.nearbyHeat(), r.nearbyCold(), r.roomHeat(), r.roomCold()
			),
			at.x, floor + 0.22, at.z, 0xC0C0C0, DETAIL_TEXT_SCALE
		);
    }

    private static void floorTile(
		PoseStack pose, MultiBufferSource buffers, Vec3 camera,
		int x, int y, int z, int argb
	) {
        box(pose, buffers, camera, x + INSET, y + 0.01, z + INSET, x + 1 - INSET, y + 0.04, z + 1 - INSET, argb);
    }

    private static void plate(
		PoseStack pose, MultiBufferSource buffers, Vec3 camera,
		RoomSnapshot.Leak leak, int argb
	) {
        double x1 = leak.x() + INSET;
        double x2 = leak.x() + 1 - INSET;
        double y1 = leak.y() + INSET;
        double y2 = leak.y() + 1 - INSET;
        double z1 = leak.z() + INSET;
        double z2 = leak.z() + 1 - INSET;
        if (leak.dx() != 0) {
            x1 = leak.dx() > 0 ? leak.x() + 1 - PLATE : leak.x();
            x2 = x1 + PLATE;
        } else if (leak.dy() != 0) {
            y1 = leak.dy() > 0 ? leak.y() + 1 - PLATE : leak.y();
            y2 = y1 + PLATE;
        } else {
            z1 = leak.dz() > 0 ? leak.z() + 1 - PLATE : leak.z();
            z2 = z1 + PLATE;
        }
        box(pose, buffers, camera, x1, y1, z1, x2, y2, z2, argb);
    }

    private static void sources(
		PoseStack pose, MultiBufferSource buffers, Vec3 camera,
		RoomSnapshot room, boolean labels
	) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (RoomSnapshot.Source source : room.sources) {
            boolean heat = source.power() > 0f;
            LevelRenderer.renderLineBox(
				pose, lines,
				source.x() - camera.x, source.y() - camera.y, source.z() - camera.z,
				source.x() + 1 - camera.x, source.y() + 1 - camera.y, source.z() + 1 - camera.z,
				heat ? 1f : 0.3f, heat ? 0.85f : 0.9f, heat ? 0.1f : 1f, 1f
			);
        }
        if (!labels) return;

        room.sources.stream()
                .filter(s -> camera.distanceToSqr(s.x() + 0.5, s.y() + 0.5, s.z() + 0.5) < TEXT_RANGE * TEXT_RANGE)
                .sorted(Comparator.comparingDouble(s -> camera.distanceToSqr(s.x() + 0.5, s.y() + 0.5, s.z() + 0.5)))
                .limit(TEXT_LIMIT)
                .forEach(s -> DebugRenderer.renderFloatingText(pose, buffers,
                        String.format("%+.1f", s.power()), s.x() + 0.5, s.y() + 1.25, s.z() + 0.5, 0xFFFFFF));
    }

    private static void box(
		PoseStack pose, MultiBufferSource buffers, Vec3 camera,
		double x1, double y1, double z1, double x2, double y2, double z2, int argb
	) {
        DebugRenderer.renderFilledBox(
			pose, buffers,
			x1 - camera.x, y1 - camera.y, z1 - camera.z, x2 - camera.x, y2 - camera.y, z2 - camera.z,
			((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f,
			((argb >>> 24) & 0xFF) / 255f
		);
    }
}
