package com.restonic4.inconvenienttemperature.client.view;

import com.mojang.blaze3d.platform.InputConstants;
import com.restonic4.inconvenienttemperature.network.RenderModePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class HeatViewClient {
    private static final KeyMapping CYCLE = new KeyMapping(
		"key.inconvenient_temperature.heat_view",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_UNKNOWN,
		"key.categories.inconvenient_temperature"
	);

    private HeatViewClient() {}

    public static void register() {
        KeyBindingHelper.registerKeyBinding(CYCLE);
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (CYCLE.consumeClick()) {
                HeatView.cycle();
            }
            HeatView.tick(minecraft);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(HeatViewRenderer::render);
        ClientPlayNetworking.registerGlobalReceiver(
            RenderModePayload.TYPE,
            (payload, context) -> payload.mode().ifPresentOrElse(HeatView::setMode, HeatView::cycle)
        );
    }
}
