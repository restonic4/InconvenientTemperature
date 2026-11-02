package com.restonic4.inconvenienttemperature.server;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DebugCommand {
    private DebugCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("inconvenient_temperature")
				.then(Commands.literal("debug").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					for (String line : ThermalServer.describe(player)) {
						context.getSource().sendSuccess(() -> Component.literal(line), false);
					}
					return 1;
				}))
				.then(Commands.literal("block").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					for (String line : ThermalServer.describeBlock(player)) {
						context.getSource().sendSuccess(() -> Component.literal(line), false);
					}
					return 1;
				})))
		);
    }
}
