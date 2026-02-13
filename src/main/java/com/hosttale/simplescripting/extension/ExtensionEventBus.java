package com.hosttale.simplescripting.extension;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event bus for extension-to-extension and extension-to-JS communication.
 * Separate from Hytale's core EventRegistry to avoid conflicts and provide
 * a simpler, more flexible event system for extensions.
 */
public final class ExtensionEventBus {
    
    private final HytaleLogger logger;
    private final Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    public ExtensionEventBus(HytaleLogger logger) {
        this.logger = logger.getSubLogger("event-bus");
    }
    
    /**
     * Register an event listener.
     * 
     * @param eventName Name of the event to listen for
     * @param listener Listener to handle the event
     * @return Handle to unregister the listener
     */
    public EventHandle on(String eventName, EventListener listener) {
        if (!active.get()) {
            throw new IllegalStateException("Event bus is shut down");
        }
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be null or blank");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        listeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.atFine().log("Registered listener for event: %s", eventName);
        
        return () -> {
            List<EventListener> list = listeners.get(eventName);
            if (list != null) {
                list.remove(listener);
            }
        };
    }
    
    /**
     * Emit an event to all registered listeners.
     * 
     * @param eventName Name of the event
     * @param payload Event payload (can be null)
     */
    public void emit(String eventName, Object payload) {
        if (!active.get()) {
            logger.atWarning().log("Event bus is shut down, ignoring event: %s", eventName);
            return;
        }
        
        List<EventListener> list = listeners.get(eventName);
        if (list == null || list.isEmpty()) {
            return;
        }
        
        ExtensionEvent event = new ExtensionEvent(eventName, payload);
        for (EventListener listener : list) {
            try {
                listener.onEvent(event);
                if (event.isCancelled()) {
                    break;
                }
            } catch (Exception e) {
                logger.atSevere().log("Event listener failed for %s: %s", eventName, e.getMessage());
            }
        }
    }
    
    /**
     * Check if an event has any registered listeners.
     * 
     * @param eventName Event name to check
     * @return true if at least one listener is registered
     */
    public boolean hasListeners(String eventName) {
        List<EventListener> list = listeners.get(eventName);
        return list != null && !list.isEmpty();
    }
    
    /**
     * Get count of listeners for an event.
     * 
     * @param eventName Event name
     * @return Number of registered listeners
     */
    public int getListenerCount(String eventName) {
        List<EventListener> list = listeners.get(eventName);
        return list != null ? list.size() : 0;
    }
    
    /**
     * Shut down the event bus. Called during plugin shutdown.
     * After shutdown, no new listeners can be registered and no events will be emitted.
     */
    void shutdown() {
        active.set(false);
        listeners.clear();
        logger.atInfo().log("Event bus shut down");
    }
    
    /**
     * Check if event bus is active.
     */
    public boolean isActive() {
        return active.get();
    }
    
    /**
     * Event listener functional interface.
     */
    @FunctionalInterface
    public interface EventListener {
        void onEvent(ExtensionEvent event);
    }
    
    /**
     * Handle for unregistering an event listener.
     */
    @FunctionalInterface
    public interface EventHandle {
        void unregister();
    }
    
    /**
     * Event object passed to listeners.
     */
    public static final class ExtensionEvent {
        private final String name;
        private final Object payload;
        private boolean cancelled;
        
        ExtensionEvent(String name, Object payload) {
            this.name = name;
            this.payload = payload;
        }
        
        /**
         * Get the event name.
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get the event payload.
         */
        public Object getPayload() {
            return payload;
        }
        
        /**
         * Check if the event has been cancelled.
         */
        public boolean isCancelled() {
            return cancelled;
        }
        
        /**
         * Cancel the event. Remaining listeners will not be called.
         */
        public void cancel() {
            this.cancelled = true;
        }
    }
}
