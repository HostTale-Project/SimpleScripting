package com.hosttale.simplescripting.managers;

import com.hosttale.simplescripting.SimpleScriptingPlugin;
import com.hosttale.simplescripting.task.Scheduler;
import com.hosttale.simplescripting.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event manager for JavaScript event handling.
 * Provides event subscription and firing mechanisms.
 */
public class EventManager {
    private final SimpleScriptingPlugin plugin;
    private final Scriptable scope;
    private final Logger logger;
    
    // Event listeners map: eventName -> list of handlers
    private final Map<String, List<EventHandler>> listeners;
    
    // Player tracking for join/quit detection
    private final Set<UUID> knownPlayers;
    
    // Player position tracking for move events
    private final Map<UUID, Vector3d> playerPositions;
    
    // Player world tracking for world change events
    private final Map<UUID, String> playerWorlds;
    
    // Movement threshold for triggering move events (blocks)
    private static final double MOVE_THRESHOLD = 0.1;
    
    // Tick counter
    private long tickCount = 0;

    public EventManager(SimpleScriptingPlugin plugin, Scriptable scope, Logger logger) {
        this.plugin = plugin;
        this.scope = scope;
        this.logger = logger;
        this.listeners = new ConcurrentHashMap<>();
        this.knownPlayers = ConcurrentHashMap.newKeySet();
        this.playerPositions = new ConcurrentHashMap<>();
        this.playerWorlds = new ConcurrentHashMap<>();
    }

    /**
     * Registers an event listener.
     * @param eventName The event name (e.g., "playerJoin", "playerDeath")
     * @param handler The JavaScript function to call when the event fires
     * @return Handler ID that can be used to unregister
     */
    public String on(@Nonnull String eventName, @Nonnull Function handler) {
        String handlerId = UUID.randomUUID().toString();
        
        listeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
            .add(new EventHandler(handlerId, handler, false));
        
        logger.fine("Registered event handler for: " + eventName);
        return handlerId;
    }

    /**
     * Registers a one-time event listener that auto-removes after first call.
     * @param eventName The event name
     * @param handler The JavaScript function to call
     * @return Handler ID
     */
    public String once(@Nonnull String eventName, @Nonnull Function handler) {
        String handlerId = UUID.randomUUID().toString();
        
        listeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
            .add(new EventHandler(handlerId, handler, true));
        
        return handlerId;
    }

    /**
     * Removes an event listener by handler ID.
     * @param eventName The event name
     * @param handlerId The handler ID returned by on() or once()
     * @return true if handler was removed
     */
    public boolean off(@Nonnull String eventName, @Nonnull String handlerId) {
        List<EventHandler> handlers = listeners.get(eventName);
        if (handlers != null) {
            return handlers.removeIf(h -> h.id.equals(handlerId));
        }
        return false;
    }

    /**
     * Removes all listeners for an event.
     * @param eventName The event name
     */
    public void removeAll(@Nonnull String eventName) {
        listeners.remove(eventName);
    }

    /**
     * Clears all event listeners.
     */
    public void clear() {
        listeners.clear();
        knownPlayers.clear();
        playerPositions.clear();
        playerWorlds.clear();
    }

    /**
     * Fires an event to all registered listeners.
     * @param eventName The event name
     * @param eventData The event data object
     * @return true if the event was cancelled (if cancellable)
     */
    public boolean fire(@Nonnull String eventName, @Nonnull NativeObject eventData) {
        List<EventHandler> handlers = listeners.get(eventName);
        if (handlers == null || handlers.isEmpty()) {
            return false;
        }

        // Add isCancelled flag for cancellable events
        boolean[] cancelled = {false};
        if (isCancellableEvent(eventName)) {
            eventData.put("cancelled", eventData, false);
            eventData.put("cancel", eventData, (Runnable) () -> {
                cancelled[0] = true;
                eventData.put("cancelled", eventData, true);
            });
        }

        List<EventHandler> toRemove = new ArrayList<>();
        
        for (EventHandler handler : handlers) {
            try {
                Context cx = Context.enter();
                try {
                    handler.function.call(cx, scope, scope, new Object[]{eventData});
                } finally {
                    Context.exit();
                }
                
                if (handler.once) {
                    toRemove.add(handler);
                }
            } catch (Exception e) {
                logger.severe("Error in event handler for " + eventName + ": " + e.getMessage());
            }
        }
        
        handlers.removeAll(toRemove);
        return cancelled[0];
    }

