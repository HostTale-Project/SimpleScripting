package com.hosttale.simplescripting.mod.runtime.api.entities;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Base class for entity wrappers providing common functionality.
 * Enables safe entity access from JavaScript without exposing native classes directly.
 * 
 * <p>This class serves as the foundation for all entity-related handles (PlayerHandle, 
 * NpcHandle, etc.) and provides basic entity reference management and validation.</p>
 */
public abstract class EntityHandle {

    protected final Ref<EntityStore> entityRef;

    /**
     * Create a new entity handle wrapping the given entity reference.
     * 
     * @param entityRef The entity reference from the ECS system
     * @throws IllegalArgumentException if entityRef is null
     */
    protected EntityHandle(Ref<EntityStore> entityRef) {
        if (entityRef == null) {
            throw new IllegalArgumentException("Entity reference cannot be null");
        }
        this.entityRef = entityRef;
    }

    /**
     * Get the entity reference for ECS access.
     * This allows JavaScript code to access the entity through the ECS API.
     * 
     * @return The entity reference
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Check if this entity reference is still valid.
     * An entity reference becomes invalid when the entity is removed from the world.
     * 
     * @return true if the entity reference is valid and the entity still exists
     */
    public boolean isValid() {
        return entityRef != null && entityRef.isValid();
    }

    /**
     * Get a string representation of the entity ID for logging and debugging.
     * 
     * @return A string representation of the entity reference
     */
    public String getEntityId() {
        if (entityRef == null) {
            return "null";
        }
        return entityRef.toString();
    }

    /**
     * Get the underlying Entity instance (internal use only).
     * 
     * <p><strong>Note:</strong> This method requires accessing the entity from the store,
     * which is not yet implemented. Subclasses should not expose this directly to JavaScript.</p>
     * 
     * @return The entity instance, or null if the entity is invalid or cannot be retrieved
     * @throws UnsupportedOperationException if entity extraction is not yet implemented
     */
    protected Entity getEntity() {
        if (!isValid()) {
            return null;
        }
        // TODO: Implement entity extraction from EntityStore via Ref
        // This requires accessing the Store and getting the entity component
        // For now, this throws UnsupportedOperationException until we implement it
        throw new UnsupportedOperationException(
            "Entity extraction from EntityStore not yet implemented. " +
            "This will be completed in Phase 1 of inventory API implementation."
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EntityHandle other = (EntityHandle) obj;
        return entityRef != null && entityRef.equals(other.entityRef);
    }

    @Override
    public int hashCode() {
        return entityRef != null ? entityRef.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{entityRef=" + getEntityId() + "}";
    }
}
