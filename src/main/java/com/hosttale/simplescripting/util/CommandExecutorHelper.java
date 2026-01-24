package com.hosttale.simplescripting.util;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;

/**
 * Helper class for executing server commands from JavaScript.
 * Allows scripts to dispatch commands through the server's console sender.
 */
public class CommandExecutorHelper {
    private final Logger logger;

    public CommandExecutorHelper(Logger logger) {
        this.logger = logger;
    }

    /**
     * Dispatches a command as the server console.
     * 
     * @param command The command to execute (without the leading /)
     * @return true if the command was dispatched successfully
     */
    public boolean executeAsConsole(@Nonnull String command) {
        try {
            String cmdString = command.startsWith("/") ? command.substring(1) : command;
            
            logger.warning("Dispatching server command: /" + cmdString);

            CommandManager.get().handleCommand(ConsoleSender.INSTANCE, cmdString);
            return true;
        } catch (Exception e) {
            logger.severe("Error executing command '" + command + "': " + e.getMessage());
            return false;
        }
    }
}
