package com.hosttale.simplescripting.mod.runtime.api.events;

import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EventCatalog {

    private static final Map<String, String> EVENT_CLASS_NAMES;

    static {
        Map<String, String> events = new LinkedHashMap<>();
        events.put("bootevent", "com.hypixel.hytale.server.core.event.events.BootEvent");
        events.put("shutdownevent", "com.hypixel.hytale.server.core.event.events.ShutdownEvent");
        events.put("playerconnectevent", "com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent");
        events.put("playerdisconnectevent", "com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent");
        events.put("playerreadyevent", "com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent");
        events.put("playerchatevent", "com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent");
        events.put("playerinteractevent", "com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent");
        events.put("breakblockevent", "com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent");
        events.put("placeblockevent", "com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent");
        events.put("useblockevent", "com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent");
        events.put("allworldsloadedevent", "com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent");
        events.put("startworldevent", "com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent");
        events.put("addworldevent", "com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent");
        events.put("removeworldevent", "com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent");
        EVENT_CLASS_NAMES = Collections.unmodifiableMap(events);
    }

    private final HytaleLogger logger;

    public EventCatalog(HytaleLogger logger) {
        this.logger = logger.getSubLogger("event-catalog");
    }

    public Class<?> resolve(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required.");
        }
        String normalized = normalize(name);
        String className = EVENT_CLASS_NAMES.get(normalized);
        if (className == null && name.contains(".")) {
            className = name;
        }
        if (className == null) {
            throw new IllegalArgumentException("Unknown event '" + name + "'. Known events: " + String.join(", ", knownEventNames()));
        }
        return load(className, name);
    }

    public List<String> knownEventNames() {
        return EVENT_CLASS_NAMES.keySet().stream()
                .map(key -> key.endsWith("event") ? key.substring(0, key.length() - 5) : key)
                .sorted()
                .toList();
    }

    private Class<?> load(String className, String providedName) {
        try {
            Class<?> loaded = Class.forName(className);
            return loaded;
        } catch (ClassNotFoundException e) {
            logger.atWarning().log("Tried to hook event %s but class %s was not found.", providedName, className);
            throw new IllegalArgumentException("Event class not found for '" + providedName + "'.", e);
        }
    }

    private String normalize(String name) {
        String trimmed = name.endsWith("Event") ? name.substring(0, name.length() - "Event".length()) : name;
        return trimmed.replaceAll("[\\s_-]", "").toLowerCase(Locale.ROOT) + "event";
    }
}
