package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Function;

/**
 * Bridges ECS world events into JS callbacks.
 */
final class JsWorldEventSystem extends WorldEventSystem<EntityStore, EcsEvent> {

    private final String name;
    private final Function handler;
    private final JsModRuntime runtime;
    private final HytaleLogger logger;

    @SuppressWarnings("unchecked")
    JsWorldEventSystem(String name,
                       Class<? extends EcsEvent> eventClass,
                       Function handler,
                       JsModRuntime runtime,
                       HytaleLogger logger) {
        super((Class<EcsEvent>) eventClass);
        this.name = name;
        this.handler = handler;
        this.runtime = runtime;
        this.logger = logger.getSubLogger("evt-" + name);
    }

    @Override
    public void handle(Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       EcsEvent event) {
        try {
            runtime.callFunction(handler, event, store, commandBuffer);
        } catch (Exception e) {
            logger.atSevere().log("JS world event handler failed: %s", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
