package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.SimpleScriptingPlugin;
import com.hosttale.simplescripting.managers.ScriptRegistry;
import com.hosttale.simplescripting.util.Logger;
import com.hypixel.hytale.protocol.GameMode;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for JavaScript to register and manage custom commands.
 */
public class CommandManager {
    private final SimpleScriptingPlugin plugin;
    private final Scriptable scope;
    private final Logger logger;
    private final ScriptRegistry scriptRegistry;
    
    // Track registered commands for unregistration
    private final Map<String, JavaScriptCommand> registeredCommands;

    public CommandManager(SimpleScriptingPlugin plugin, Scriptable scope, Logger logger, ScriptRegistry scriptRegistry) {
        this.plugin = plugin;
        this.scope = scope;
        this.logger = logger;
        this.scriptRegistry = scriptRegistry;
        this.registeredCommands = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new command builder.
     * @return A command builder for fluent command creation
     */
    public CommandBuilder register() {
        return new CommandBuilder();
    }
    
    /**
     * Unregisters a command by name.
     * Note: This removes from our tracking but Hytale may not support dynamic unregistration.
     * @param commandName The command name to unregister
     * @return true if the command was tracked and removed
     */
    public boolean unregisterCommand(@Nonnull String commandName) {
        JavaScriptCommand cmd = registeredCommands.remove(commandName);
        if (cmd != null) {
            // Hytale's CommandRegistry may not support unregistration
            // But we can disable the command by clearing its handler
            cmd.setHandler(null);
            logger.info("Unregistered command: /" + commandName);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a command is registered.
     * @param commandName The command name
     * @return true if registered
     */
    public boolean isRegistered(@Nonnull String commandName) {
        return registeredCommands.containsKey(commandName);
    }
    
    /**
     * Gets the count of registered commands.
     * @return Number of registered commands
     */
    public int getRegisteredCount() {
        return registeredCommands.size();
    }

    /**
     * Builder class for creating commands with a fluent API.
     */
    public class CommandBuilder {
        private String name;
        private String description = "";
        private JavaScriptCommand command;

        /**
         * Sets the command name.
         * @param name The command name
         * @return This builder for chaining
         */
        public CommandBuilder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the command description.
         * @param description The command description
         * @return This builder for chaining
         */
        public CommandBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds an alias for the command.
         * @param alias The alias to add
         * @return This builder for chaining
         */
        public CommandBuilder addAlias(String alias) {
            ensureCommand();
            command.addAliases(alias);
            return this;
        }

        /**
         * Sets the permission group for the command.
         * @param gameMode The game mode required to use this command
         * @return This builder for chaining
         */
        public CommandBuilder setPermissionGroup(String gameMode) {
            ensureCommand();
            try {
                GameMode mode = GameMode.valueOf(gameMode);
                command.setCommandPermissionGroup(mode);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid game mode: " + gameMode + ". Using default.");
            }
            return this;
        }

        /**
         * Adds an optional string argument to the command.
         * @param argName The argument name
         * @param argDescription The argument description
         * @return This builder for chaining
         */
        public CommandBuilder addOptionalStringArg(String argName, String argDescription) {
            ensureCommand();
            command.addOptionalStringArg(argName, argDescription);
            return this;
        }

        /**
         * Adds a required string argument to the command.
         * @param argName The argument name
         * @param argDescription The argument description
         * @return This builder for chaining
         */
        public CommandBuilder addRequiredStringArg(String argName, String argDescription) {
            ensureCommand();
            command.addRequiredStringArg(argName, argDescription);
            return this;
        }

        /**
         * Adds an optional integer argument to the command.
         * @param argName The argument name
         * @param argDescription The argument description
         * @return This builder for chaining
         */
        public CommandBuilder addOptionalIntArg(String argName, String argDescription) {
            ensureCommand();
            command.addOptionalIntArg(argName, argDescription);
            return this;
        }

        /**
         * Adds a required integer argument to the command.
         * @param argName The argument name
         * @param argDescription The argument description
         * @return This builder for chaining
         */
        public CommandBuilder addRequiredIntArg(String argName, String argDescription) {
            ensureCommand();
            command.addRequiredIntArg(argName, argDescription);
            return this;
        }

        /**
         * Adds an optional double argument to the command.
         * @param argName The argument name
         * @param argDescription The argument description
         * @return This builder for chaining
         */
        public CommandBuilder addOptionalDoubleArg(String argName, String argDescription) {
            ensureCommand();
            command.addOptionalDoubleArg(argName, argDescription);
            return this;
        }

        /**
         * Adds a required double argument to the command.
         * @param argName The argument name
         * @param argDescription The argument description
         * @return This builder for chaining
         */
        public CommandBuilder addRequiredDoubleArg(String argName, String argDescription) {
            ensureCommand();
            command.addRequiredDoubleArg(argName, argDescription);
            return this;
        }

        /**
         * Adds a subcommand to this command.
         * @param subCommand The subcommand builder
         * @return This builder for chaining
         */
        public CommandBuilder addSubCommand(CommandBuilder subCommand) {
            ensureCommand();
            if (subCommand.command != null) {
                command.addJavaScriptSubCommand(subCommand.command);
            }
            return this;
        }

        /**
         * Sets the handler function for this command.
         * @param handler The JavaScript function to execute
         * @return This builder for chaining
         */
        public CommandBuilder setHandler(Function handler) {
            ensureCommand();
            command.setHandler(handler);
            plugin.getCommandRegistry().registerCommand(command);
            registeredCommands.put(name, command);
            
            // Track command in script registry
            if (scriptRegistry != null) {
                scriptRegistry.recordCommand(name);
            }
            
            logger.info("Registered command: /" + name);
            return this;
        }

        /**
         * Ensures the command object is created.
         */
        private void ensureCommand() {
            if (command == null) {
                if (name == null || name.isEmpty()) {
                    throw new IllegalStateException("Command name must be set before other properties");
                }
                command = new JavaScriptCommand(name, description, scope);
            }
        }
    }
}