    /**
     * Checks if an event type is cancellable.
     */
    private boolean isCancellableEvent(String eventName) {
        return eventName.equals("playerChat") || eventName.equals("playerCommand");
    }

    /**
     * Called each tick to detect player-related events.
     * This should be called from an ISystem registered with the plugin.
     */
    public void tick() {
        tickCount++;
        
        // Fire tick event
        if (listeners.containsKey("tick")) {
            NativeObject tickData = createObject();
            tickData.put("tickCount", tickData, tickCount);
            fire("tick", tickData);
        }
        
        // Check for player join/quit/move events
        checkPlayerEvents();
    }

    /**
     * Checks for player-related events (join, quit, move, world change).
     */
    private void checkPlayerEvents() {
        Set<UUID> currentPlayers = new HashSet<>();
        
        for (PlayerRef player : Universe.get().getPlayers()) {
            UUID uuid = player.getUuid();
            currentPlayers.add(uuid);
            
            // Check for new players (join event)
            if (!knownPlayers.contains(uuid)) {
                knownPlayers.add(uuid);
                firePlayerJoin(player);
            }
            
            // Check for position changes (move event)
            checkPlayerMove(player);
            
            // Check for world changes
            checkPlayerWorldChange(player);
        }
        
        // Check for players who left (quit event)
        Set<UUID> leftPlayers = new HashSet<>(knownPlayers);
        leftPlayers.removeAll(currentPlayers);
        
        for (UUID uuid : leftPlayers) {
            knownPlayers.remove(uuid);
            playerPositions.remove(uuid);
            playerWorlds.remove(uuid);
            firePlayerQuit(uuid);
        }
    }

    /**
     * Fires a playerJoin event.
     */
    private void firePlayerJoin(PlayerRef player) {
        if (!listeners.containsKey("playerJoin")) return;
        
        NativeObject data = createObject();
        data.put("player", data, player);
        data.put("uuid", data, player.getUuid().toString());
        data.put("username", data, player.getUsername());
        fire("playerJoin", data);
    }

    /**
     * Fires a playerQuit event.
     */
    private void firePlayerQuit(UUID uuid) {
        if (!listeners.containsKey("playerQuit")) return;
        
        NativeObject data = createObject();
        data.put("uuid", data, uuid.toString());
        fire("playerQuit", data);
    }

