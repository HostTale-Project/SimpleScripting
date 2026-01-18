package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.util.MessageHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Player command class that executes a JavaScript handler function.
 * Extends AbstractPlayerCommand to ensure proper thread handling and PlayerRef access.
 */
public class JavaScriptCommand extends AbstractPlayerCommand {
    private final Scriptable scope;
    private Function handler;
    // Store argument objects (RequiredArg or OptionalArg) - we use Object since they share no common interface
    private final Map<String, Object> argumentMap = new HashMap<>();

    public JavaScriptCommand(String name, String description, Scriptable scope) {
        super(name, description);
        this.scope = scope;
    }

    public void setHandler(Function handler) {
        this.handler = handler;
    }

    public void setCommandPermissionGroup(GameMode gameMode) {
        setPermissionGroup(gameMode);
    }

    public void addOptionalStringArg(String name, String description) {
        Object arg = withOptionalArg(name, description, (ArgumentType) ArgTypes.STRING);
        argumentMap.put(name, arg);
    }

    public void addRequiredStringArg(String name, String description) {
        Object arg = withRequiredArg(name, description, (ArgumentType) ArgTypes.STRING);
        argumentMap.put(name, arg);
    }

    public void addOptionalIntArg(String name, String description) {
        Object arg = withOptionalArg(name, description, (ArgumentType) ArgTypes.INTEGER);
        argumentMap.put(name, arg);
    }

    public void addRequiredIntArg(String name, String description) {
        Object arg = withRequiredArg(name, description, (ArgumentType) ArgTypes.INTEGER);
        argumentMap.put(name, arg);
    }

    public void addOptionalDoubleArg(String name, String description) {
        Object arg = withOptionalArg(name, description, (ArgumentType) ArgTypes.DOUBLE);
        argumentMap.put(name, arg);
    }

    public void addRequiredDoubleArg(String name, String description) {
        Object arg = withRequiredArg(name, description, (ArgumentType) ArgTypes.DOUBLE);
        argumentMap.put(name, arg);
    }

    public void addJavaScriptSubCommand(JavaScriptCommand subCommand) {
        addSubCommand(subCommand);
    }
    
    /**
     * Gets the argument map for this command.
     * @return Map of argument names to argument objects
     */
    public Map<String, Object> getArgumentMap() {
        return argumentMap;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, 
                          @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, 
                          @Nonnull PlayerRef playerRef, 
                          @Nonnull World world) {
        if (handler == null) {
            return;
        }

        Context cx = Context.enter();
        try {
            // Create enhanced wrapper with player info and argument map
            CommandContextWrapper wrapper = new CommandContextWrapper(commandContext, store, ref, playerRef, world, argumentMap);
            Object jsContext = Context.javaToJS(wrapper, scope);
            handler.call(cx, scope, scope, new Object[]{jsContext});
        } catch (Exception e) {
            System.err.println("[JavaScriptCommand] Error: " + e.getMessage());
            commandContext.sender().sendMessage(Message.raw("§cError: " + e.getMessage()));
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }
}
