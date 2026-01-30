package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.PositionDataComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates built-in component discovery and resolution.
 */
final class EcsComponentResolver {

    private final HytaleLogger logger;
    private final Map<String, ComponentType<EntityStore, ?>> builtinComponents = new LinkedHashMap<>();

    EcsComponentResolver(HytaleLogger logger) {
        this.logger = logger;
        loadBuiltinComponents();
    }

    Map<String, ComponentType<EntityStore, ?>> components() {
        return builtinComponents;
    }

    ComponentType<EntityStore, ?> resolveComponentId(String id) {
        if (id == null) {
            return null;
        }
        String idLower = id.toLowerCase();
        ComponentType<EntityStore, ?> type = builtinComponents.get(id);
        if (type == null) {
            type = builtinComponents.get(idLower);
        }
        if (type != null) {
            return type;
        }

        refreshCoreComponentTypes();
        type = builtinComponents.get(id);
        if (type == null) {
            type = builtinComponents.get(idLower);
        }
        if (type != null) {
            return type;
        }

        type = resolveCoreByName(idLower);
        if (type != null) {
            registerAliases(type, type.getTypeClass().getSimpleName());
            return type;
        }

        type = resolveViaEntityModule(idLower);
        if (type != null) {
            registerAliases(type, type.getTypeClass().getSimpleName());
            return type;
        }

        String[] prefixes = new String[]{
                "",
                "com.hypixel.hytale.server.core.modules.entity.component.",
                "com.hypixel.hytale.server.core.modules.physics.component.",
                "com.hypixel.hytale.server.core.modules.projectile.component.",
                "com.hypixel.hytale.server.core.modules.interaction.components.",
                "com.hypixel.hytale.server.core.blocktype.component.",
                "com.hypixel.hytale.component.",
                "com.hypixel.hytale.server.core.universe."
        };
        boolean hasSuffix = id.endsWith("Component");
        String base = id;
        String withSuffix = hasSuffix ? id : id + "Component";
        for (String prefix : prefixes) {
            String[] candidates = new String[]{prefix + base, prefix + withSuffix};
            for (String candidate : candidates) {
                try {
                    Class<?> clazz = Class.forName(candidate);
                    if (!Component.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    Object value = clazz.getMethod("getComponentType").invoke(null);
                    if (!(value instanceof ComponentType<?, ?> ct) || ct == null) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    ComponentType<EntityStore, ?> cast = (ComponentType<EntityStore, ?>) ct;
                    builtinComponents.put(id, cast);
                    builtinComponents.put(clazz.getSimpleName(), cast);
                    builtinComponents.put(clazz.getSimpleName().toLowerCase(), cast);
                    return cast;
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private void loadBuiltinComponents() {
        Class<?>[] classes = new Class<?>[]{
                TransformComponent.class,
                HeadRotation.class,
                Velocity.class,
                BoundingBox.class,
                PositionDataComponent.class,
                com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent.class,
                com.hypixel.hytale.server.core.modules.entity.component.DynamicLight.class,
                com.hypixel.hytale.server.core.modules.entity.component.PersistentDynamicLight.class,
                com.hypixel.hytale.server.core.modules.entity.component.ModelComponent.class,
                com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent.class,
                com.hypixel.hytale.server.core.modules.entity.component.RotateObjectComponent.class,
                com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues.class,
                com.hypixel.hytale.server.core.modules.projectile.component.Projectile.class,
                com.hypixel.hytale.server.core.modules.projectile.component.PredictedProjectile.class,
                com.hypixel.hytale.server.core.modules.interaction.components.PlacedByInteractionComponent.class,
                com.hypixel.hytale.server.core.blocktype.component.BlockPhysics.class,
                com.hypixel.hytale.component.NonTicking.class,
                com.hypixel.hytale.component.NonSerialized.class,
                com.hypixel.hytale.server.core.universe.PlayerRef.class
        };
        for (Class<?> clazz : classes) {
            try {
                Object value = clazz.getMethod("getComponentType").invoke(null);
                if (value instanceof ComponentType<?, ?> type) {
                    @SuppressWarnings("unchecked")
                    ComponentType<EntityStore, ?> cast = (ComponentType<EntityStore, ?>) type;
                    registerAliases(cast, clazz.getSimpleName());
                }
            } catch (Exception ignored) {
            }
        }
        safeAlias("TransformComponent", TransformComponent::getComponentType);
        safeAlias("Velocity", Velocity::getComponentType);
        safeAlias("HeadRotation", HeadRotation::getComponentType);
        safeAlias("BoundingBox", BoundingBox::getComponentType);
        safeAlias("PositionDataComponent", PositionDataComponent::getComponentType);
        tryAlias("DynamicLight", "PersistentDynamicLight",
                com.hypixel.hytale.server.core.modules.entity.component.DynamicLight::getComponentType,
                com.hypixel.hytale.server.core.modules.entity.component.PersistentDynamicLight::getComponentType);
        safeAlias("PlayerRef", com.hypixel.hytale.server.core.universe.PlayerRef::getComponentType);
    }

    private void registerAliases(ComponentType<EntityStore, ?> type, String simpleName) {
        builtinComponents.put(simpleName, type);
        builtinComponents.put(simpleName.toLowerCase(), type);
        if (simpleName.endsWith("Component")) {
            String base = simpleName.substring(0, simpleName.length() - "Component".length());
            builtinComponents.put(base, type);
            builtinComponents.put(base.toLowerCase(), type);
        }
    }

    private void forceRegisterAlias(String id, ComponentType<EntityStore, ?> type) {
        if (type == null) return;
        builtinComponents.put(id, type);
        builtinComponents.put(id.toLowerCase(), type);
        if (id.endsWith("Component")) {
            String base = id.substring(0, id.length() - "Component".length());
            builtinComponents.put(base, type);
            builtinComponents.put(base.toLowerCase(), type);
        }
    }

    @SafeVarargs
    private void tryAlias(String primaryId, String fallbackId, java.util.function.Supplier<ComponentType<EntityStore, ?>>... suppliers) {
        for (java.util.function.Supplier<ComponentType<EntityStore, ?>> s : suppliers) {
            try {
                ComponentType<EntityStore, ?> type = s.get();
                if (type != null) {
                    forceRegisterAlias(primaryId, type);
                    if (fallbackId != null) {
                        forceRegisterAlias(fallbackId, type);
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void safeAlias(String id, java.util.function.Supplier<ComponentType<EntityStore, ?>> supplier) {
        try {
            ComponentType<EntityStore, ?> type = supplier.get();
            forceRegisterAlias(id, type);
        } catch (Exception ignored) {
        }
    }

    private ComponentType<EntityStore, ?> resolveCoreByName(String idLower) {
        try {
            return switch (idLower) {
                case "transformcomponent", "transform" -> TransformComponent.getComponentType();
                case "velocity" -> Velocity.getComponentType();
                case "headrotation" -> HeadRotation.getComponentType();
                case "boundingbox" -> BoundingBox.getComponentType();
                case "positiondatacomponent", "positiondata" -> PositionDataComponent.getComponentType();
                case "dynamiclight" -> com.hypixel.hytale.server.core.modules.entity.component.DynamicLight.getComponentType();
                case "persistentdynamiclight" -> com.hypixel.hytale.server.core.modules.entity.component.PersistentDynamicLight.getComponentType();
                case "playerref" -> com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType();
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private ComponentType<EntityStore, ?> resolveViaEntityModule(String idLower) {
        try {
            var module = com.hypixel.hytale.server.core.modules.entity.EntityModule.get();
            if (module == null) return null;
            return switch (idLower) {
                case "transformcomponent", "transform" -> module.getTransformComponentType();
                case "velocity" -> module.getVelocityComponentType();
                case "headrotation" -> module.getHeadRotationComponentType();
                case "boundingbox" -> module.getBoundingBoxComponentType();
                case "positiondatacomponent", "positiondata" -> module.getPositionDataComponentType();
                case "dynamiclight" -> module.getDynamicLightComponentType();
                case "persistentdynamiclight" -> module.getPersistentDynamicLightComponentType();
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private void refreshCoreComponentTypes() {
        try {
            var module = com.hypixel.hytale.server.core.modules.entity.EntityModule.get();
            if (module == null) {
                return;
            }
            for (var method : module.getClass().getMethods()) {
                if (method.getParameterCount() != 0) continue;
                if (!ComponentType.class.isAssignableFrom(method.getReturnType())) continue;
                String name = method.getName();
                if (!name.endsWith("ComponentType")) continue;
                Object value = method.invoke(module);
                if (!(value instanceof ComponentType<?, ?> ct) || ct == null) continue;
                @SuppressWarnings("unchecked")
                ComponentType<EntityStore, ?> cast = (ComponentType<EntityStore, ?>) ct;
                Class<?> typeClass = cast.getTypeClass();
                String simpleName = typeClass != null ? typeClass.getSimpleName()
                        : name.substring(0, name.length() - "ComponentType".length());
                forceRegisterAlias(simpleName, cast);
            }
        } catch (Exception e) {
            logger.atWarning().log("Unable to refresh core component types: %s", e.getMessage());
        }
    }
}
