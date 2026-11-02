package com.restonic4.inconvenienttemperature.server;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.restonic4.inconvenienttemperature.core.ViewMode;
import com.restonic4.inconvenienttemperature.network.RenderModePayload;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ThermalCommand {
    public static final String ROOT = "inconvenient_temperature";

    private ThermalCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(tree())
        );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        LiteralArgumentBuilder<CommandSourceStack> render = Commands.literal("render")
            .executes(context -> sendRenderMode(context, Optional.empty()));

        for (ViewMode mode : ViewMode.values()) {
            render.then(Commands.literal(mode.commandName())
                .executes(context -> sendRenderMode(context, Optional.of(mode))));
        }

        return Commands.literal(ROOT)
            .then(Commands.literal("debug").executes(context -> report(context, ThermalServer::describe)))
            .then(Commands.literal("block").executes(context -> report(context, ThermalServer::describeBlock)))
            .then(render);
    }

    private static int report(
        CommandContext<CommandSourceStack> context,
        Function<ServerPlayer, List<String>> describe
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (String line : describe.apply(player)) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }

        return 1;
    }

    private static int sendRenderMode(
        CommandContext<CommandSourceStack> context,
        Optional<ViewMode> mode
    ) throws CommandSyntaxException {
        ServerPlayNetworking.send(context.getSource().getPlayerOrException(), new RenderModePayload(mode));
        return 1;
    }
}
