package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.ScriptableObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple dynamic component storing a key/value map copied from a JS object.
 */
public final class JsDynamicComponent implements Component<EntityStore> {

    private final Map<String, Object> data = new HashMap<>();

    public JsDynamicComponent() {
    }

    public JsDynamicComponent(ScriptableObject source) {
        Object[] ids = source.getIds();
        for (Object id : ids) {
            if (id instanceof String key) {
                data.put(key, ScriptableObject.getProperty(source, key));
            }
        }
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @Override
    public Component<EntityStore> clone() {
        JsDynamicComponent copy = new JsDynamicComponent();
        copy.data.putAll(this.data);
        return copy;
    }
}
