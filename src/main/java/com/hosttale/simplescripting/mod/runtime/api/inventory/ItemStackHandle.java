package com.hosttale.simplescripting.mod.runtime.api.inventory;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * JavaScript-safe wrapper for Hytale's ItemStack class.
 * 
 * <p>ItemStack represents a stack of items with quantity, durability, and metadata.
 * Most modification methods return NEW instances - the original stack is never modified.</p>
 * 
 * <h3>Key Features</h3>
 * <ul>
 *   <li><strong>Immutable</strong>: Most methods return new ItemStackHandle instances</li>
 *   <li><strong>Metadata</strong>: Full BSON metadata support with JavaScript object conversion</li>
 *   <li><strong>Durability</strong>: Track and modify item durability</li>
 *   <li><strong>Comparison</strong>: Check if stacks can be combined or are equivalent</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // JavaScript
 * const stack = inventory.createStack('Stone', 64);
 * console.info('Item:', stack.itemId, 'Quantity:', stack.quantity);
 * 
 * // Add metadata
 * const withMeta = stack.withMetadata({ owner: 'Steve', enchanted: true });
 * 
 * // Modify durability
 * const damaged = stack.damage(10);
 * console.info('Durability:', damaged.durability, '/', damaged.maxDurability);
 * }</pre>
 */
public final class ItemStackHandle {

    private final ItemStack delegate;
    private final Scriptable scope;

    /**
     * Create a new ItemStackHandle wrapping a Hytale ItemStack.
     * 
     * @param delegate The ItemStack to wrap
     * @param scope The JavaScript scope for metadata conversion
     * @throws IllegalArgumentException if delegate is null
     */
    public ItemStackHandle(ItemStack delegate, Scriptable scope) {
        if (delegate == null) {
            throw new IllegalArgumentException("ItemStack cannot be null");
        }
        this.delegate = delegate;
        this.scope = scope;
    }

    // ===== Property Getters =====

    /**
     * Get the item ID (e.g., "Stone", "DiamondSword").
     * 
     * @return The item ID string
     */
    public String getItemId() {
        return delegate.getItemId();
    }

    /**
     * Get the quantity in this stack.
     * 
     * @return The stack quantity (1-64 typically)
     */
    public int getQuantity() {
        return delegate.getQuantity();
    }

    /**
     * Get the current durability.
     * 0 means broken, maxDurability means full health.
     * 
     * @return The current durability value
     */
    public double getDurability() {
        return delegate.getDurability();
    }

    /**
     * Get the maximum durability for this item.
     * Returns 0 if the item is unbreakable.
     * 
     * @return The maximum durability value
     */
    public double getMaxDurability() {
        return delegate.getMaxDurability();
    }

    /**
     * Check if this stack is broken (durability == 0 and item has durability system).
     * 
     * @return true if broken, false otherwise
     */
    public boolean isBroken() {
        return delegate.isBroken();
    }

    /**
     * Check if this item is unbreakable (maxDurability <= 0).
     * 
     * @return true if unbreakable, false otherwise
     */
    public boolean isUnbreakable() {
        return delegate.isUnbreakable();
    }

    /**
     * Check if this is an empty stack.
     * 
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * Check if this stack references a valid item definition.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return delegate.isValid();
    }

    /**
     * Get the block key if this item places a block.
     * 
     * @return The block key, or null if this item doesn't place a block
     */
    public String getBlockKey() {
        String key = delegate.getBlockKey();
        return "Empty".equals(key) ? null : key;
    }

    /**
     * Get the underlying ItemStack delegate (internal use only).
     * 
     * @return The wrapped ItemStack
     */
    public ItemStack getDelegate() {
        return delegate;
    }

    // ===== Metadata Access =====

    /**
     * Get metadata as a JavaScript object.
     * Returns null if no metadata exists.
     * 
     * @return JavaScript object with metadata, or null
     */
    @SuppressWarnings("deprecation") // getMetadata() is deprecated but no alternative exists for full metadata access
    public Object getMetadata() {
        BsonDocument metadata = delegate.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return BsonConverter.toScriptable(metadata, scope);
    }

