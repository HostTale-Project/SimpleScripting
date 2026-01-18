package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.util.MessageHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * JavaScript-friendly wrapper for CommandContext.
 * Provides simplified access to command context data including player information.
 */
public class CommandContextWrapper {
    private final CommandContext context;
    private final Store<EntityStore> store;
    private final Ref<EntityStore> ref;
    private final PlayerRef playerRef;
    private final World world;
    // Stores RequiredArg/OptionalArg objects - we use Object since they share no common interface
    private final Map<String, Object> argumentMap;

    /**
     * Creates a wrapper with full player context (for player commands).
     */
    public CommandContextWrapper(@Nonnull CommandContext context, 
                                 @Nonnull Store<EntityStore> store, 
                                 @Nonnull Ref<EntityStore> ref, 
                                 @Nonnull PlayerRef playerRef, 
                                 @Nonnull World world,
                                 @Nonnull Map<String, Object> argumentMap) {
        this.context = context;
        this.store = store;
        this.ref = ref;
        this.playerRef = playerRef;
        this.world = world;
        this.argumentMap = argumentMap;
    }

    /**
     * Creates a wrapper without player context (for console commands).
     */
    public CommandContextWrapper(@Nonnull CommandContext context) {
        this.context = context;
        this.store = null;
        this.ref = null;
        this.playerRef = null;
        this.world = null;
        this.argumentMap = new HashMap<>();
    }

    /**
     * Gets the command sender (usually a player).
     * @return The command sender
     */
    public CommandSender getSender() {
        return context.sender();
    }

    /**
     * Gets an argument value by name using the stored argument objects.
     * Uses reflection to call .get(context) since RequiredArg and OptionalArg share no common interface.
     * @param name The argument name
     * @return The argument value, or null if not found
     */
    @Nullable
    public Object getArg(String name) {
        Object argObj = argumentMap.get(name);
        if (argObj == null) {
            System.err.println("[CommandContextWrapper] Argument '" + name + "' not found in argumentMap. Available: " + argumentMap.keySet());
            return null;
        }
        
        try {
            // Use reflection to call .get(CommandContext) on the argument object
            Method getMethod = argObj.getClass().getMethod("get", CommandContext.class);
            Object result = getMethod.invoke(argObj, context);
            return result;
        } catch (Exception e) {
            System.err.println("[CommandContextWrapper] Error getting argument '" + name + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets an argument value as a string.
     * @param name The argument name
     * @return The argument value as string, or null if not found
     */
    @Nullable
    public String getArgAsString(String name) {
        System.out.println("[CommandContextWrapper] getArgAsString('" + name + "') called, argumentMap keys: " + argumentMap.keySet());
        Object value = getArg(name);
        System.out.println("[CommandContextWrapper] getArg returned: " + value);
        String result = value != null ? value.toString() : null;
        System.out.println("[CommandContextWrapper] returning: " + result);
        return result;
    }

    /**
     * Gets an argument value as a string with a default.
     * @param name The argument name
     * @param defaultValue The default value if argument not found
     * @return The argument value or default
     */
    public String getArgAsString(String name, String defaultValue) {
        String value = getArgAsString(name);
        System.out.println("[CommandContextWrapper] getArgAsString with default, value='" + value + "', default='" + defaultValue + "'");
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    /**
     * Gets an argument value as an integer.
     * @param name The argument name
     * @return The argument value as integer, or null if not found or invalid
     */
    @Nullable
    public Integer getArgAsInt(String name) {
        Object value = getArg(name);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets an argument value as an integer with a default.
     * @param name The argument name
     * @param defaultValue The default value if argument not found
     * @return The argument value or default
     */
    public int getArgAsInt(String name, int defaultValue) {
        Integer value = getArgAsInt(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets an argument value as a double.
     * @param name The argument name
     * @return The argument value as double, or null if not found or invalid
     */
    @Nullable
    public Double getArgAsDouble(String name) {
        Object value = getArg(name);
        if (value == null) return null;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets an argument value as a double with a default.
     * @param name The argument name
     * @param defaultValue The default value if argument not found
     * @return The argument value or default
     */
    public double getArgAsDouble(String name, double defaultValue) {
        Double value = getArgAsDouble(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks if an argument was provided.
     * @param name The argument name
     * @return true if the argument exists and is not null
     */
    public boolean hasArg(String name) {
        return getArg(name) != null;
    }

    /**
     * Gets the underlying CommandContext (for advanced usage).
     * @return The wrapped CommandContext
     */
    public CommandContext getContext() {
        return context;
    }

    /**
     * Sends a message to the command sender.
     * Supports & color codes (e.g., &a for green, &c for red).
     * @param message The message text to send (with optional color codes)
     */
    public void sendMessage(String message) {
        context.sender().sendMessage(MessageHelper.colorize(message));
    }

    // ========================================================================
    // PLAYER CONTEXT ACCESSORS
    // ========================================================================

    /**
     * Gets the PlayerRef for the executing player.
     * @return The PlayerRef, or null if not a player command
     */
    @Nullable
    public PlayerRef getPlayer() {
        return playerRef;
    }

    /**
     * Gets the entity store for the player.
     * @return The Store, or null if not a player command
     */
    @Nullable
    public Store<EntityStore> getStore() {
        return store;
    }

    /**
     * Gets the entity reference for the player.
     * @return The Ref, or null if not a player command
     */
    @Nullable
    public Ref<EntityStore> getRef() {
        return ref;
    }

    /**
     * Gets the world the player is in.
     * @return The World, or null if not a player command
     */
    @Nullable
    public World getWorld() {
        return world;
    }

    /**
     * Checks if this is a player command (has player context).
     * @return true if executing player context is available
     */
    public boolean isPlayerCommand() {
        return playerRef != null;
    }
}
