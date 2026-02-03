package com.hosttale.simplescripting.mod.runtime.api.entities;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Wrapper for LivingEntity that provides inventory access and other living entity functionality.
 * This class is extended by PlayerHandle, NpcHandle, and other entity-specific handles.
 * 
 * <p>LivingEntity is the base class in Hytale for all entities that have inventories,
 * health, and other living characteristics.</p>
 */
public class LivingEntityHandle extends EntityHandle {

    /**
     * Create a new living entity handle.
     * 
     * @param entityRef The entity reference from the ECS system
     * @throws IllegalArgumentException if entityRef is null
     */
    protected LivingEntityHandle(Ref<EntityStore> entityRef) {
        super(entityRef);
    }

    /**
     * Get this living entity's inventory.
     * 
     * <p><strong>Note:</strong> This will be implemented in Phase 1 of the inventory API.
     * For now, it returns null as a placeholder until InventoryHandle is created.</p>
     * 
     * @return The inventory handle, or null if the entity is invalid or has no inventory
     */
    public Object getInventory() {
        if (!isValid()) {
            return null;
        }
        
        // TODO: Implement in Phase 1 after InventoryHandle is created
        // LivingEntity livingEntity = getLivingEntity();
        // if (livingEntity == null) {
        //     return null;
        // }
        // 
        // Inventory inventory = livingEntity.getInventory();
        // if (inventory == null) {
        //     return null;
        // }
        // 
        // return new InventoryHandle(inventory);
        
        throw new UnsupportedOperationException(
            "Inventory access will be implemented in Phase 1 of inventory API. " +
            "This requires InventoryHandle class to be created first."
        );
    }

    /**
     * Check if this entity has an inventory.
     * 
     * <p><strong>Note:</strong> This will be properly implemented in Phase 1.
     * For now, it always returns false as a safe default.</p>
     * 
     * @return true if the entity has an inventory, false otherwise
     */
    public boolean hasInventory() {
        // TODO: Implement in Phase 1
        // return getInventory() != null;
        return false;
    }

    /**
     * Get the underlying LivingEntity instance (internal use).
     * 
     * <p>This method extracts the LivingEntity from the ECS entity reference.
     * It's protected so subclasses can use it, but it's not exposed to JavaScript.</p>
     * 
     * @return The LivingEntity instance, or null if invalid or not a living entity
     */
    protected LivingEntity getLivingEntity() {
        Entity entity = getEntity(); // This will throw UnsupportedOperationException for now
        if (entity instanceof LivingEntity) {
            return (LivingEntity) entity;
        }
        return null;
    }
}
