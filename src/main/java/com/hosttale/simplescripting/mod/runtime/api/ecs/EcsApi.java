package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.ModRegistrationTracker;
import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayerHandle;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.component.spatial.KDTree;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.PositionDataComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.NativeJavaObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ECS bridge for JS mods.
 */
public final class EcsApi {

    private final HytaleLogger logger;
    private final ModRegistrationTracker registrationTracker;
    private final JsModRuntime runtime;
    private final EcsComponentResolver componentResolver;
    private final Map<String, Class<?>> builtinEvents = new LinkedHashMap<>();
    private final Map<Class<?>, Integer> systemRefCounts = new ConcurrentHashMap<>();

    public EcsApi(HytaleLogger logger, ModRegistrationTracker registrationTracker, JsModRuntime runtime) {
        this.logger = logger.getSubLogger("ecs");
        this.registrationTracker = registrationTracker;
        this.runtime = runtime;
        this.componentResolver = new EcsComponentResolver(this.logger);
        loadBuiltinEvents();
    }

    private void loadBuiltinEvents() {
        Class<?>[] classes = new Class<?>[]{
                com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent.class,
                com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent.class,
                com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Pre.class,
                com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Post.class,
                com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent.class,
                com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent.class,
                com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent.PlayerRequest.class,
                com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent.Drop.class,
                com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent.class,
                com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent.Pre.class,
                com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent.Post.class,
                com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent.class,
                com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent.class,
                com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkSaveEvent.class,
                com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent.class,
                com.hypixel.hytale.server.core.universe.world.events.ecs.MoonPhaseChangeEvent.class
        };
        for (Class<?> clazz : classes) {
            builtinEvents.put(clazz.getSimpleName(), clazz);
        }
    }

    // --------- Reference helpers ---------

    @Nullable
    public Ref<EntityStore> toRef(Object target) {
        try {
            return resolveRef(target);
        } catch (Exception e) {
            logger.atWarning().log(e.getMessage());
            return null;
        }
    }

    // --------- Position / rotation / motion ---------

    @Nullable
    public Vector3d getPosition(Object target) {
        Ref<EntityStore> ref = resolveRef(target);
        TransformComponent transform = getComponent(ref, TransformComponent.getComponentType(), true);
        return transform == null ? null : transform.getPosition();
    }

    public void setPosition(Object target, Object position) {
        setPosition(target, position, null);
    }

    public void setPosition(Object target, Object position, Object commandBufferObj) {
        Ref<EntityStore> ref = resolveRef(target);
        Vector3d vec = VectorConverters.toVector3d(position, "position");
        CommandBuffer<EntityStore> cmd = toCommandBuffer(commandBufferObj);
        if (cmd != null) {
            TransformComponent transform = cmd.ensureAndGetComponent(ref, TransformComponent.getComponentType());
            transform.setPosition(vec);
            return;
        }
        TransformComponent transform = getOrCreate(ref, TransformComponent.getComponentType(), TransformComponent::new);
        transform.setPosition(vec);
        transform.markChunkDirty(ref.getStore());
    }

    public void teleport(Object target, Object position, Object rotation) {
        teleport(target, position, rotation, null);
    }

    public void teleport(Object target, Object position, Object rotation, Object commandBufferObj) {
        Ref<EntityStore> ref = resolveRef(target);
        CommandBuffer<EntityStore> cmd = toCommandBuffer(commandBufferObj);
        Vector3d pos = VectorConverters.toVector3d(position, "position");
        Vector3f rot = VectorConverters.toVector3f(rotation, "rotation");
        if (cmd != null) {
            TransformComponent transform = cmd.ensureAndGetComponent(ref, TransformComponent.getComponentType());
            transform.teleportPosition(pos);
            transform.teleportRotation(rot);
            return;
        }
        TransformComponent transform = getOrCreate(ref, TransformComponent.getComponentType(), TransformComponent::new);
        transform.teleportPosition(pos);
        transform.teleportRotation(rot);
    }

    @Nullable
    public Vector3f getRotation(Object target) {
        Ref<EntityStore> ref = resolveRef(target);
        TransformComponent transform = getComponent(ref, TransformComponent.getComponentType(), true);
        return transform == null ? null : transform.getRotation();
    }

    public void setRotation(Object target, Object rotation) {
        setRotation(target, rotation, null);
    }

    public void setRotation(Object target, Object rotation, Object commandBufferObj) {
        Ref<EntityStore> ref = resolveRef(target);
        Vector3f rot = VectorConverters.toVector3f(rotation, "rotation");
        CommandBuffer<EntityStore> cmd = toCommandBuffer(commandBufferObj);
        if (cmd != null) {
            TransformComponent transform = cmd.ensureAndGetComponent(ref, TransformComponent.getComponentType());
            transform.setRotation(rot);
            return;
        }
        TransformComponent transform = getOrCreate(ref, TransformComponent.getComponentType(), TransformComponent::new);
        transform.setRotation(rot);
        transform.markChunkDirty(ref.getStore());
    }

