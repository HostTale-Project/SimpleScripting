package com.hosttale.simplescripting.mod.runtime.api.entities;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Wrapper for NPC entities.
 * 
 * <p>This is a placeholder for future NPC API implementation. NPCs share the same
 * inventory capabilities as players through the LivingEntityHandle base class.</p>
 * 
 * <p>Future enhancements may include:</p>
 * <ul>
 *   <li>Dialogue management - {@code getDialogue()}, {@code setDialogue()}</li>
 *   <li>Trade management - {@code getTrades()}, {@code addTrade()}</li>
 *   <li>AI behavior - {@code getAI()}, {@code setAI()}</li>
 *   <li>Spawn management - {@code spawn()}, {@code despawn()}</li>
 *   <li>Custom properties - {@code setProperty()}, {@code getProperty()}</li>
 * </ul>
 */
public class NpcHandle extends LivingEntityHandle {

    /**
     * Create a new NPC handle.
     * 
     * @param entityRef The entity reference for this NPC
     * @throws IllegalArgumentException if entityRef is null
     */
    public NpcHandle(Ref<EntityStore> entityRef) {
        super(entityRef);
    }

    // Future NPC-specific methods will be added here:
    
    // public DialogueHandle getDialogue() { ... }
    // public void setDialogue(DialogueHandle dialogue) { ... }
    // public TradeHandle[] getTrades() { ... }
    // public void addTrade(TradeHandle trade) { ... }
    // public AIHandle getAI() { ... }
    // public void setAI(AIHandle ai) { ... }
    // etc.
}
