package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.QuerySystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Function;

final class JsEntityTickingSystem extends EntityTickingSystem<EntityStore> implements QuerySystem<EntityStore> {
    private final Query<EntityStore> query;
    private final Function tickFn;
    private final boolean parallel;
    private final String name;
    private final SystemGroup<EntityStore> group;
    private final JsModRuntime runtime;

    JsEntityTickingSystem(String name,
                          Query<EntityStore> query,
                          Function tickFn,
                          boolean parallel,
                          SystemGroup<EntityStore> group,
                          JsModRuntime runtime) {
        this.name = name;
        this.query = query;
        this.tickFn = tickFn;
        this.parallel = parallel;
        this.group = group;
        this.runtime = runtime;
    }

    @Override
    public boolean isParallel(int chunkIdx, int total) {
        return parallel;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return group;
    }

    @Override
    public void tick(float dt, int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        runtime.callFunction(tickFn, dt, entityIndex, chunk, store, commandBuffer);
    }

    @Override
    public String toString() {
        return name;
    }
}
