package com.restonic4.inconvenienttemperature.client;

import com.restonic4.inconvenienttemperature.client.view.HeatViewClient;
import com.restonic4.inconvenienttemperature.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.world.item.component.DyedItemColor;

public class InconvenientTemperatureClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ThermalClient::tick);
        HeatViewClient.register();
        ColorProviderRegistry.ITEM.register(
			(stack, tintIndex) -> tintIndex == 0 ? 0xFF000000 | DyedItemColor.getOrDefault(stack, ModItems.WOOL_COLOR) : -1,
			ModItems.all()
		);
    }
}
