package com.hosttale.simplescripting.mod.runtime.api.inventory;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript-safe wrapper for Hytale's inventory transaction results.
 * 
 * <p>Transaction results indicate the success/failure of inventory operations
 * and provide details about the operation (items moved, remainders, etc.).</p>
 * 
 * <h3>Key Features</h3>
 * <ul>
 *   <li><strong>Success Status</strong>: Check if operation succeeded</li>
 *   <li><strong>Remainder</strong>: Get items that didn't fit (for add operations)</li>
 *   <li><strong>Moved Items</strong>: Get items that were transferred</li>
 *   <li><strong>Slot Info</strong>: Get before/after slot states</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // JavaScript
 * const stack = inventory.createStack('Stone', 64);
 * const result = container.addItem(stack);
 * 
 * if (result.success) {
 *   console.info('Added successfully');
 *   if (result.remainder) {
 *     console.info('Could not fit:', result.remainder.quantity);
 *   }
 * } else {
 *   console.error('Failed to add:', result.message);
 * }
 * }</pre>
 */
public final class TransactionResultHandle {

    private final boolean success;
    private final String message;
    private final ItemStackHandle remainder;
    private final ItemStackHandle slotBefore;
    private final ItemStackHandle slotAfter;
    private final Integer slot;

    private TransactionResultHandle(boolean success, String message, 
                                   ItemStackHandle remainder, 
                                   ItemStackHandle slotBefore,
                                   ItemStackHandle slotAfter,
                                   Integer slot) {
        this.success = success;
        this.message = message;
        this.remainder = remainder;
        this.slotBefore = slotBefore;
        this.slotAfter = slotAfter;
        this.slot = slot;
    }

    // ===== Properties =====

    /**
     * Check if the transaction was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get error/status message.
     * Only present if transaction failed or has warnings.
     * 
     * @return Status message, or null if none
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get remainder item (for add operations).
     * Present if item didn't fully fit in container.
     * 
     * @return The remainder item, or null if all items fit
     */
    public ItemStackHandle getRemainder() {
        return remainder;
    }

    /**
     * Get the slot state before the operation.
     * Only present for slot-specific operations.
     * 
     * @return Item before operation, or null if slot was empty
     */
    public ItemStackHandle getSlotBefore() {
        return slotBefore;
    }

    /**
     * Get the slot state after the operation.
     * Only present for slot-specific operations.
     * 
     * @return Item after operation, or null if slot is now empty
     */
    public ItemStackHandle getSlotAfter() {
        return slotAfter;
    }

    /**
     * Get the slot index involved in this transaction.
     * Only present for slot-specific operations.
     * 
     * @return Slot number, or null if not applicable
     */
    public Integer getSlot() {
        return slot;
    }

    // ===== Factory Methods =====

    /**
     * Create a success result with no additional data.
     * 
     * @return Success result
     */
    public static TransactionResultHandle success() {
        return new TransactionResultHandle(true, null, null, null, null, null);
    }

    /**
     * Create a success result with message.
     * 
     * @param message Status message
     * @return Success result with message
     */
    public static TransactionResultHandle success(String message) {
        return new TransactionResultHandle(true, message, null, null, null, null);
    }

    /**
     * Create a failure result with error message.
     * 
     * @param message Error message
     * @return Failure result
     */
    public static TransactionResultHandle failure(String message) {
        return new TransactionResultHandle(false, message, null, null, null, null);
    }

    /**
     * Create result from ItemStackTransaction.
     * 
     * @param tx The transaction
     * @param scope JavaScript scope for ItemStack conversion
     * @return Transaction result
     */
    public static TransactionResultHandle fromItemStackTransaction(ItemStackTransaction tx, Scriptable scope) {
        if (tx == null) {
            return failure("Transaction is null");
        }

        boolean success = tx.succeeded();
        ItemStackHandle remainder = null;

        ItemStack remainderStack = tx.getRemainder();
        if (remainderStack != null && !remainderStack.isEmpty()) {
            remainder = new ItemStackHandle(remainderStack, scope);
        }

        String message = success ? null : "Transaction failed";
        return new TransactionResultHandle(success, message, remainder, null, null, null);
    }

