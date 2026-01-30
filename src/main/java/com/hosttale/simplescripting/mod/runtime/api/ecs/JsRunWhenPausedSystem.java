package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.system.tick.RunWhenPausedSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Function;

/** JS-backed RunWhenPausedSystem. */
final class JsRunWhenPausedSystem implements RunWhenPausedSystem<EntityStore> {

    private final String name;
    private final Function tickFn;
    private final JsModRuntime runtime;
    private final HytaleLogger logger;
    private final SystemGroup<EntityStore> group;

    JsRunWhenPausedSystem(String name,
                          Function tickFn,
                          JsModRuntime runtime,
                          HytaleLogger logger,
                          SystemGroup<EntityStore> group) {
        this.name = name;
        this.tickFn = tickFn;
        this.runtime = runtime;
        this.logger = logger.getSubLogger(name);
        this.group = group;
    }

    @Override
    public void tick(float dt, int storeIndex, Store<EntityStore> store) {
        try {
            runtime.callFunction(tickFn, dt, storeIndex, store);
        } catch (Exception e) {
            logger.atSevere().log("JS run-when-paused system failed: %s", e.getMessage());
        }
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return name;
    }
}
