package com.hosttale.simplescripting.mod.runtime.api.commands;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.ModRegistrationTracker;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public final class CommandsApi {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]+(:[a-z0-9_-]+)?$");

    private final String modId;
    private final CommandRegistry commandRegistry;
    private final JsModRuntime runtime;
    private final ModRegistrationTracker registrationTracker;
    private final HytaleLogger logger;
    private final PlayersApi playersApi;
    private final AtomicInteger idSequence = new AtomicInteger();
    private final Map<String, CommandRegistration> registrations = new ConcurrentHashMap<>();

    public CommandsApi(String modId,
                       CommandRegistry commandRegistry,
                       JsModRuntime runtime,
                       ModRegistrationTracker registrationTracker,
                       HytaleLogger logger,
                       PlayersApi playersApi) {
        this.modId = modId;
        this.commandRegistry = commandRegistry;
        this.runtime = runtime;
        this.registrationTracker = registrationTracker;
        this.logger = logger.getSubLogger("commands");
        this.playersApi = playersApi;
    }

    public String register(String name, Function handler) {
        return register(name, handler, null);
    }

    public String register(String name, Function handler, Object options) {
        if (handler == null) {
            throw new IllegalArgumentException("commands.register requires a handler function.");
        }
        NameParts nameParts = normalizeName(name);
        CommandOptions parsed = parseOptions(options, nameParts.commandName());

        JsCommand command = new JsCommand(
                nameParts.commandName(),
                nameParts.aliasOptional().orElse(null),
                parsed.description,
                parsed.allowExtraArgs,
                handler,
                runtime,
                logger,
                playersApi
        );
        nameParts.aliasOptional().ifPresent(command::addAliasInternal);
        if (parsed.permission != null && !parsed.permission.isBlank()) {
            command.requirePermission(parsed.permission);
        }

        try {
            CommandRegistration registration = commandRegistry.registerCommand(command);
            String handle = modId + "-cmd-" + idSequence.incrementAndGet();
            registrations.put(handle, registration);
            registrationTracker.trackRegistration(registration);
            return handle;
        } catch (Exception e) {
            logger.atSevere().log("Failed to register command '%s': %s", nameParts.commandName(), e.getMessage());
            throw new IllegalArgumentException("Failed to register command '" + nameParts.commandName() + "': " + e.getMessage(), e);
        }
    }

    public void unregister(String handle) {
        CommandRegistration registration = registrations.remove(handle);
        if (registration != null) {
            registration.unregister();
        }
    }

    public void clear() {
        registrations.values().forEach(CommandRegistration::unregister);
        registrations.clear();
    }

    private NameParts normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command name is required.");
        }
        String candidate = name.toLowerCase(Locale.ROOT);
        if (candidate.contains(":")) {
            if (!NAME_PATTERN.matcher(candidate).matches()) {
                throw new IllegalArgumentException("Invalid command name '" + name + "'. Use [a-z0-9_-] and optionally namespace as modId:command.");
            }
            return new NameParts(candidate, null);
        }
        if (!NAME_PATTERN.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Invalid command name '" + name + "'. Use [a-z0-9_-] for un-namespaced names.");
        }
        String namespaced = modId + ":" + candidate;
        return new NameParts(candidate, namespaced);
    }

    private CommandOptions parseOptions(Object options, String commandName) {
        if (!(options instanceof Scriptable scriptable)) {
            return CommandOptions.defaultOptions(commandName);
        }
        String description = getString(scriptable, "description", CommandOptions.defaultOptions(commandName).description);
        String permission = getString(scriptable, "permission", null);
        boolean allowExtraArgs = getBoolean(scriptable, "allowExtraArgs", false);
        return new CommandOptions(description, permission, allowExtraArgs);
    }

    private String getString(Scriptable scriptable, String key, String fallback) {
        Object raw = ScriptableObject.getProperty(scriptable, key);
        if (raw == null || raw == Scriptable.NOT_FOUND) {
            return fallback;
        }
        return raw.toString();
    }

    private boolean getBoolean(Scriptable scriptable, String key, boolean fallback) {
        Object raw = ScriptableObject.getProperty(scriptable, key);
        if (raw == null || raw == Scriptable.NOT_FOUND) {
            return fallback;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        return fallback;
    }

    private record CommandOptions(String description, String permission, boolean allowExtraArgs) {
        private static CommandOptions defaultOptions(String commandName) {
            return new CommandOptions("JS command " + commandName, null, false);
        }
    }

    private record NameParts(String commandName, String alias) {
        public java.util.Optional<String> aliasOptional() {
            return alias == null ? java.util.Optional.empty() : java.util.Optional.of(alias);
        }
    }

    private static final class JsCommand extends AbstractCommand {
        private final Function handler;
        private final JsModRuntime runtime;
        private final HytaleLogger logger;
        private final PlayersApi playersApi;
        private final java.util.List<String> commandNames;

        JsCommand(String name, String alias, String description, boolean allowExtraArgs, Function handler, JsModRuntime runtime, HytaleLogger logger, PlayersApi playersApi) {
            super(name, description); // Avoid the confirm/flag behavior tied to the 3-arg constructor.
            setAllowsExtraArguments(true); // Always accept extra args to avoid built-in confirm prompts.
            this.handler = handler;
            this.runtime = runtime;
            this.logger = logger.getSubLogger("cmd-" + name);
            this.playersApi = playersApi;
            if (alias != null && !alias.isBlank()) {
                addAliases(alias);
            }
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            names.add(name);
            if (alias != null && !alias.isBlank()) {
                names.add(alias);
            }
            this.commandNames = java.util.List.copyOf(names);
        }

        void addAliasInternal(String alias) {
            addAliases(alias);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext commandContext) {
            try {
                ParsedArgs parsed = parseArgs(commandContext.getInputString());
                runtime.callFunction(handler, new JsCommandContext(commandContext, playersApi, parsed.args(), parsed.raw()));
            } catch (Exception e) {
                logger.atSevere().log("Command handler failed: %s", e.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        private ParsedArgs parseArgs(String input) {
            if (input == null || input.isBlank()) {
                return new ParsedArgs(new String[0], "");
            }
            String[] tokens = input.trim().split("\\s+");
            if (tokens.length == 0) {
                return new ParsedArgs(new String[0], "");
            }
            int start = 0;
            String first = strip(tokens[0]);
            for (String name : commandNames) {
                if (first.equalsIgnoreCase(strip(name))) {
                    start = 1;
                    break;
                }
            }
            if (start >= tokens.length) {
                return new ParsedArgs(new String[0], "");
            }
            int remaining = tokens.length - start;
            String[] args = new String[remaining];
            System.arraycopy(tokens, start, args, 0, remaining);
            String raw = String.join(" ", args);
            return new ParsedArgs(args, raw);
        }

        private String strip(String token) {
            if (token == null) {
                return "";
            }
            return token.startsWith("/") ? token.substring(1) : token;
        }

        private record ParsedArgs(String[] args, String raw) {
        }
    }
}
