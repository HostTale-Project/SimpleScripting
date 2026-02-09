package com.hosttale.simplescripting.mod.runtime.api.inventory;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;
import org.mozilla.javascript.Scriptable;

/**
 * JavaScript API for inventory operations.
 * Provides factory methods for creating ItemStack instances and utility functions.
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // JavaScript - exposed as global 'inventory' object
 * const stack = inventory.createStack('Stone', 64);
 * const sword = inventory.createStack('DiamondSword');
 * const custom = inventory.createStack({
 *   itemId: 'IronPickaxe',
 *   quantity: 1,
 *   durability: 50,
 *   maxDurability: 250,
 *   metadata: { enchanted: true, level: 3 }
 * });
 * }</pre>
 */
public final class InventoryApi {

    private final Scriptable scope;

    /**
     * Create a new InventoryApi instance.
     * 
     * @param scope The JavaScript scope for object creation
     */
    public InventoryApi(Scriptable scope) {
        this.scope = scope;
    }

    // ===== Factory Methods =====

    /**
     * Create an ItemStack with the specified item ID and quantity 1.
     * 
     * @param itemId The item ID (e.g., "Stone", "DiamondSword")
     * @return New ItemStackHandle
     * @throws IllegalArgumentException if itemId is null, empty, or "Empty"
     */
    public ItemStackHandle createStack(String itemId) {
        return createStack(itemId, 1);
    }

    /**
     * Create an ItemStack with the specified item ID and quantity.
     * 
     * @param itemId The item ID
     * @param quantity The stack quantity (must be > 0)
     * @return New ItemStackHandle
     * @throws IllegalArgumentException if itemId is invalid or quantity <= 0
     */
    public ItemStackHandle createStack(String itemId, int quantity) {
        validateItemId(itemId);
        validateQuantity(quantity);
        
        ItemStack stack = new ItemStack(itemId, quantity);
        return new ItemStackHandle(stack, scope);
    }

    /**
     * Create an ItemStack from a JavaScript options object.
     * 
     * <p>Supported properties:</p>
     * <ul>
     *   <li><code>itemId</code> (string, required) - The item ID</li>
     *   <li><code>quantity</code> (number, default: 1) - Stack quantity</li>
     *   <li><code>durability</code> (number, optional) - Current durability</li>
     *   <li><code>maxDurability</code> (number, optional) - Maximum durability</li>
     *   <li><code>metadata</code> (object, optional) - BSON metadata</li>
     * </ul>
     * 
     * @param options JavaScript object with stack properties
     * @return New ItemStackHandle
     * @throws IllegalArgumentException if options is invalid or missing required fields
     */
    public ItemStackHandle createStack(Object options) {
        if (!(options instanceof Scriptable scriptable)) {
            throw new IllegalArgumentException("Options must be an object");
        }

        // Extract itemId (required)
        Object itemIdObj = scriptable.get("itemId", scriptable);
        if (itemIdObj == Scriptable.NOT_FOUND || !(itemIdObj instanceof String)) {
            throw new IllegalArgumentException("Options must have 'itemId' string property");
        }
        String itemId = (String) itemIdObj;
        validateItemId(itemId);

        // Extract quantity (optional, default 1)
        int quantity = 1;
        Object quantityObj = scriptable.get("quantity", scriptable);
        if (quantityObj != Scriptable.NOT_FOUND && quantityObj instanceof Number) {
            quantity = ((Number) quantityObj).intValue();
            validateQuantity(quantity);
        }

        // Extract durability values (optional)
        Double durability = null;
        Object durabilityObj = scriptable.get("durability", scriptable);
        if (durabilityObj != Scriptable.NOT_FOUND && durabilityObj instanceof Number) {
            durability = ((Number) durabilityObj).doubleValue();
        }

        Double maxDurability = null;
        Object maxDurabilityObj = scriptable.get("maxDurability", scriptable);
        if (maxDurabilityObj != Scriptable.NOT_FOUND && maxDurabilityObj instanceof Number) {
            maxDurability = ((Number) maxDurabilityObj).doubleValue();
        }

        // Extract metadata (optional)
        BsonDocument metadata = null;
        Object metadataObj = scriptable.get("metadata", scriptable);
        if (metadataObj != Scriptable.NOT_FOUND && metadataObj != null) {
            metadata = BsonConverter.toBson(metadataObj);
        }

        // Create ItemStack with appropriate constructor
        ItemStack stack;
        if (durability != null && maxDurability != null && metadata != null) {
            // Full constructor
            stack = new ItemStack(itemId, quantity, durability, maxDurability, metadata);
        } else if (metadata != null) {
            // Constructor with metadata only
            stack = new ItemStack(itemId, quantity, metadata);
        } else {
            // Basic constructor, then apply durability if provided
            stack = new ItemStack(itemId, quantity);
            if (durability != null) {
                stack = stack.withDurability(durability);
            }
            if (maxDurability != null) {
                stack = stack.withMaxDurability(maxDurability);
            }
        }

        return new ItemStackHandle(stack, scope);
    }

    /**
     * Get the empty ItemStack singleton wrapped in a handle.
     * 
     * @return ItemStackHandle wrapping ItemStack.EMPTY
     */
    public ItemStackHandle emptyStack() {
        return new ItemStackHandle(ItemStack.EMPTY, scope);
    }

    // ===== Utility Methods =====

    /**
     * Check if a stack is empty or null.
     * 
     * @param stack The stack to check (may be null)
     * @return true if stack is null or empty, false otherwise
     */
    public boolean isEmpty(ItemStackHandle stack) {
        return ItemStack.isEmpty(stack == null ? null : stack.getDelegate());
    }

    /**
     * Check if two stacks can be combined (stackable).
     * 
     * @param a First stack (may be null)
     * @param b Second stack (may be null)
     * @return true if stacks are stackable, false otherwise
     */
    public boolean areStackable(ItemStackHandle a, ItemStackHandle b) {
        return ItemStack.isStackableWith(
                a == null ? null : a.getDelegate(),
                b == null ? null : b.getDelegate()
        );
    }

    /**
     * Check if two stacks have the same item type.
     * 
     * @param a First stack (may be null)
     * @param b Second stack (may be null)
     * @return true if stacks have same type, false otherwise
     */
    public boolean areSameType(ItemStackHandle a, ItemStackHandle b) {
        return ItemStack.isSameItemType(
                a == null ? null : a.getDelegate(),
                b == null ? null : b.getDelegate()
        );
    }

    // ===== Validation Methods =====

    /**
     * Validate an item ID.
     * 
     * @param itemId The item ID to validate
     * @throws IllegalArgumentException if itemId is invalid
     */
    private void validateItemId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if ("Empty".equals(itemId)) {
            throw new IllegalArgumentException("Item ID cannot be 'Empty' - use emptyStack() instead");
        }
    }

    /**
     * Validate a quantity value.
     * 
     * @param quantity The quantity to validate
     * @throws IllegalArgumentException if quantity is invalid
     */
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0 (got: " + quantity + ")");
        }
    }
}