    @Nullable
    public Vector3f getHeadRotation(Object target) {
        Ref<EntityStore> ref = resolveRef(target);
        HeadRotation head = getComponent(ref, HeadRotation.getComponentType(), true);
        return head == null ? null : head.getRotation();
    }

    public void setHeadRotation(Object target, Object rotation) {
        setHeadRotation(target, rotation, null);
    }

    public void setHeadRotation(Object target, Object rotation, Object commandBufferObj) {
        Ref<EntityStore> ref = resolveRef(target);
        Vector3f rot = VectorConverters.toVector3f(rotation, "rotation");
        CommandBuffer<EntityStore> cmd = toCommandBuffer(commandBufferObj);
        if (cmd != null) {
            HeadRotation head = cmd.ensureAndGetComponent(ref, HeadRotation.getComponentType());
            head.setRotation(rot);
            return;
        }
        HeadRotation head = getOrCreate(ref, HeadRotation.getComponentType(), HeadRotation::new);
        head.setRotation(rot);
    }

    @Nullable
    public Vector3d getVelocity(Object target) {
        Ref<EntityStore> ref = resolveRef(target);
        Velocity velocity = getComponent(ref, Velocity.getComponentType(), true);
        return velocity == null ? null : velocity.getVelocity();
    }

    public void setVelocity(Object target, Object velocityValue) {
        setVelocity(target, velocityValue, null);
    }

    public void setVelocity(Object target, Object velocityValue, Object commandBufferObj) {
        Ref<EntityStore> ref = resolveRef(target);
        Vector3d vec = VectorConverters.toVector3d(velocityValue, "velocity");
        CommandBuffer<EntityStore> cmd = toCommandBuffer(commandBufferObj);
        if (cmd != null) {
            Velocity velocity = cmd.ensureAndGetComponent(ref, Velocity.getComponentType());
            velocity.set(vec);
            return;
        }
        Velocity velocity = getOrCreate(ref, Velocity.getComponentType(), Velocity::new);
        velocity.set(vec);
    }

    public void addForce(Object target, Object force) {
        addForce(target, force, null);
    }

    public void addForce(Object target, Object force, Object commandBufferObj) {
        Ref<EntityStore> ref = resolveRef(target);
        Vector3d vec = VectorConverters.toVector3d(force, "force");

        CommandBuffer<EntityStore> cmd = toCommandBuffer(commandBufferObj);

        if (cmd != null) {
            Velocity velocity = cmd.ensureAndGetComponent(ref, Velocity.getComponentType());
            velocity.addForce(vec);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            throw new IllegalStateException("Missing store for entity.");
        }
        store.assertThread();
        Velocity velocity = store.ensureAndGetComponent(ref, Velocity.getComponentType());
        velocity.addForce(vec);
    }

    /**
     * Acquire a CommandBuffer for the store backing the target (Ref or Store), invoke the given JS function with it,
     * and always release the buffer. Use this from commands/events to mutate safely off the store thread.
     */
    public void withCommandBuffer(Object target, Function fn) {
        Store<EntityStore> store = resolveStoreForBuffer(target);
        CommandBuffer<EntityStore> cmd = borrowCommandBuffer(store);
        try {
            runtime.callFunction(fn, cmd);
        } finally {
            returnCommandBuffer(store, cmd);
        }
    }

