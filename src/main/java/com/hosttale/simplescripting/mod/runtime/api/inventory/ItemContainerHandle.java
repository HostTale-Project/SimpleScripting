package com.hosttale.simplescripting.mod.runtime.api.inventory;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import org.mozilla.javascript.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * JavaScript-safe wrapper for Hytale's ItemContainer class.
 * 
 * <p>ItemContainer represents a collection of item slots (hotbar, storage, armor, etc.).
 * Provides operations for adding, removing, moving, and searching items.</p>
 * 
 * <h3>Key Features</h3>
 * <ul>
 *   <li><strong>Slot-Based</strong>: Access items by slot index (0 to capacity-1)</li>
 *   <li><strong>Transaction-Based</strong>: Operations return results indicating success/failure</li>
 *   <li><strong>Search/Query</strong>: Find items by predicate, count, check existence</li>
 *   <li><strong>Iteration</strong>: forEach, map, filter over non-empty slots</li>
 *   <li><strong>Transfer</strong>: Move items between containers</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // JavaScript
 * const container = player.getInventory();
 * console.info('Capacity:', container.capacity, 'Empty:', container.empty);
 * 
 * // Add items
 * const stack = inventory.createStack('Stone', 64);
 * const result = container.addItem(stack);
 * 
 * // Search
 * const stoneCount = container.count((item, slot) => item.itemId === 'Stone');
 * console.info('Stone count:', stoneCount);
 * 
 * // Iterate
 * container.forEach((item, slot) => {
 *   console.info('Slot', slot, ':', item.itemId, 'x', item.quantity);
 * });
 * }</pre>
 */
public final class ItemContainerHandle {

    private final ItemContainer delegate;
    private final Scriptable scope;

    /**
     * Create a new ItemContainerHandle wrapping a Hytale ItemContainer.
     * 
     * @param delegate The ItemContainer to wrap
     * @param scope The JavaScript scope for object conversion
     * @throws IllegalArgumentException if delegate is null
     */
    public ItemContainerHandle(ItemContainer delegate, Scriptable scope) {
        if (delegate == null) {
            throw new IllegalArgumentException("ItemContainer cannot be null");
        }
        this.delegate = delegate;
        this.scope = scope;
    }

    // ===== Properties =====

    /**
     * Get the container's capacity (number of slots).
     * 
     * @return The total number of slots in this container
     */
    public int getCapacity() {
        return delegate.getCapacity();
    }

    /**
     * Check if container is completely empty.
     * 
     * @return true if all slots are empty, false otherwise
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    // ===== Slot Operations =====

    /**
     * Get the item stack at a specific slot.
     * Returns null if slot is empty or out of bounds.
     * 
     * @param slot The slot index (0 to capacity-1)
     * @return The item at that slot, or null if empty/invalid
     */
    public ItemStackHandle getItem(int slot) {
        if (slot < 0 || slot >= getCapacity()) {
            return null;
        }
        
        ItemStack stack = delegate.getItemStack((short) slot);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        
        return new ItemStackHandle(stack, scope);
    }

    /**
     * Set the item at a specific slot.
     * Replaces any existing item. Pass null to clear the slot.
     * 
     * @param slot The slot index
     * @param item The item to set, or null to clear
     * @return Transaction result with success status
     */
    public TransactionResultHandle setItem(int slot, ItemStackHandle item) {
        if (slot < 0 || slot >= getCapacity()) {
            return TransactionResultHandle.failure("Invalid slot: " + slot);
        }
        
        ItemStack stack = item != null ? item.getDelegate() : null;
        ItemStackSlotTransaction tx = delegate.setItemStackForSlot((short) slot, stack);
        
        return TransactionResultHandle.fromSlotTransaction(tx, scope);
    }

