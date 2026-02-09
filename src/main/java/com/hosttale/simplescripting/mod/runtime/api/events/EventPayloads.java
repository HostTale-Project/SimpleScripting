package com.hosttale.simplescripting.mod.runtime.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hosttale.simplescripting.mod.runtime.api.entities.EntityHandle;
import com.hosttale.simplescripting.mod.runtime.api.events.GenericEvent;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayerHandle;

import java.util.List;

public final class EventPayloads {

    private EventPayloads() {
    }

    @SuppressWarnings("removal") // getUuid() deprecated, no alternative for Player -> PlayerRef lookup
    public static Object adapt(Object event) {
        // Player events
        if (event instanceof PlayerChatEvent chat) {
            return new PlayerChat(chat);
        }
        if (event instanceof PlayerConnectEvent connect) {
            com.hypixel.hytale.server.core.universe.PlayerRef playerRef = connect.getPlayerRef();
            if (playerRef == null) {
                throw new IllegalStateException("PlayerConnectEvent.getPlayerRef() returned null");
            }
            return new PlayerRefPayload("playerConnect", playerRef);
        }
        if (event instanceof PlayerDisconnectEvent disconnect) {
            com.hypixel.hytale.server.core.universe.PlayerRef playerRef = disconnect.getPlayerRef();
            if (playerRef == null) {
                throw new IllegalStateException("PlayerDisconnectEvent.getPlayerRef() returned null");
            }
            return new PlayerRefPayload("playerDisconnect", playerRef);
        }
        if (event instanceof PlayerReadyEvent ready) {
            var player = ready.getPlayer();
            com.hypixel.hytale.server.core.universe.PlayerRef ref = null;
            
            if (player != null) {
                // Try Universe lookup first (proper non-deprecated approach)
                com.hypixel.hytale.server.core.universe.Universe universe = com.hypixel.hytale.server.core.universe.Universe.get();
                if (universe != null) {
                    ref = universe.getPlayer(player.getUuid());
                }
                
                // Fallback to deprecated method for test environments
                if (ref == null) {
                    ref = player.getPlayerRef();
                }
            }
            
            return new PlayerRefPayload("playerReady", ref);
        }
        
        // Inventory events (Phase 0 - Priority 1)
        if (event instanceof LivingEntityInventoryChangeEvent inventoryChange) {
            return new LivingEntityInventoryChange(inventoryChange);
        }
        if (event instanceof InteractivelyPickupItemEvent pickup) {
            return new InteractivelyPickupItem(pickup);
        }
        if (event instanceof SwitchActiveSlotEvent switchSlot) {
            return new SwitchActiveSlot(switchSlot);
        }
        
        return new GenericEvent(event);
    }

    public static final class PlayerChat {
        private final PlayerChatEvent delegate;

        public PlayerChat(PlayerChatEvent delegate) {
            this.delegate = delegate;
        }

        public String getType() {
            return "playerChat";
        }

        public PlayerHandle getSender() {
            return new PlayerHandle(delegate.getSender());
        }

        /**
         * Alias for getSender() to keep names predictable in JS.
         */
        public PlayerHandle getPlayer() {
            return new PlayerHandle(delegate.getSender());
        }

        /**
         * Alias for getSender() for backwards compatibility.
         */
        public PlayerHandle getPlayerRef() {
            return new PlayerHandle(delegate.getSender());
        }

        public List<PlayerHandle> getTargets() {
            return delegate.getTargets().stream()
                    .map(PlayerHandle::new)
                    .toList();
        }

        public String getMessage() {
            return delegate.getContent();
        }

        public void setMessage(String message) {
            delegate.setContent(message);
        }

        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        public void cancel() {
            delegate.setCancelled(true);
        }

    }

    public static final class PlayerRefPayload {
        private final String type;
        private final PlayerHandle player;

        public PlayerRefPayload(String type, com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
            this.type = type;
            this.player = new PlayerHandle(playerRef);
        }

