package com.restonic4.inconvenienttemperature;

import com.restonic4.inconvenienttemperature.item.ModItems;
import com.restonic4.inconvenienttemperature.network.ModNetworking;
import com.restonic4.inconvenienttemperature.server.ThermalCommand;
import com.restonic4.inconvenienttemperature.server.ThermalServer;
import com.restonic4.inconvenienttemperature.world.HeatSourceLoader;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class InconvenientTemperature implements ModInitializer {
    public static final String MOD_ID = "inconvenient_temperature";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModAttachments.register();
        ModEffects.register();
        ModItems.register();
        ModNetworking.register();
        HeatSourceLoader.register();
        ThermalServer.register();
        ThermalCommand.register();
    }
}