    /**
     * Get a specific metadata value by key.
     * Returns null if the key doesn't exist.
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    @SuppressWarnings("deprecation") // getMetadata() is deprecated but no alternative exists for full metadata access
    public Object getMetadataValue(String key) {
        BsonDocument metadata = delegate.getMetadata();
        return BsonConverter.getMetadataValue(metadata, key, scope);
    }

    /**
     * Check if this stack has metadata with a specific key.
     * 
     * @param key The metadata key to check
     * @return true if the key exists, false otherwise
     */
    @SuppressWarnings("deprecation") // getMetadata() is deprecated but no alternative exists for full metadata access
    public boolean hasMetadata(String key) {
        return BsonConverter.hasMetadata(delegate.getMetadata(), key);
    }

    /**
     * Check if this stack has any metadata.
     * 
     * @return true if metadata exists, false otherwise
     */
    @SuppressWarnings("deprecation") // getMetadata() is deprecated but no alternative exists for full metadata access
    public boolean hasMetadata() {
        BsonDocument metadata = delegate.getMetadata();
        return metadata != null && !metadata.isEmpty();
    }

    // ===== Modification Methods (Return New Instances) =====

    /**
     * Create a new stack with a different quantity.
     * Returns null if quantity is 0.
     * 
     * @param quantity The new quantity (must be > 0)
     * @return New ItemStackHandle with updated quantity, or null if quantity is 0
     * @throws IllegalArgumentException if quantity is negative
     */
    public ItemStackHandle withQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (quantity == 0) {
            return null; // Hytale returns null for 0 quantity
        }
        
