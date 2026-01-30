package com.hosttale.simplescripting.mod.runtime.api.events;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.ModRegistrationTracker;
import com.hosttale.simplescripting.mod.runtime.api.events.EventPayloads;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Function;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class EventsApi {

    private final String modId;
    private final EventRegistry eventRegistry;
    private final JsModRuntime runtime;
    private final ModRegistrationTracker registrationTracker;
    private final HytaleLogger logger;
    private final EventCatalog catalog;
    private final AtomicInteger idSequence = new AtomicInteger();
    private final Map<String, EventRegistration<?, ?>> registrations = new ConcurrentHashMap<>();

    public EventsApi(String modId,
                     EventRegistry eventRegistry,
                     JsModRuntime runtime,
                     ModRegistrationTracker registrationTracker,
                     HytaleLogger logger,
                     EventCatalog catalog) {
        this.modId = modId;
        this.eventRegistry = eventRegistry;
        this.runtime = runtime;
        this.registrationTracker = registrationTracker;
        this.logger = logger.getSubLogger("events");
        this.catalog = catalog;
    }

    public String on(String eventName, Function handler) {
        return register(eventName, handler, false);
    }

    public String once(String eventName, Function handler) {
        return register(eventName, handler, true);
    }

    public void off(String handle) {
        EventRegistration<?, ?> registration = registrations.remove(handle);
        if (registration != null) {
            registration.unregister();
        }
    }

    public void clear() {
        registrations.values().forEach(EventRegistration::unregister);
        registrations.clear();
    }

    public String[] knownEvents() {
        return catalog.knownEventNames().toArray(String[]::new);
    }

    private String register(String eventName, Function handler, boolean once) {
        if (handler == null) {
            throw new IllegalArgumentException("events.on requires a callback function.");
        }
        Class<?> eventClass = catalog.resolve(eventName);
        String handle = modId + "-evt-" + idSequence.incrementAndGet();

        Consumer<Object> consumer = event -> {
            try {
                Object payload = EventPayloads.adapt(event);
                runtime.callFunction(handler, payload);
            } catch (Exception e) {
                logger.atSevere().log("Mod %s handler failed for %s: %s", modId, eventClass.getSimpleName(), e.getMessage());
            } finally {
                if (once) {
                    off(handle);
                }
            }
        };

        EventRegistration<?, ?> registration = register(eventClass, consumer);
        registrationTracker.trackRegistration(registration);
        registrations.put(handle, registration);
        return handle;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private EventRegistration<?, ?> register(Class<?> eventClass, Consumer<Object> consumer) {
        if (com.hypixel.hytale.event.IAsyncEvent.class.isAssignableFrom(eventClass)) {
            java.util.function.Function<CompletableFuture, CompletableFuture> fn = future -> future.thenApply(event -> {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    logger.atSevere().log("Async event handler failed: %s", e.getMessage());
                }
                return event;
            });
            EventRegistration<?, ?> registration = eventRegistry.registerAsyncGlobal((Class) eventClass, (java.util.function.Function) fn);
            return registration;
        }

        EventRegistration<?, ?> registration = eventRegistry.registerGlobal((Class) eventClass, (Consumer) consumer);
        return registration;
    }
}
