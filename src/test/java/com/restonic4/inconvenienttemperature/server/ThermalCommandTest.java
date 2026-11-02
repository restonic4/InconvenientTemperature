package com.restonic4.inconvenienttemperature.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.restonic4.inconvenienttemperature.core.ViewMode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThermalCommandTest {
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        dispatcher.register(ThermalCommand.tree());
    }

    private boolean runnable(String line) {
        ParseResults<CommandSourceStack> parse = dispatcher.parse(line, null);

        return !parse.getReader().canRead() && parse.getExceptions().isEmpty() && parse.getContext().getCommand() != null;
    }

    @Test
    void debugAndBlockStayUnderTheRoot() {
        assertTrue(runnable("inconvenient_temperature debug"));
        assertTrue(runnable("inconvenient_temperature block"));
    }

    @Test
    void renderCyclesWithoutAnArgument() {
        assertTrue(runnable("inconvenient_temperature render"));
    }

    @Test
    void renderAcceptsEveryMode() {
        for (ViewMode mode : ViewMode.values()) {
            assertTrue(runnable("inconvenient_temperature render " + mode.commandName()), mode.name());
        }
    }

    @Test
    void unknownCategoriesAndModesAreRejected() {
        assertFalse(runnable("inconvenient_temperature render lava"));
        assertFalse(runnable("inconvenient_temperature itview"));
        assertFalse(runnable("inconvenient_temperature"));
        assertNotNull(dispatcher.getRoot().getChild("inconvenient_temperature").getChild("render"));
    }
}
