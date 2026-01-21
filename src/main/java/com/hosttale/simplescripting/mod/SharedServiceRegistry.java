package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import org.mozilla.javascript.Scriptable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SharedServiceRegistry {

    private final Map<String, ServiceEntry> services = new ConcurrentHashMap<>();

    public boolean expose(String name, String ownerId, JsModRuntime runtime, Scriptable apiObject) {
        if (name == null || name.isBlank() || apiObject == null) {
            return false;
        }
        ServiceEntry existing = services.get(name);
        if (existing != null && !existing.ownerId.equals(ownerId)) {
            // Prevent accidental takeover by another mod.
            return false;
        }
        services.put(name, new ServiceEntry(name, ownerId, runtime, apiObject));
        return true;
    }

    public Optional<ServiceEntry> get(String name) {
        return Optional.ofNullable(services.get(name));
    }

    public void removeOwner(String ownerId) {
        services.entrySet().removeIf(e -> e.getValue().ownerId.equals(ownerId));
    }

    public record ServiceEntry(String name, String ownerId, JsModRuntime runtime, Scriptable apiObject) {
    }
}