    @SuppressWarnings("unchecked")
    private CommandBuffer<EntityStore> toCommandBuffer(Object commandBufferObj) {
        if (commandBufferObj instanceof NativeJavaObject njo) {
            commandBufferObj = njo.unwrap();
        }
        if (commandBufferObj instanceof CommandBuffer<?> cb) {
            return (CommandBuffer<EntityStore>) cb;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private CommandBuffer<EntityStore> borrowCommandBuffer(Store<EntityStore> store) {
        // takeCommandBuffer is package-private; call via current store thread using the public accessor if available.
        // Some Store implementations expose getCommandBuffer() on InteractionContext; fall back to reflection in-store thread.
        if (store == null) {
            throw new IllegalArgumentException("Store is required to borrow command buffer.");
        }
        try {
            var m = Store.class.getDeclaredMethod("takeCommandBuffer");
            m.setAccessible(true);
            return (CommandBuffer<EntityStore>) m.invoke(store);
        } catch (NoSuchMethodException e) {
            // Fallback: if store implements ComponentAccessor with a command buffer accessor
            throw new IllegalStateException("Store does not support command buffers.");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to acquire command buffer: " + e.getMessage(), e);
        }
    }

    private void returnCommandBuffer(Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        try {
            var m = Store.class.getDeclaredMethod("storeCommandBuffer", CommandBuffer.class);
            m.setAccessible(true);
            m.invoke(store, cmd);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Store does not support command buffers.");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to return command buffer: " + e.getMessage(), e);
        }
    }

    @Nullable
    public BoundingBox getBoundingBox(Object target) {
        Ref<EntityStore> ref = resolveRef(target);
        return getComponent(ref, BoundingBox.getComponentType(), true);
    }

    @Nullable
    public PositionDataComponent getPositionData(Object target) {
        Ref<EntityStore> ref = resolveRef(target);
        return getComponent(ref, PositionDataComponent.getComponentType(), true);
    }

    // --------- Events ---------

    public void invokeEntityEvent(Object target, Object event) {
        Ref<EntityStore> ref = resolveRef(target);
        Store<EntityStore> store = ref.getStore();
        if (store == null) throw new IllegalStateException("Missing store for entity.");
        EcsEvent evt = castEvent(event, "entity event");
        store.invoke(ref, evt);
    }

    public void invokeWorldEvent(Object event) {
        throw new IllegalArgumentException("invokeWorldEvent(event) requires a Store/Ref target; use invokeWorldEvent(storeLike, event).");
    }

    public void invokeWorldEvent(Object storeLike, Object event) {
        Store<EntityStore> store = resolveStore(storeLike);
        EcsEvent evt = castEvent(event, "world event");
        store.invoke(evt);
    }

    // --------- Access to built-ins ---------

    public Map<String, ComponentType<EntityStore, ?>> components() {
        return componentResolver.components();
    }

    public Map<String, Class<?>> events() {
        return builtinEvents;
    }

    // --------- Groups / Damage helpers ---------

    public SystemGroup<EntityStore> damageGatherGroup() {
        DamageModule module = DamageModule.get();
        return module == null ? null : module.getGatherDamageGroup();
    }

    public SystemGroup<EntityStore> damageFilterGroup() {
        DamageModule module = DamageModule.get();
        return module == null ? null : module.getFilterDamageGroup();
    }

    public SystemGroup<EntityStore> damageInspectGroup() {
        DamageModule module = DamageModule.get();
        return module == null ? null : module.getInspectDamageGroup();
    }

    public Map<String, DamageCause> damageCauses() {
        Map<String, DamageCause> map = new LinkedHashMap<>();
        try {
            var fields = DamageCause.class.getFields();
            for (var f : fields) {
                if (DamageCause.class.isAssignableFrom(f.getType())) {
                    DamageCause cause = (DamageCause) f.get(null);
                    if (cause != null) {
                        map.put(f.getName(), cause);
                        if (cause.getId() != null) {
                            map.put(cause.getId(), cause);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    public void applyDamage(Object target, Object optionsObj) {
        Ref<EntityStore> ref = resolveRef(target);
        Store<EntityStore> store = ref.getStore();
        if (store == null) throw new IllegalStateException("Missing store for entity.");

        float amount;
        DamageCause cause = DamageCause.OUT_OF_WORLD;
        if (optionsObj instanceof Number n) {
            amount = n.floatValue();
        } else {
            Scriptable opts = ensureScriptable(optionsObj, "damage options");
            Object amtRaw = ScriptableObject.getProperty(opts, "amount");
            if (!(amtRaw instanceof Number n)) {
                throw new IllegalArgumentException("damage.amount number is required.");
            }
            amount = n.floatValue();
            Object causeRaw = ScriptableObject.getProperty(opts, "cause");
            if (causeRaw instanceof DamageCause dc) {
                cause = dc;
            } else if (causeRaw != null && causeRaw != Scriptable.NOT_FOUND) {
                cause = resolveDamageCause(Objects.toString(causeRaw, null));
            }
        }

        Damage damage = new Damage(Damage.NULL_SOURCE, cause, amount);
        DamageSystems.executeDamage(ref, store, damage);
    }

    public ComponentType<EntityStore, ?> component(String id) {
        ComponentType<EntityStore, ?> type = resolveComponentId(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown component type id '" + id + "'");
        }
        return type;
    }

    public Class<?> event(String id) {
        Class<?> clazz = builtinEvents.get(id);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown event id '" + id + "'");
        }
        return clazz;
    }

    private ComponentType<EntityStore, ?> resolveComponentId(String id) {
        return componentResolver.resolveComponentId(id);
    }

    // --------- Spawning ---------

    public Ref<EntityStore> spawn(Object worldLike, Object componentsArray, Object addReasonValue) {
        Store<EntityStore> store = resolveStore(worldLike);
        AddReason reason = parseAddReason(addReasonValue);
        Component<EntityStore>[] components = toComponentArray(componentsArray);
        Archetype<EntityStore> archetype = archetypeFromComponents(components);
        Holder<EntityStore> holder = registry().newHolder();
        holder.init(archetype, components);
        return store.addEntity(holder, reason);
    }

    public Component<?> createComponent(Object componentTypeObj) {
        if (componentTypeObj instanceof ComponentType<?, ?> type) {
            @SuppressWarnings("unchecked")
            ComponentType<EntityStore, Component<EntityStore>> cast = (ComponentType<EntityStore, Component<EntityStore>>) type;
            return registry().createComponent(cast);
        }
        throw new IllegalArgumentException("Expected ComponentType.");
    }

    // --------- Query helpers ---------

    public Archetype<EntityStore> archetype(Object componentTypesArray) {
        ComponentType<EntityStore, ?>[] types = toComponentTypes(componentTypesArray);
        return Archetype.of(types);
    }

    // --------- System registration (EntityTicking) ---------

    public Object registerEntityTickingSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        Function tickFn = getFunction(options, "tick", true);
        Query<EntityStore> query = buildQuery(options);
        boolean parallel = getBoolean(options, "parallel", false);
        String name = getString(options, "name", "js-entity-system");
        SystemGroup<EntityStore> group = extractGroup(options);

        JsEntityTickingSystem system = new JsEntityTickingSystem(name, query, tickFn, parallel, group, runtime);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    // --------- System registration (RefSystem) ---------

    public Object registerRefSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        Function onAdd = getFunction(options, "onAdd", false);
        Function onRemove = getFunction(options, "onRemove", false);
        Query<EntityStore> query = buildQuery(options);
        String name = getString(options, "name", "js-ref-system");

        JsRefSystem system = new JsRefSystem(name, query, onAdd, onRemove, runtime);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    // --------- System registration (RefChangeSystem) ---------

    public Object registerRefChangeSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        ComponentType<EntityStore, ?> componentType = extractComponentType(options);
        Function onAdded = getFunction(options, "onComponentAdded", false);
        Function onSet = getFunction(options, "onComponentSet", false);
        Function onRemoved = getFunction(options, "onComponentRemoved", false);
        String name = getString(options, "name", "js-refchange-system");

        JsRefChangeSystem system = new JsRefChangeSystem(name, componentType, onAdded, onSet, onRemoved, runtime);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    // --------- System registration (Tickable / RunWhenPaused) ---------

    public Object registerTickableSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        Function tickFn = getFunction(options, "tick", true);
        String name = getString(options, "name", "js-tickable-system");
        SystemGroup<EntityStore> group = extractGroup(options);

        JsTickableSystem system = new JsTickableSystem(name, tickFn, runtime, logger, group);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    public Object registerRunWhenPausedSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        Function tickFn = getFunction(options, "tick", true);
        String name = getString(options, "name", "js-runwhenpaused-system");
        SystemGroup<EntityStore> group = extractGroup(options);

        JsRunWhenPausedSystem system = new JsRunWhenPausedSystem(name, tickFn, runtime, logger, group);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    // --------- System groups ---------

    public SystemGroup<EntityStore> registerSystemGroup() {
        SystemGroup<EntityStore> group = registry().registerSystemGroup();
        registrationTracker.trackRegistration(() -> registry().unregisterSystemGroup(group));
        return group;
    }

    // --------- System registration (ECS events) ---------

    public Object registerEntityEventSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        Class<? extends EcsEvent> eventClass = resolveEventClass(ScriptableObject.getProperty(options, "event"));
        Function handler = getFunction(options, "handle", true);
        Query<EntityStore> query = buildQuery(options);
        String name = getString(options, "name", "js-entity-event-" + eventClass.getSimpleName());

        ensureEntityEventType(eventClass);

        JsEntityEventSystem system = new JsEntityEventSystem(name, eventClass, query, handler, runtime, logger);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    public Object registerWorldEventSystem(Object optionsObj) {
        Scriptable options = ensureScriptable(optionsObj, "options");
        Class<? extends EcsEvent> eventClass = resolveEventClass(ScriptableObject.getProperty(options, "event"));
        Function handler = getFunction(options, "handle", true);
        String name = getString(options, "name", "js-world-event-" + eventClass.getSimpleName());

        ensureWorldEventType(eventClass);

        JsWorldEventSystem system = new JsWorldEventSystem(name, eventClass, handler, runtime, logger);
        registry().registerSystem(system, true);
        incrementSystemRef(system.getClass());
        registrationTracker.trackRegistration(() -> unregisterSystemIfLast(system.getClass()));
        return system;
    }

    // --------- Component/Resource registration ---------

    public ComponentType<EntityStore, ?> registerComponent(String id, Object supplierObj) {
        validateId(id, "component id");
        java.util.function.Supplier<Component<EntityStore>> baseSupplier = toComponentSupplier(supplierObj);
        Component<EntityStore> sample = baseSupplier.get();
        if (sample == null) {
            throw new IllegalArgumentException("Component supplier returned null.");
        }
        @SuppressWarnings("unchecked")
        Class<Component<EntityStore>> clazz = (Class<Component<EntityStore>>) (Class<?>) sample.getClass();
        java.util.function.Supplier<Component<EntityStore>> supplier = () -> {
            Component<EntityStore> c = baseSupplier.get();
            if (c == null) throw new IllegalArgumentException("Component supplier returned null.");
            return c;
        };
        ComponentType<EntityStore, Component<EntityStore>> type =
                registry().registerComponent(clazz, supplier);
        componentResolver.components().put(id, type);
        registrationTracker.trackRegistration(() -> registry().unregisterComponent(type));
        return type;
    }

    public ResourceType<EntityStore, Resource<EntityStore>> registerResource(String id, Object supplierObj) {
        validateId(id, "resource id");
        java.util.function.Supplier<com.hypixel.hytale.component.Resource<EntityStore>> supplier = toResourceSupplier(supplierObj);
        ResourceType<EntityStore, Resource<EntityStore>> type = registry().registerResource(Resource.class, supplier);
        registrationTracker.trackRegistration(() -> registry().unregisterResource(type));
        return type;
    }

    // Convenience overload for JS to omit the supplier.
    public ComponentType<EntityStore, ?> registerComponent(String id) {
        return registerComponent(id, null);
    }

    // --------- Spatial resources ---------

    public ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> registerSpatialResource(Object structureObj) {
        SpatialStructure<Ref<EntityStore>> structure = resolveSpatialStructure(structureObj);
        ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> type =
                registry().registerSpatialResource(() -> structure);
        registrationTracker.trackRegistration(() -> registry().unregisterResource(type));
        return type;
    }

    public SpatialStructure<Ref<EntityStore>> spatialStructure(String kind) {
        return resolveSpatialStructure(kind);
    }

    // --------- Query helpers ---------

    public Query<EntityStore> queryAny() {
        return Query.any();
    }

    public Query<EntityStore> queryAll(Object types) {
        return Archetype.of(toComponentTypes(types));
    }

    public Query<EntityStore> queryNot(Object types) {
        return Query.not(queryAll(types));
    }

    public Query<EntityStore> queryOr(Object a, Object b) {
        return Query.or(queryAll(a), queryAll(b));
    }

    private ComponentRegistry<EntityStore> registry() {
        return EntityStore.REGISTRY;
    }

    // --------- internal helpers ---------

    private Store<EntityStore> resolveStore(Object worldLike) {
        if (worldLike instanceof Store<?> s) {
            @SuppressWarnings("unchecked")
            Store<EntityStore> store = (Store<EntityStore>) s;
            return store;
        }
        if (worldLike instanceof Ref<?> ref) {
            @SuppressWarnings("unchecked")
            Ref<EntityStore> cast = (Ref<EntityStore>) ref;
            Store<EntityStore> store = cast.getStore();
            if (store == null) throw new IllegalArgumentException("Ref has no store.");
            return store;
        }
        return resolveDefaultStore();
    }

    private Store<EntityStore> resolveDefaultStore() {
        throw new IllegalStateException("A Ref or Store is required to resolve the target EntityStore.");
    }

    private AddReason parseAddReason(Object value) {
        if (value instanceof AddReason reason) return reason;
        if (value instanceof String s) {
            try {
                return AddReason.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return AddReason.SPAWN;
    }

    @SuppressWarnings("unchecked")
    private Component<EntityStore>[] toComponentArray(Object input) {
        if (!(input instanceof Scriptable scriptable)) {
            throw new IllegalArgumentException("Components must be an array of component instances.");
        }
        int length = getLength(scriptable);
        List<Component<EntityStore>> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object raw = ScriptableObject.getProperty(scriptable, i);
            if (!(raw instanceof Component<?> component)) {
                throw new IllegalArgumentException("Entry " + i + " is not a Component.");
            }
            result.add((Component<EntityStore>) component);
        }
        Component<EntityStore>[] arr = new Component[result.size()];
        return result.toArray(arr);
    }

    private Archetype<EntityStore> archetypeFromComponents(Component<EntityStore>[] components) {
        ComponentType<EntityStore, ?>[] types = new ComponentType[components.length];
        for (int i = 0; i < components.length; i++) {
            Component<EntityStore> comp = components[i];
            ComponentType<EntityStore, ?> type = resolveComponentType(comp);
            types[i] = type;
        }
        return Archetype.of(types);
    }

    private SystemGroup<EntityStore> extractGroup(Scriptable options) {
        Object raw = ScriptableObject.getProperty(options, "group");
        if (raw == null || raw == Scriptable.NOT_FOUND) {
            return null;
        }
        if (raw instanceof NativeJavaObject njo) {
            raw = njo.unwrap();
        }
        if (raw instanceof SystemGroup<?> group) {
            @SuppressWarnings("unchecked")
            SystemGroup<EntityStore> cast = (SystemGroup<EntityStore>) group;
            return cast;
        }
        if (raw instanceof String s) {
            return resolveSystemGroupId(s);
        }
        throw new IllegalArgumentException("group must be a SystemGroup or string id.");
    }

    private SystemGroup<EntityStore> resolveSystemGroupId(String id) {
        String norm = id.toLowerCase();
        switch (norm) {
            case "damage:gather":
            case "gatherdamage":
                return damageGatherGroup();
            case "damage:filter":
            case "filterdamage":
                return damageFilterGroup();
            case "damage:inspect":
            case "inspectdamage":
                return damageInspectGroup();
            default:
                SystemGroup<EntityStore> group = registry().registerSystemGroup();
                registrationTracker.trackRegistration(() -> registry().unregisterSystemGroup(group));
                return group;
        }
    }

    private DamageCause resolveDamageCause(String id) {
        if (id == null) return DamageCause.OUT_OF_WORLD;
        // Try static fields (PHYSICAL, OUT_OF_WORLD, etc.)
        try {
            var field = DamageCause.class.getField(id.toUpperCase());
            Object value = field.get(null);
            if (value instanceof DamageCause dc) return dc;
        } catch (Exception ignored) {
        }
        return DamageCause.OUT_OF_WORLD;
    }

    @SuppressWarnings("unchecked")
    private ComponentType<EntityStore, ?> resolveComponentType(Component<?> component) {
        try {
            var method = component.getClass().getMethod("getComponentType");
            Object value = method.invoke(null);
            if (value instanceof ComponentType<?, ?> type) {
                return (ComponentType<EntityStore, ?>) type;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("Unable to resolve ComponentType for " + component.getClass().getName() + ". Provide component types explicitly in queries/spawn.");
    }

    @SuppressWarnings("unchecked")
    private SpatialStructure<Ref<EntityStore>> resolveSpatialStructure(Object raw) {
        if (raw instanceof NativeJavaObject njo) {
            raw = njo.unwrap();
        }
        if (raw instanceof SpatialStructure<?> s) {
            return (SpatialStructure<Ref<EntityStore>>) s;
        }
        if (raw == null) {
            return new KDTree<>(ref -> true);
        }
        if (raw instanceof String s) {
            // We only have KDTree available in hytale-server 1.0.2
            return new KDTree<>(ref -> true);
        }
        throw new IllegalArgumentException("Unknown spatial structure: " + raw);
    }

    private ComponentType<EntityStore, ?>[] toComponentTypes(Object value) {
        if (value instanceof NativeJavaObject njo) {
            value = njo.unwrap();
        }
        if (value instanceof String s) {
            ComponentType<EntityStore, ?> type = resolveComponentId(s);
            if (type == null) {
                throw new IllegalArgumentException("Unknown component type id '" + s + "'");
            }
            return new ComponentType[]{type};
        }
        if (value instanceof ComponentType<?, ?> single) {
            @SuppressWarnings("unchecked")
            ComponentType<EntityStore, ?> cast = (ComponentType<EntityStore, ?>) single;
            return new ComponentType[]{cast};
        }
        if (!(value instanceof Scriptable scriptable)) {
            throw new IllegalArgumentException("Expected a ComponentType or array of ComponentTypes.");
        }
        int length = getLength(scriptable);
        @SuppressWarnings("unchecked")
        ComponentType<EntityStore, ?>[] types = new ComponentType[length];
        for (int i = 0; i < length; i++) {
            Object raw = ScriptableObject.getProperty(scriptable, i);
            if (raw instanceof NativeJavaObject njo) {
                raw = njo.unwrap();
            }
            if (raw instanceof String s) {
                raw = componentResolver.components().get(s);
            }
            if (raw instanceof Component<?> component) {
                raw = resolveComponentType(component);
            }
            if (raw == null) {
                throw new IllegalArgumentException("Entry " + i + " is null (unknown component).");
            }
            if (!(raw instanceof ComponentType<?, ?> type)) {
                throw new IllegalArgumentException("Entry " + i + " is not a ComponentType.");
            }
            @SuppressWarnings("unchecked")
            ComponentType<EntityStore, ?> cast = (ComponentType<EntityStore, ?>) type;
            types[i] = cast;
        }
        return types;
    }

    private int getLength(Scriptable scriptable) {
        Object raw = ScriptableObject.getProperty(scriptable, "length");
        if (raw instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Expected a JS array.");
    }

    private Ref<EntityStore> resolveRef(Object target) {
        if (target instanceof Ref<?> rawRef) {
            @SuppressWarnings("unchecked")
            Ref<EntityStore> ref = (Ref<EntityStore>) rawRef;
            validateRef(ref);
            return ref;
        }
        if (target instanceof PlayerHandle handle) {
            Ref<EntityStore> ref = handle.getEntityRef();
            validateRef(ref);
            return ref;
        }
        if (target instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            validateRef(ref);
            return ref;
        }
        throw new IllegalArgumentException("Unsupported target for ECS operation: " + target);
    }

    private void validateRef(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            throw new IllegalArgumentException("Entity reference is not valid.");
        }
    }

    private <T extends Component<EntityStore>> T getComponent(Ref<EntityStore> ref, ComponentType<EntityStore, T> type, boolean readonly) {
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            throw new IllegalStateException("Missing store for entity.");
        }
        store.assertThread();
        return readonly ? store.getComponent(ref, type) : store.ensureAndGetComponent(ref, type);
    }

    private <T extends Component<EntityStore>> T getOrCreate(Ref<EntityStore> ref, ComponentType<EntityStore, T> type, java.util.function.Supplier<T> supplier) {
        T existing = getComponent(ref, type, true);
        if (existing != null) {
            return existing;
        }
        T created = supplier.get();
        setComponent(ref, type, created);
        return created;
    }

    private <T extends Component<EntityStore>> void setComponent(Ref<EntityStore> ref, ComponentType<EntityStore, T> type, T value) {
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            throw new IllegalStateException("Missing store for entity.");
        }
        store.assertThread();
        store.putComponent(ref, type, value);
    }

    private Scriptable ensureScriptable(Object obj, String name) {
        if (obj instanceof Scriptable scriptable) {
            return scriptable;
        }
        throw new IllegalArgumentException("Expected object for " + name);
    }

    private boolean getBoolean(Scriptable scriptable, String key, boolean fallback) {
        Object raw = ScriptableObject.getProperty(scriptable, key);
        if (raw == null || raw == Scriptable.NOT_FOUND) return fallback;
        if (raw instanceof Boolean b) return b;
        return fallback;
    }

    private String getString(Scriptable scriptable, String key, String fallback) {
        Object raw = ScriptableObject.getProperty(scriptable, key);
        if (raw == null || raw == Scriptable.NOT_FOUND) return fallback;
        return Objects.toString(raw, fallback);
    }

    private Function getFunction(Scriptable scriptable, String key, boolean required) {
        Object raw = ScriptableObject.getProperty(scriptable, key);
        if (raw instanceof Function fn) return fn;
        if (required) {
            throw new IllegalArgumentException("Missing function '" + key + "'.");
        }
        return null;
    }

    private Store<EntityStore> resolveStoreForBuffer(Object target) {
        if (target instanceof Store<?> s) {
            @SuppressWarnings("unchecked") Store<EntityStore> store = (Store<EntityStore>) s;
            return store;
        }
        if (target instanceof Ref<?> r) {
            @SuppressWarnings("unchecked") Ref<EntityStore> ref = (Ref<EntityStore>) r;
            Store<EntityStore> store = ref.getStore();
            if (store == null) throw new IllegalArgumentException("Ref has no store.");
            return store;
        }
        if (target instanceof PlayerHandle ph) {
            Ref<EntityStore> ref = ph.getEntityRef();
            if (ref == null) throw new IllegalArgumentException("Player has no entity ref.");
            Store<EntityStore> store = ref.getStore();
            if (store == null) throw new IllegalArgumentException("Ref has no store.");
            return store;
        }
        return resolveStore(target);
    }

    private void incrementSystemRef(Class<?> clazz) {
        systemRefCounts.merge(clazz, 1, Integer::sum);
    }

    private void unregisterSystemIfLast(Class<?> clazz) {
        systemRefCounts.compute(clazz, (c, count) -> {
            int next = count == null ? 0 : count - 1;
            if (next <= 0) {
                @SuppressWarnings("unchecked")
                Class<? extends ISystem<EntityStore>> cast =
                        (Class<? extends ISystem<EntityStore>>) clazz;
                registry().unregisterSystem(cast);
                return null;
            }
            return next;
        });
    }

    private Query<EntityStore> buildQuery(Scriptable options) {
        Object raw = ScriptableObject.getProperty(options, "query");
        if (raw == null || raw == Scriptable.NOT_FOUND) {
            return Query.any();
        }
        
        if (raw instanceof NativeJavaObject njo) {
            raw = njo.unwrap();
        }
        if (raw instanceof com.hypixel.hytale.component.query.Query<?> q) {
            @SuppressWarnings("unchecked")
            Query<EntityStore> cast = (Query<EntityStore>) q;
            return cast;
        }
        if (raw instanceof Query<?> q) {
            @SuppressWarnings("unchecked")
            Query<EntityStore> cast = (Query<EntityStore>) q;
            return cast;
        }
        ComponentType<EntityStore, ?>[] types = toComponentTypes(raw);
        return Archetype.of(types);
    }

    private ComponentType<EntityStore, ?> extractComponentType(Scriptable options) {
        Object raw = ScriptableObject.getProperty(options, "component");
        if (raw instanceof NativeJavaObject njo) {
            raw = njo.unwrap();
        }
        if (raw instanceof ComponentType<?, ?> type) {
            @SuppressWarnings("unchecked")
            ComponentType<EntityStore, ?> cast = (ComponentType<EntityStore, ?>) type;
            return cast;
        }
        throw new IllegalArgumentException("component: ComponentType is required.");
    }

    private EcsEvent castEvent(Object event, String label) {
        if (event instanceof EcsEvent evt) {
            return evt;
        }
        throw new IllegalArgumentException("Expected " + label + " to extend EcsEvent.");
    }

    @SuppressWarnings("unchecked")
    private Class<? extends EcsEvent> resolveEventClass(Object raw) {
        if (raw instanceof NativeJavaObject njo) {
            raw = njo.unwrap();
        }
        if (raw instanceof Class<?> clazz) {
            if (EcsEvent.class.isAssignableFrom(clazz)) {
                return (Class<? extends EcsEvent>) clazz;
            }
            throw new IllegalArgumentException("event must extend EcsEvent.");
        }
        if (raw instanceof String s) {
            String key = s.endsWith("Event") ? s.substring(0, s.length() - "Event".length()) : s;
            // Try built-ins first (case-insensitive).
            for (Map.Entry<String, Class<?>> entry : builtinEvents.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key) || entry.getKey().equalsIgnoreCase(key + "Event")) {
                    return (Class<? extends EcsEvent>) entry.getValue();
                }
            }
            try {
                Class<?> clazz = Class.forName(s);
                if (EcsEvent.class.isAssignableFrom(clazz)) {
                    return (Class<? extends EcsEvent>) clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new IllegalArgumentException("Unknown ECS event '" + raw + "'");
    }

    private void ensureEntityEventType(Class<? extends EcsEvent> clazz) {
        var reg = registry();
        var existing = reg.getEntityEventTypeForClass(clazz);
        if (existing == null) {
            var type = reg.registerEntityEventType(clazz);
            registrationTracker.trackRegistration(() -> reg.unregisterEntityEventType(type));
        }
    }

    private void ensureWorldEventType(Class<? extends EcsEvent> clazz) {
        var reg = registry();
        var existing = reg.getWorldEventTypeForClass(clazz);
        if (existing == null) {
            var type = reg.registerWorldEventType(clazz);
            registrationTracker.trackRegistration(() -> reg.unregisterWorldEventType(type));
        }
    }

    private void validateId(String id, String label) {
        if (id == null || id.isBlank() || !id.matches("^[a-z0-9_-]+$")) {
            throw new IllegalArgumentException(label + " must match [a-z0-9_-]");
        }
    }

    private java.util.function.Supplier<Component<EntityStore>> toComponentSupplier(Object supplierObj) {
        if (supplierObj instanceof Function fn) {
            return () -> {
                Object obj = runtime.callFunction(fn);
                if (obj instanceof Component<?> c) {
                    @SuppressWarnings("unchecked")
                    Component<EntityStore> comp = (Component<EntityStore>) c;
                    return comp;
                }
                throw new IllegalArgumentException("Component supplier must return a Component.");
            };
        }
        return JsDynamicComponent::new;
    }

    private java.util.function.Supplier<com.hypixel.hytale.component.Resource<EntityStore>> toResourceSupplier(Object supplierObj) {
        if (supplierObj instanceof Function fn) {
            return () -> {
                Object obj = runtime.callFunction(fn);
                if (obj instanceof com.hypixel.hytale.component.Resource<?> r) {
                    @SuppressWarnings("unchecked")
                    com.hypixel.hytale.component.Resource<EntityStore> res = (com.hypixel.hytale.component.Resource<EntityStore>) r;
                    return res;
                }
                throw new IllegalArgumentException("Resource supplier must return a Resource.");
            };
        }
        return () -> new com.hypixel.hytale.component.Resource<>() {
            @Override
            public com.hypixel.hytale.component.Resource<EntityStore> clone() {
                return this;
            }
        };
    }

}