    /**
     * Create result from SlotTransaction.
     * 
     * @param tx The transaction
     * @param scope JavaScript scope for ItemStack conversion
     * @return Transaction result
     */
    public static TransactionResultHandle fromSlotTransaction(SlotTransaction tx, Scriptable scope) {
        if (tx == null) {
            return failure("Transaction is null");
        }

        boolean success = tx.succeeded();
        ItemStackHandle before = null;
        ItemStackHandle after = null;
        Integer slot = null;

        ItemStack beforeStack = tx.getSlotBefore();
        if (beforeStack != null && !beforeStack.isEmpty()) {
            before = new ItemStackHandle(beforeStack, scope);
        }

        ItemStack afterStack = tx.getSlotAfter();
        if (afterStack != null && !afterStack.isEmpty()) {
            after = new ItemStackHandle(afterStack, scope);
        }

        if (tx instanceof ItemStackSlotTransaction slotTx) {
            slot = (int) slotTx.getSlot();
        }

        String message = success ? null : "Transaction failed";
        return new TransactionResultHandle(success, message, null, before, after, slot);
    }

    /**
     * Create result from ItemStackSlotTransaction.
     * 
     * @param tx The transaction
     * @param scope JavaScript scope for ItemStack conversion
     * @return Transaction result
     */
    public static TransactionResultHandle fromItemStackSlotTransaction(ItemStackSlotTransaction tx, Scriptable scope) {
        if (tx == null) {
            return failure("Transaction is null");
        }

        boolean success = tx.succeeded();
        ItemStackHandle remainder = null;
        ItemStackHandle before = null;
        ItemStackHandle after = null;
        Integer slot = (int) tx.getSlot();

        ItemStack remainderStack = tx.getRemainder();
        if (remainderStack != null && !remainderStack.isEmpty()) {
            remainder = new ItemStackHandle(remainderStack, scope);
        }

        ItemStack beforeStack = tx.getSlotBefore();
        if (beforeStack != null && !beforeStack.isEmpty()) {
            before = new ItemStackHandle(beforeStack, scope);
        }

        ItemStack afterStack = tx.getSlotAfter();
        if (afterStack != null && !afterStack.isEmpty()) {
            after = new ItemStackHandle(afterStack, scope);
        }

        String message = success ? null : "Transaction failed";
        return new TransactionResultHandle(success, message, remainder, before, after, slot);
    }

    /**
     * Create result from ClearTransaction.
     * 
     * @param tx The transaction
     * @param scope JavaScript scope
     * @return Transaction result
     */
    public static TransactionResultHandle fromClearTransaction(ClearTransaction tx, Scriptable scope) {
        if (tx == null) {
            return failure("Transaction is null");
        }

        boolean success = tx.succeeded();
        String message = success ? "Container cleared" : "Failed to clear container";
        return new TransactionResultHandle(success, message, null, null, null, null);
    }

    /**
     * Create JavaScript array of results from ListTransaction.
     * 
     * @param listTx The list transaction
     * @param scope JavaScript scope
     * @return JavaScript array of TransactionResultHandle objects
     */
    public static Object fromListTransaction(ListTransaction<?> listTx, Scriptable scope) {
        if (listTx == null) {
            return Context.getCurrentContext().newArray(scope, 0);
        }

        List<TransactionResultHandle> results = new ArrayList<>();
        
        for (Object tx : listTx.getList()) {
            if (tx instanceof ItemStackTransaction itemStackTx) {
                results.add(fromItemStackTransaction(itemStackTx, scope));
            } else if (tx instanceof ItemStackSlotTransaction slotTx) {
                results.add(fromItemStackSlotTransaction(slotTx, scope));
            } else if (tx instanceof SlotTransaction slotTx) {
                results.add(fromSlotTransaction(slotTx, scope));
            } else {
                // Generic fallback
                results.add(new TransactionResultHandle(true, null, null, null, null, null));
            }
        }

        return Context.getCurrentContext().newArray(scope, results.toArray());
    }

    // ===== JavaScript Compatibility =====

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransactionResult{");
        sb.append("success=").append(success);
        if (message != null) {
            sb.append(", message='").append(message).append("'");
        }
        if (remainder != null) {
            sb.append(", remainder=").append(remainder);
        }
        if (slot != null) {
            sb.append(", slot=").append(slot);
        }
        sb.append("}");
        return sb.toString();
    }
}
