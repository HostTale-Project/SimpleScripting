package com.hosttale.simplescripting.extension;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.ModRegistrationTracker;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Function;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaScript API for accessing the extension event bus.
 * Allows JS mods to listen to and emit extension events.
 */
public final class ExtensionEventsApi {
    
    private final ExtensionEventBus eventBus;
    private final JsModRuntime runtime;
    private final ModRegistrationTracker registrationTracker;
    private final HytaleLogger logger;
    private final Map<String, ExtensionEventBus.EventHandle> handles = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger();
    
    public ExtensionEventsApi(ExtensionEventBus eventBus,
                             JsModRuntime runtime,
                             ModRegistrationTracker registrationTracker,
                             HytaleLogger logger) {
        this.eventBus = eventBus;
        this.runtime = runtime;
        this.registrationTracker = registrationTracker;
        this.logger = logger.getSubLogger("ext-events");
    }
    
    /**
     * Listen to extension events from JavaScript.
     * 
     * @param eventName Event name to listen for
     * @param handler JavaScript function to call when event fires
     * @return Handle ID for unregistering
     */
    public String on(String eventName, Function handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler function is required");
        }
        
        String handleId = "ext-evt-" + idSequence.incrementAndGet();
        
        ExtensionEventBus.EventListener listener = event -> {
            try {
                runtime.callFunction(handler, event.getPayload());
            } catch (Exception e) {
                logger.atSevere().log("JS handler failed for extension event %s: %s",
                        eventName, e.getMessage());
            }
        };
        
        ExtensionEventBus.EventHandle handle = eventBus.on(eventName, listener);
        handles.put(handleId, handle);
        
        // Track for cleanup on mod disable/reload
        registrationTracker.trackRegistration(() -> {
            ExtensionEventBus.EventHandle h = handles.remove(handleId);
            if (h != null) {
                h.unregister();
            }
        });
        
        return handleId;
    }
    
    /**
     * Emit an extension event from JavaScript.
     * 
     * @param eventName Event name
     * @param payload Event payload (optional)
     */
    public void emit(String eventName, Object payload) {
        eventBus.emit(eventName, payload);
    }
    
    /**
     * Emit an extension event without payload.
     */
    public void emit(String eventName) {
        eventBus.emit(eventName, null);
    }
    
    /**
     * Unregister a specific event listener.
     * 
     * @param handleId Handle ID returned by on()
     */
    public void off(String handleId) {
        ExtensionEventBus.EventHandle handle = handles.remove(handleId);
        if (handle != null) {
            handle.unregister();
        }
    }
    
    /**
     * Clear all event listeners registered by this mod.
     */
    public void clear() {
        handles.values().forEach(ExtensionEventBus.EventHandle::unregister);
        handles.clear();
    }
    
    /**
     * Check if an event has listeners.
     * 
     * @param eventName Event name to check
     * @return true if listeners are registered
     */
    public boolean hasListeners(String eventName) {
        return eventBus.hasListeners(eventName);
    }
}