    /**
     * Checks for player movement and fires move events.
     */
    private void checkPlayerMove(PlayerRef player) {
        if (!listeners.containsKey("playerMove")) return;
        
        UUID uuid = player.getUuid();
        
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            
            if (store == null) return;
            
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return;
            
            Vector3d currentPos = transform.getPosition();
            Vector3d lastPos = playerPositions.get(uuid);
                
                if (lastPos != null) {
                    double distance = Math.sqrt(
                        Math.pow(currentPos.getX() - lastPos.getX(), 2) +
                        Math.pow(currentPos.getY() - lastPos.getY(), 2) +
                        Math.pow(currentPos.getZ() - lastPos.getZ(), 2)
                    );
                    
                    if (distance >= MOVE_THRESHOLD) {
                        NativeObject data = createObject();
                        data.put("player", data, player);
                        data.put("uuid", data, uuid.toString());
                        
                        NativeObject from = createObject();
                        from.put("x", from, lastPos.getX());
                        from.put("y", from, lastPos.getY());
                        from.put("z", from, lastPos.getZ());
                        data.put("from", data, from);
                        
                        NativeObject to = createObject();
                        to.put("x", to, currentPos.getX());
                        to.put("y", to, currentPos.getY());
                        to.put("z", to, currentPos.getZ());
                        data.put("to", data, to);
                        
                        data.put("distance", data, distance);
                        
                        fire("playerMove", data);
                    }
                }
                
                playerPositions.put(uuid, currentPos.clone());
        } catch (Exception e) {
            logger.fine("Error checking player move: " + e.getMessage());
        }
    }

    /**
     * Checks for player world changes.
     */
    private void checkPlayerWorldChange(PlayerRef player) {
        if (!listeners.containsKey("playerChangeWorld")) return;
        
        UUID uuid = player.getUuid();
        
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            
            if (store == null) return;
            
            EntityStore entityStore = store.getExternalData();
            World world = entityStore.getWorld();
            String currentWorld = world.getName();
            String lastWorld = playerWorlds.get(uuid);
            
            if (lastWorld != null && !lastWorld.equals(currentWorld)) {
                NativeObject data = createObject();
                data.put("player", data, player);
                data.put("uuid", data, uuid.toString());
                data.put("fromWorld", data, lastWorld);
                data.put("toWorld", data, currentWorld);
                fire("playerChangeWorld", data);
            }
            
            playerWorlds.put(uuid, currentWorld);
        } catch (Exception e) {
            logger.fine("Error checking world change: " + e.getMessage());
        }
    }

    /**
     * Fires a playerDeath event.
     * This should be called when a player death is detected.
     */
    public void firePlayerDeath(PlayerRef player, double x, double y, double z, float pitch, float yaw, String worldName) {
        if (!listeners.containsKey("playerDeath")) return;
        
        NativeObject data = createObject();
        data.put("player", data, player);
        data.put("uuid", data, player.getUuid().toString());
        data.put("username", data, player.getUsername());
        
        NativeObject position = createObject();
        position.put("x", position, x);
        position.put("y", position, y);
        position.put("z", position, z);
        position.put("pitch", position, pitch);
        position.put("yaw", position, yaw);
        position.put("worldName", position, worldName);
        data.put("position", data, position);
        
        fire("playerDeath", data);
    }

    /**
     * Fires a playerRespawn event.
     */
    public void firePlayerRespawn(PlayerRef player) {
        if (!listeners.containsKey("playerRespawn")) return;
        
        NativeObject data = createObject();
        data.put("player", data, player);
        data.put("uuid", data, player.getUuid().toString());
        data.put("username", data, player.getUsername());
        fire("playerRespawn", data);
    }

    /**
     * Fires a playerChat event. Returns true if cancelled.
     */
    public boolean firePlayerChat(PlayerRef player, String message) {
        if (!listeners.containsKey("playerChat")) return false;
        
        NativeObject data = createObject();
        data.put("player", data, player);
        data.put("uuid", data, player.getUuid().toString());
        data.put("username", data, player.getUsername());
        data.put("message", data, message);
        
        return fire("playerChat", data);
    }

    /**
     * Fires a playerCommand event. Returns true if cancelled.
     */
    public boolean firePlayerCommand(PlayerRef player, String command, String[] args) {
        if (!listeners.containsKey("playerCommand")) return false;
        
        NativeObject data = createObject();
        data.put("player", data, player);
        data.put("uuid", data, player.getUuid().toString());
        data.put("username", data, player.getUsername());
        data.put("command", data, command);
        data.put("args", data, args);
        
        return fire("playerCommand", data);
    }

    /**
     * Gets the current tick count.
     */
    public long getTickCount() {
        return tickCount;
    }

    /**
     * Gets a list of all registered event names.
     */
    public Set<String> getRegisteredEvents() {
        return new HashSet<>(listeners.keySet());
    }

    /**
     * Gets the number of handlers for an event.
     */
    public int getHandlerCount(String eventName) {
        List<EventHandler> handlers = listeners.get(eventName);
        return handlers != null ? handlers.size() : 0;
    }

    /**
     * Creates a JavaScript-compatible object.
     */
    private NativeObject createObject() {
        NativeObject obj = new NativeObject();
        obj.setParentScope(scope);
        return obj;
    }

    /**
     * Event handler wrapper.
     */
    private static class EventHandler {
        final String id;
        final Function function;
        final boolean once;

        EventHandler(String id, Function function, boolean once) {
            this.id = id;
            this.function = function;
            this.once = once;
        }
    }

    /**
     * Starts the event polling system using the Scheduler.
     * Should be called after script loading is complete.
     */
    public void startEventPolling(Scheduler scheduler) {
        // Poll every tick (50ms) for player events
        scheduler.runRepeatingMs(() -> {
            try {
                tick();
            } catch (Exception e) {
                logger.fine("Error in event polling: " + e.getMessage());
            }
        }, 50);
        logger.info("Event polling system started");
    }
}