    /**
     * Add an item to a specific slot.
     * Will stack with existing item if compatible.
     * 
     * @param slot The slot index
     * @param item The item to add
     * @return Transaction result with remainder if any
     */
    public TransactionResultHandle addToSlot(int slot, ItemStackHandle item) {
        if (slot < 0 || slot >= getCapacity()) {
            return TransactionResultHandle.failure("Invalid slot: " + slot);
        }
        if (item == null) {
            return TransactionResultHandle.failure("Item cannot be null");
        }
        
        ItemStackSlotTransaction tx = delegate.addItemStackToSlot((short) slot, item.getDelegate());
        return TransactionResultHandle.fromSlotTransaction(tx, scope);
    }

    /**
     * Remove the item at a specific slot.
     * Returns the removed item, or null if slot was empty.
     * 
     * @param slot The slot index
     * @return The removed item, or null if empty
     */
    public ItemStackHandle removeFromSlot(int slot) {
        if (slot < 0 || slot >= getCapacity()) {
            return null;
        }
        
        SlotTransaction tx = delegate.removeItemStackFromSlot((short) slot);
        if (!tx.succeeded()) {
            return null;
        }
        
        ItemStack removed = tx.getSlotBefore();
        if (removed == null || removed.isEmpty()) {
            return null;
        }
        
        return new ItemStackHandle(removed, scope);
    }

    /**
     * Remove a specific quantity from a slot.
     * 
     * @param slot The slot index
     * @param quantity How many to remove
     * @return Transaction result with removed item
     */
    public TransactionResultHandle removeFromSlot(int slot, int quantity) {
        if (slot < 0 || slot >= getCapacity()) {
            return TransactionResultHandle.failure("Invalid slot: " + slot);
        }
        if (quantity <= 0) {
            return TransactionResultHandle.failure("Quantity must be positive");
        }
        
        ItemStackSlotTransaction tx = delegate.removeItemStackFromSlot((short) slot, quantity);
        return TransactionResultHandle.fromSlotTransaction(tx, scope);
    }

    /**
     * Clear a specific slot.
     * Equivalent to setItem(slot, null).
     * 
     * @param slot The slot index
     * @return Transaction result
     */
    public TransactionResultHandle clearSlot(int slot) {
        return setItem(slot, null);
    }

    // ===== Container-Wide Operations =====

    /**
     * Add an item to the container.
     * Automatically finds available slots and stacks where possible.
     * 
     * @param item The item to add
     * @return Transaction result with remainder if item doesn't fully fit
     */
    public TransactionResultHandle addItem(ItemStackHandle item) {
        if (item == null) {
            return TransactionResultHandle.failure("Item cannot be null");
        }
        
        ItemStackTransaction tx = delegate.addItemStack(item.getDelegate());
        return TransactionResultHandle.fromItemStackTransaction(tx, scope);
    }

    /**
     * Add multiple items to the container.
     * 
     * @param items Array of items to add
     * @return Array of transaction results, one per item
     */
    public Object addItems(Object items) {
        if (!(items instanceof NativeArray jsArray)) {
            throw new IllegalArgumentException("Expected array of items");
        }
        
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < jsArray.size(); i++) {
            Object obj = jsArray.get(i);
            if (obj instanceof ItemStackHandle handle) {
                stacks.add(handle.getDelegate());
            }
        }
        
