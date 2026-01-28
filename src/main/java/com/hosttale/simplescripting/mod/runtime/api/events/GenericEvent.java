package com.hosttale.simplescripting.mod.runtime.api.events;

/**
 * Safe wrapper for events that do not have a dedicated adapter yet.
 */
public final class GenericEvent {
    private final String type;
    private final String description;

    public GenericEvent(Object nativeEvent) {
        this.type = nativeEvent == null ? "unknown" : nativeEvent.getClass().getSimpleName();
        this.description = nativeEvent == null ? "" : nativeEvent.toString();
    }

    public String getType() {
        return type;
    }

    public String describe() {
        return description;
    }
}
