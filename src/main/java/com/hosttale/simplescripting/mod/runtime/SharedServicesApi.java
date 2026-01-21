package com.hosttale.simplescripting.mod.runtime;

import com.hosttale.simplescripting.mod.SharedServiceRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import java.util.Optional;

public final class SharedServicesApi {

    private final String ownerId;
    private final SharedServiceRegistry registry;
    private final JsModRuntime runtime;
    private final HytaleLogger logger;

    public SharedServicesApi(String ownerId, SharedServiceRegistry registry, JsModRuntime runtime, HytaleLogger logger) {
        this.ownerId = ownerId;
        this.registry = registry;
        this.runtime = runtime;
        this.logger = logger.getSubLogger("shared-services");
    }

    /**
     * Expose an API object to other mods. It must be a plain JS object with functions you are willing to share.
     */
    public boolean expose(String name, Object apiObject) {
        if (!(apiObject instanceof Scriptable scriptable)) {
            logger.atWarning().log("Mod %s tried to expose '%s' but object is not scriptable.", ownerId, name);
            return false;
        }
        boolean success = registry.expose(name, ownerId, runtime, scriptable);
        if (!success) {
            logger.atWarning().log("Service name '%s' is already claimed by another mod.", name);
        }
        return success;
    }

    /**
     * Call a method on another mod's exposed service.
     */
    public Object call(String serviceName, String methodName, Object args) {
        Optional<SharedServiceRegistry.ServiceEntry> entryOpt = registry.get(serviceName);
        if (entryOpt.isEmpty()) {
            logger.atWarning().log("Service '%s' not found.", serviceName);
            return null;
        }
        SharedServiceRegistry.ServiceEntry entry = entryOpt.get();
        try {
            Object[] callArgs = toArgs(args);
            return entry.runtime().invokeFunction(entry.apiObject(), methodName, callArgs);
        } catch (Exception e) {
            logger.atSevere().log("Failed calling %s.%s from mod %s: %s", serviceName, methodName, ownerId, e.getMessage());
            return null;
        }
    }

    private Object[] toArgs(Object args) {
        if (args instanceof NativeArray array) {
            Object[] out = new Object[(int) array.getLength()];
            for (int i = 0; i < array.getLength(); i++) {
                out[i] = array.get(i, array);
            }
            return out;
        }
        if (args == null) {
            return Context.emptyArgs;
        }
        return new Object[]{args};
    }
}