        public String getType() {
            return type;
        }

        public PlayerHandle getPlayer() {
            return player;
        }

        public PlayerHandle getPlayerRef() {
            return player;
        }
    }

    /**
     * Wrapper for LivingEntityInventoryChangeEvent.
     * Fired when a living entity's inventory changes.
     */
    public static final class LivingEntityInventoryChange {
        private final LivingEntityInventoryChangeEvent delegate;

        public LivingEntityInventoryChange(LivingEntityInventoryChangeEvent delegate) {
            this.delegate = delegate;
        }

        public String getType() {
            return "livingEntityInventoryChange";
        }

        /**
         * Get the entity reference for the living entity whose inventory changed.
         * Note: This returns the LivingEntity directly from the event.
         * Phase 1 will provide proper EntityRef access through LivingEntityHandle.
         */
        public Object getLivingEntity() {
            return delegate.getEntity();
        }

        /**
         * Get the item container that changed.
         * Returns the raw Hytale ItemContainer - Phase 1 will provide InventoryHandle wrapper.
         */
        public ItemContainer getItemContainer() {
            return delegate.getItemContainer();
        }

        /**
         * Get the transaction that caused the inventory change.
         * Contains details about what items were added/removed/moved.
         */
        public Transaction getTransaction() {
            return delegate.getTransaction();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /**
     * Wrapper for InteractivelyPickupItemEvent.
     * Fired when a player attempts to pick up an item interactively.
     * This is a cancellable event.
     */
    public static final class InteractivelyPickupItem {
        private final InteractivelyPickupItemEvent delegate;

        public InteractivelyPickupItem(InteractivelyPickupItemEvent delegate) {
            this.delegate = delegate;
        }

        public String getType() {
            return "interactivelyPickupItem";
        }

        /**
         * Get the ItemStack being picked up.
         */
        public ItemStack getItemStack() {
            return delegate.getItemStack();
        }

        /**
         * Set the ItemStack to be picked up (allows modifying the pickup).
         */
        public void setItemStack(ItemStack itemStack) {
            delegate.setItemStack(itemStack);
        }

        /**
         * Check if the event is cancelled.
         */
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        /**
         * Cancel the pickup event.
         */
        public void cancel() {
            delegate.setCancelled(true);
        }

        /**
         * Allow the pickup event (undo cancellation).
         */
        public void allow() {
            delegate.setCancelled(false);
        }
    }

    /**
     * Wrapper for SwitchActiveSlotEvent.
     * Fired when a player switches their active hotbar slot.
     * This is a cancellable event.
     */
    public static final class SwitchActiveSlot {
        private final SwitchActiveSlotEvent delegate;

        public SwitchActiveSlot(SwitchActiveSlotEvent delegate) {
            this.delegate = delegate;
        }

        public String getType() {
            return "switchActiveSlot";
        }

        /**
         * Get the previous slot index.
         */
        public int getPreviousSlot() {
            return delegate.getPreviousSlot();
        }

        /**
         * Get the new slot index.
         */
        public byte getNewSlot() {
            return delegate.getNewSlot();
        }

        /**
         * Set the new slot index (allows redirecting the slot switch).
         */
        public void setNewSlot(byte newSlot) {
            delegate.setNewSlot(newSlot);
        }

        /**
         * Check if this was a server-initiated request.
         */
        public boolean isServerRequest() {
            return delegate.isServerRequest();
        }

        /**
         * Check if this was a client-initiated request.
         */
        public boolean isClientRequest() {
            return delegate.isClientRequest();
        }

        /**
         * Get the inventory section ID.
         */
        public int getInventorySectionId() {
            return delegate.getInventorySectionId();
        }

        /**
         * Check if the event is cancelled.
         */
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        /**
         * Cancel the slot switch.
         */
        public void cancel() {
            delegate.setCancelled(true);
        }

        /**
         * Allow the slot switch (undo cancellation).
         */
        public void allow() {
            delegate.setCancelled(false);
        }
    }
}
