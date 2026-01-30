package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Function;

/**
 * Bridges ECS entity events into JS callbacks.
 */
final class JsEntityEventSystem extends EntityEventSystem<EntityStore, EcsEvent>
        implements com.hypixel.hytale.component.system.QuerySystem<EntityStore> {

    private final String name;
    private final Query<EntityStore> query;
    private final Function handler;
    private final JsModRuntime runtime;
    private final HytaleLogger logger;

    @SuppressWarnings("unchecked")
    JsEntityEventSystem(String name,
                        Class<? extends EcsEvent> eventClass,
                        Query<EntityStore> query,
                        Function handler,
                        JsModRuntime runtime,
                        HytaleLogger logger) {
        super((Class<EcsEvent>) eventClass);
        this.name = name;
        this.query = query;
        this.handler = handler;
        this.runtime = runtime;
        this.logger = logger.getSubLogger("evt-" + name);
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       EcsEvent event) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            runtime.callFunction(handler, event, ref, store, commandBuffer);
        } catch (Exception e) {
            logger.atSevere().log("JS entity event handler failed: %s", e.getMessage());
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return name;
    }
}