        ItemStack newStack = delegate.withQuantity(quantity);
        return newStack == null ? null : new ItemStackHandle(newStack, scope);
    }

    /**
     * Create a new stack with updated durability.
     * 
     * @param durability The new durability value
     * @return New ItemStackHandle with updated durability
     */
    public ItemStackHandle withDurability(double durability) {
        ItemStack newStack = delegate.withDurability(durability);
        return new ItemStackHandle(newStack, scope);
    }

    /**
     * Create a new stack with durability increased by delta.
     * Positive delta heals, negative delta damages.
     * 
     * @param delta The durability change
     * @return New ItemStackHandle with updated durability
     */
    public ItemStackHandle withIncreasedDurability(double delta) {
        ItemStack newStack = delegate.withIncreasedDurability(delta);
        return new ItemStackHandle(newStack, scope);
    }

    /**
     * Create a new stack with updated maximum durability.
     * 
     * @param maxDurability The new maximum durability
     * @return New ItemStackHandle with updated max durability
     */
    public ItemStackHandle withMaxDurability(double maxDurability) {
        ItemStack newStack = delegate.withMaxDurability(maxDurability);
        return new ItemStackHandle(newStack, scope);
    }

    /**
     * Create a new stack with fully restored durability.
     * Both current and max durability are set to the provided value.
     * 
     * @param durability The durability value for both current and max
     * @return New ItemStackHandle with restored durability
     */
    public ItemStackHandle withRestoredDurability(double durability) {
        ItemStack newStack = delegate.withRestoredDurability(durability);
        return new ItemStackHandle(newStack, scope);
    }

    /**
     * Create a new stack with completely replaced metadata.
     * 
     * @param metadata JavaScript object with new metadata
     * @return New ItemStackHandle with updated metadata
     */
    public ItemStackHandle withMetadata(Object metadata) {
        BsonDocument bsonDoc = BsonConverter.toBson(metadata);
        ItemStack newStack = delegate.withMetadata(bsonDoc);
        return new ItemStackHandle(newStack, scope);
    }

    /**
     * Create a new stack with a single metadata key-value pair updated.
     * Other metadata is preserved.
     * 
     * @param key The metadata key
     * @param value The JavaScript value
     * @return New ItemStackHandle with updated metadata
     */
    @SuppressWarnings("deprecation") // getMetadata() is deprecated but no alternative exists for full metadata access
    public ItemStackHandle withMetadata(String key, Object value) {
        BsonDocument currentMeta = delegate.getMetadata();
        BsonDocument newMeta = BsonConverter.withMetadata(currentMeta, key, value);
        ItemStack newStack = delegate.withMetadata(newMeta);
        return new ItemStackHandle(newStack, scope);
    }

    // ===== Convenience Methods =====

    /**
     * Damage this stack by reducing durability.
     * 
     * @param amount The damage amount (positive number)
     * @return New ItemStackHandle with reduced durability
     */
    public ItemStackHandle damage(double amount) {
        return withIncreasedDurability(-Math.abs(amount));
    }

    /**
     * Repair this stack by increasing durability.
     * 
     * @param amount The repair amount (positive number)
     * @return New ItemStackHandle with increased durability
     */
    public ItemStackHandle repair(double amount) {
        return withIncreasedDurability(Math.abs(amount));
    }

    /**
     * Fully repair this stack to max durability.
     * 
     * @return New ItemStackHandle with full durability
     */
    public ItemStackHandle fullyRepair() {
        return withDurability(delegate.getMaxDurability());
    }

    // ===== Comparison Methods =====

    /**
     * Check if this stack can be combined with another stack.
     * Stacks are stackable if they have the same item type and metadata.
     * 
     * @param other The other stack to compare
     * @return true if stackable, false otherwise
     */
    public boolean isStackableWith(ItemStackHandle other) {
        if (other == null) {
            return false;
        }
        return delegate.isStackableWith(other.delegate);
    }

    /**
     * Check if this stack has the same item type as another stack.
     * Ignores metadata and durability.
     * 
     * @param other The other stack to compare
     * @return true if same type, false otherwise
     */
    public boolean isSameItemType(ItemStackHandle other) {
        if (other == null) {
            return false;
        }
        return ItemStack.isSameItemType(delegate, other.delegate);
    }

    /**
     * Check if this stack is equivalent to another stack.
     * Considers item type but may have different comparison logic than isStackableWith.
     * 
     * @param other The other stack to compare
     * @return true if equivalent, false otherwise
     */
    public boolean isEquivalentType(ItemStackHandle other) {
        if (other == null) {
            return false;
        }
        return delegate.isEquivalentType(other.delegate);
    }

    // ===== Serialization =====

    /**
     * Convert this stack to a plain JavaScript object for serialization.
     * 
     * @return JavaScript object with all stack properties
     */
    public Object toObject() {
        var obj = Context.getCurrentContext().newObject(scope);
        obj.put("itemId", obj, delegate.getItemId());
        obj.put("quantity", obj, delegate.getQuantity());
        obj.put("durability", obj, delegate.getDurability());
        obj.put("maxDurability", obj, delegate.getMaxDurability());
        obj.put("broken", obj, delegate.isBroken());
        obj.put("unbreakable", obj, delegate.isUnbreakable());
        obj.put("empty", obj, delegate.isEmpty());
        obj.put("valid", obj, delegate.isValid());
        
        String blockKey = getBlockKey();
        if (blockKey != null) {
            obj.put("blockKey", obj, blockKey);
        }
        
        Object metadata = getMetadata();
        if (metadata != null) {
            obj.put("metadata", obj, metadata);
        }
        
        return obj;
    }

    @Override
    public String toString() {
        return String.format("ItemStack{itemId=%s, quantity=%d, durability=%.1f/%.1f%s}",
                delegate.getItemId(),
                delegate.getQuantity(),
                delegate.getDurability(),
                delegate.getMaxDurability(),
                hasMetadata() ? ", metadata={...}" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemStackHandle other)) return false;
        return delegate.equals(other.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
