package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Function;

final class JsRefChangeSystem extends RefChangeSystem<EntityStore, Component<EntityStore>> {
    private final String name;
    private final ComponentType<EntityStore, ?> componentType;
    private final Function onAdded;
    private final Function onSet;
    private final Function onRemoved;
    private final JsModRuntime runtime;

    JsRefChangeSystem(String name,
                      ComponentType<EntityStore, ?> componentType,
                      Function onAdded,
                      Function onSet,
                      Function onRemoved,
                      JsModRuntime runtime) {
        this.name = name;
        this.componentType = componentType;
        this.onAdded = onAdded;
        this.onSet = onSet;
        this.onRemoved = onRemoved;
        this.runtime = runtime;
    }

    @Override
    public ComponentType<EntityStore, Component<EntityStore>> componentType() {
        @SuppressWarnings("unchecked")
        ComponentType<EntityStore, Component<EntityStore>> cast = (ComponentType<EntityStore, Component<EntityStore>>) componentType;
        return cast;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(componentType);
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, Component<EntityStore> comp, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (onAdded != null) runtime.callFunction(onAdded, ref, comp, store, commandBuffer);
    }

    @Override
    public void onComponentSet(Ref<EntityStore> ref, Component<EntityStore> oldComp, Component<EntityStore> newComp, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (onSet != null) runtime.callFunction(onSet, ref, oldComp, newComp, store, commandBuffer);
    }

    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, Component<EntityStore> comp, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (onRemoved != null) runtime.callFunction(onRemoved, ref, comp, store, commandBuffer);
    }

    @Override
    public String toString() {
        return name;
    }
}
