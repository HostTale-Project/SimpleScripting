package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Function;

final class JsRefSystem extends RefSystem<EntityStore> {
    private final String name;
    private final Query<EntityStore> query;
    private final Function onAdd;
    private final Function onRemove;
    private final JsModRuntime runtime;

    JsRefSystem(String name,
                Query<EntityStore> query,
                Function onAdd,
                Function onRemove,
                JsModRuntime runtime) {
        this.name = name;
        this.query = query;
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.runtime = runtime;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onEntityAdded(Ref<EntityStore> ref, com.hypixel.hytale.component.AddReason addReason, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (onAdd != null) {
            runtime.callFunction(onAdd, ref, addReason, store, commandBuffer);
        }
    }

    @Override
    public void onEntityRemove(Ref<EntityStore> ref, com.hypixel.hytale.component.RemoveReason removeReason, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (onRemove != null) {
            runtime.callFunction(onRemove, ref, removeReason, store, commandBuffer);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