        ListTransaction<ItemStackTransaction> listTx = delegate.addItemStacks(stacks);
        return TransactionResultHandle.fromListTransaction(listTx, scope);
    }

    /**
     * Check if items can be added (enough space).
     * 
     * @param items Array of items to check
     * @return true if all items can fit, false otherwise
     */
    public boolean canAddItems(Object items) {
        if (!(items instanceof NativeArray jsArray)) {
            return false;
        }
        
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < jsArray.size(); i++) {
            Object obj = jsArray.get(i);
            if (obj instanceof ItemStackHandle handle) {
                stacks.add(handle.getDelegate());
            }
        }
        
        return delegate.canAddItemStacks(stacks);
    }

    /**
     * Remove a specific item stack.
     * Searches for matching items and removes the quantity.
     * 
     * @param item The item to remove (matches by type and quantity)
     * @return Transaction result with removed item
     */
    public TransactionResultHandle removeItem(ItemStackHandle item) {
        if (item == null) {
            return TransactionResultHandle.failure("Item cannot be null");
        }
        
        ItemStackTransaction tx = delegate.removeItemStack(item.getDelegate());
        return TransactionResultHandle.fromItemStackTransaction(tx, scope);
    }

    /**
     * Check if a specific item can be removed.
     * 
     * @param item The item to check
     * @return true if the item can be removed, false otherwise
     */
    public boolean canRemoveItem(ItemStackHandle item) {
        if (item == null) {
            return false;
        }
        return delegate.canRemoveItemStack(item.getDelegate());
    }

    /**
     * Clear the entire container.
     * 
     * @return Transaction result
     */
    public TransactionResultHandle clear() {
        ClearTransaction tx = delegate.clear();
        return TransactionResultHandle.fromClearTransaction(tx, scope);
    }

    // ===== Searching and Querying =====

    /**
     * Count items matching a predicate.
     * 
     * @param predicate JavaScript function (item, slot) => boolean
     * @return The count of matching items
     */
    public int count(Object predicate) {
        if (!(predicate instanceof Function)) {
            throw new IllegalArgumentException("Predicate must be a function");
        }
        
        Function predicateFn = (Function) predicate;
        int count = 0;
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                Object result = predicateFn.call(
                    Context.getCurrentContext(),
                    scope,
                    scope,
                    new Object[]{handle, (int) slot}
                );
                
                if (result instanceof Boolean && (Boolean) result) {
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * Find first slot matching predicate.
     * 
     * @param predicate JavaScript function (item, slot) => boolean
     * @return Slot number or -1 if not found
     */
    public int findSlot(Object predicate) {
        if (!(predicate instanceof Function)) {
            return -1;
        }
        
        Function predicateFn = (Function) predicate;
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                Object result = predicateFn.call(
                    Context.getCurrentContext(),
                    scope,
                    scope,
                    new Object[]{handle, (int) slot}
                );
                
                if (result instanceof Boolean && (Boolean) result) {
                    return slot;
                }
            }
        }
        
        return -1;
    }

    /**
     * Find all slots matching predicate.
     * 
     * @param predicate JavaScript function (item, slot) => boolean
     * @return JavaScript array of slot numbers
     */
    public Object findSlots(Object predicate) {
        if (!(predicate instanceof Function)) {
            return Context.getCurrentContext().newArray(scope, 0);
        }
        
        Function predicateFn = (Function) predicate;
        List<Integer> slots = new ArrayList<>();
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                Object result = predicateFn.call(
                    Context.getCurrentContext(),
                    scope,
                    scope,
                    new Object[]{handle, (int) slot}
                );
                
                if (result instanceof Boolean && (Boolean) result) {
                    slots.add((int) slot);
                }
            }
        }
        
        return Context.getCurrentContext().newArray(scope, slots.toArray());
    }

    /**
     * Check if container contains items stackable with given stack.
     * 
     * @param item The item to check for stackable matches
     * @return true if container has stackable items, false otherwise
     */
    public boolean containsStackable(ItemStackHandle item) {
        if (item == null) {
            return false;
        }
        return delegate.containsItemStacksStackableWith(item.getDelegate());
    }

    /**
     * Check if container contains at least quantity of itemId.
     * 
     * @param itemId The item ID to check for
     * @param quantity Minimum quantity (defaults to 1 if not provided)
     * @return true if container has enough of the item
     */
    public boolean contains(String itemId, Object quantity) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        
        int requiredQty = 1;
        if (quantity instanceof Number) {
            requiredQty = ((Number) quantity).intValue();
        }
        
        return getQuantity(itemId) >= requiredQty;
    }

    /**
     * Get total quantity of a specific item type.
     * 
     * @param itemId The item ID to count
     * @return Total quantity across all slots
     */
    public int getQuantity(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return 0;
        }
        
        int total = 0;
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty() && itemId.equals(stack.getItemId())) {
                total += stack.getQuantity();
            }
        }
        return total;
    }

    /**
     * Check if container has a specific item in any slot.
     * 
     * @param itemId The item ID to check for
     * @return true if at least one exists
     */
    public boolean has(String itemId) {
        return contains(itemId, 1);
    }

    // ===== Iteration Methods =====

    /**
     * Iterate over all non-empty slots.
     * 
     * @param callback JavaScript function (item, slot) => void
     */
    public void forEach(Object callback) {
        if (!(callback instanceof Function)) {
            throw new IllegalArgumentException("Callback must be a function");
        }
        
        Function callbackFn = (Function) callback;
        Context cx = Context.getCurrentContext();
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                callbackFn.call(cx, scope, scope, new Object[]{handle, (int) slot});
            }
        }
    }

    /**
     * Map over all non-empty slots.
     * 
     * @param callback JavaScript function (item, slot) => T
     * @return JavaScript array of results
     */
    public Object map(Object callback) {
        if (!(callback instanceof Function)) {
            throw new IllegalArgumentException("Callback must be a function");
        }
        
        Function callbackFn = (Function) callback;
        Context cx = Context.getCurrentContext();
        List<Object> results = new ArrayList<>();
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                Object result = callbackFn.call(cx, scope, scope, new Object[]{handle, (int) slot});
                results.add(result);
            }
        }
        
        return cx.newArray(scope, results.toArray());
    }

    /**
     * Filter slots by predicate.
     * 
     * @param predicate JavaScript function (item, slot) => boolean
     * @return JavaScript array of items that match
     */
    public Object filter(Object predicate) {
        if (!(predicate instanceof Function)) {
            throw new IllegalArgumentException("Predicate must be a function");
        }
        
        Function predicateFn = (Function) predicate;
        Context cx = Context.getCurrentContext();
        List<ItemStackHandle> results = new ArrayList<>();
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                Object result = predicateFn.call(cx, scope, scope, new Object[]{handle, (int) slot});
                
                if (result instanceof Boolean && (Boolean) result) {
                    results.add(handle);
                }
            }
        }
        
        return cx.newArray(scope, results.toArray());
    }

    /**
     * Get all items in the container.
     * 
     * @return JavaScript array of all non-empty items
     */
    public Object getAll() {
        Context cx = Context.getCurrentContext();
        List<ItemStackHandle> items = new ArrayList<>();
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                items.add(new ItemStackHandle(stack, scope));
            }
        }
        
        return cx.newArray(scope, items.toArray());
    }

    /**
     * Get all items as slot map.
     * 
     * @return JavaScript object mapping slot number to item
     */
    public Object getAllSlots() {
        Context cx = Context.getCurrentContext();
        Scriptable obj = cx.newObject(scope);
        
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                ItemStackHandle handle = new ItemStackHandle(stack, scope);
                obj.put(String.valueOf(slot), obj, handle);
            }
        }
        
        return obj;
    }

    // ===== Internal Access =====

    /**
     * Get the underlying Hytale ItemContainer.
     * For internal use by other bridge classes.
     * 
     * @return The wrapped ItemContainer
     */
    public ItemContainer getDelegate() {
        return delegate;
    }

    // ===== JavaScript Compatibility =====

    @Override
    public String toString() {
        int nonEmptySlots = 0;
        for (short slot = 0; slot < getCapacity(); slot++) {
            ItemStack stack = delegate.getItemStack(slot);
            if (stack != null && !stack.isEmpty()) {
                nonEmptySlots++;
            }
        }
        return "ItemContainer{capacity=" + getCapacity() + ", occupied=" + nonEmptySlots + "}";
    }
}
