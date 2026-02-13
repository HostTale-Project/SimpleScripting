package com.hosttale.simplescripting.mod.runtime.api.inventory;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemContainerHandle.
 * Tests the JavaScript-safe wrapper for Hytale's ItemContainer class.
 */
class ItemContainerHandleTest {

    private Context context;
    private Scriptable scope;
    private ItemContainer mockContainer;
    private ItemContainerHandle handle;

    @BeforeEach
    void setUp() {
        context = Context.enter();
        context.setLanguageVersion(Context.VERSION_ES6);
        scope = context.initStandardObjects();
        
        mockContainer = mock(ItemContainer.class);
        handle = new ItemContainerHandle(mockContainer, scope);
    }

    // ===== Constructor Tests =====

    @Test
    void constructorThrowsOnNullContainer() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ItemContainerHandle(null, scope));
    }

    @Test
    void constructorAcceptsValidContainer() {
        assertDoesNotThrow(() -> new ItemContainerHandle(mockContainer, scope));
    }

    // ===== Property Tests =====

    @Test
    void getCapacityReturnsContainerCapacity() {
        when(mockContainer.getCapacity()).thenReturn((short) 40);
        
        assertEquals(40, handle.getCapacity());
        verify(mockContainer).getCapacity();
    }

    @Test
    void isEmptyReturnsTrueWhenContainerEmpty() {
        when(mockContainer.isEmpty()).thenReturn(true);
        
        assertTrue(handle.isEmpty());
        verify(mockContainer).isEmpty();
    }

    @Test
    void isEmptyReturnsFalseWhenContainerHasItems() {
        when(mockContainer.isEmpty()).thenReturn(false);
        
        assertFalse(handle.isEmpty());
        verify(mockContainer).isEmpty();
    }

    // ===== Slot Operation Tests =====

    @Test
    void getItemReturnsNullForEmptySlot() {
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        when(mockContainer.getItemStack(anyShort())).thenReturn(null);
        
        assertNull(handle.getItem(0));
        verify(mockContainer).getItemStack((short) 0);
    }

    @Test
    void getItemReturnsNullForInvalidSlot() {
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        assertNull(handle.getItem(-1));
        assertNull(handle.getItem(10));
        assertNull(handle.getItem(100));
    }

    @Test
    void getItemReturnsHandleForValidItem() {
        ItemStack mockStack = mock(ItemStack.class);
        when(mockStack.isEmpty()).thenReturn(false);
        when(mockStack.getItemId()).thenReturn("Stone");
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        when(mockContainer.getItemStack((short) 0)).thenReturn(mockStack);
        
        ItemStackHandle result = handle.getItem(0);
        
        assertNotNull(result);
        assertEquals("Stone", result.getItemId());
    }

    @Test
    void setItemSetsSlotToNull() {
        ItemStackSlotTransaction mockTx = mock(ItemStackSlotTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.setItemStackForSlot(anyShort(), any())).thenReturn(mockTx);
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.setItem(0, null);
        
        assertTrue(result.isSuccess());
        verify(mockContainer).setItemStackForSlot((short) 0, null);
    }

    @Test
    void setItemRejectsInvalidSlot() {
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.setItem(-1, null);
        
        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    void addToSlotAddsItemToSlot() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemStackHandle itemHandle = new ItemStackHandle(mockStack, scope);
        ItemStackSlotTransaction mockTx = mock(ItemStackSlotTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.addItemStackToSlot(anyShort(), any())).thenReturn(mockTx);
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.addToSlot(0, itemHandle);
        
        assertTrue(result.isSuccess());
        verify(mockContainer).addItemStackToSlot((short) 0, mockStack);
    }

    @Test
    void addToSlotRejectsNullItem() {
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.addToSlot(0, null);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("cannot be null"));
    }

    @Test
    void removeFromSlotRemovesItem() {
        ItemStack mockStack = mock(ItemStack.class);
        when(mockStack.isEmpty()).thenReturn(false);
        when(mockStack.getItemId()).thenReturn("Stone");
        
        SlotTransaction mockTx = mock(SlotTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockTx.getSlotBefore()).thenReturn(mockStack);
        when(mockTx.getSlotAfter()).thenReturn(null);  // Add this
        when(mockContainer.removeItemStackFromSlot(anyShort())).thenReturn(mockTx);
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        ItemStackHandle result = handle.removeFromSlot(0);
        
        assertNotNull(result);
        assertEquals("Stone", result.getItemId());
        verify(mockContainer).removeItemStackFromSlot((short) 0);
    }

    @Test
    void removeFromSlotWithQuantity() {
        ItemStackSlotTransaction mockTx = mock(ItemStackSlotTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.removeItemStackFromSlot(anyShort(), anyInt())).thenReturn(mockTx);
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.removeFromSlot(0, 5);
        
        assertTrue(result.isSuccess());
        verify(mockContainer).removeItemStackFromSlot((short) 0, 5);
    }

    @Test
    void removeFromSlotRejectsInvalidQuantity() {
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.removeFromSlot(0, 0);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("positive"));
    }

    @Test
    void clearSlotClearsSlot() {
        ItemStackSlotTransaction mockTx = mock(ItemStackSlotTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.setItemStackForSlot(anyShort(), any())).thenReturn(mockTx);
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        
        TransactionResultHandle result = handle.clearSlot(0);
        
        assertTrue(result.isSuccess());
        verify(mockContainer).setItemStackForSlot((short) 0, null);
    }

    // ===== Container-Wide Operation Tests =====

    @Test
    void addItemAddsToContainer() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemStackHandle itemHandle = new ItemStackHandle(mockStack, scope);
        ItemStackTransaction mockTx = mock(ItemStackTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.addItemStack(any())).thenReturn(mockTx);
        
        TransactionResultHandle result = handle.addItem(itemHandle);
        
        assertTrue(result.isSuccess());
        verify(mockContainer).addItemStack(mockStack);
    }

    @Test
    void addItemRejectsNullItem() {
        TransactionResultHandle result = handle.addItem(null);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("cannot be null"));
    }

    @Test
    void addItemsAddsMultipleItems() {
        ItemStack mockStack1 = mock(ItemStack.class);
        ItemStack mockStack2 = mock(ItemStack.class);
        ItemStackHandle handle1 = new ItemStackHandle(mockStack1, scope);
        ItemStackHandle handle2 = new ItemStackHandle(mockStack2, scope);
        
        NativeArray jsArray = new NativeArray(new Object[]{handle1, handle2});
        
        List<ItemStackTransaction> transactions = new ArrayList<>();
        ItemStackTransaction tx1 = mock(ItemStackTransaction.class);
        ItemStackTransaction tx2 = mock(ItemStackTransaction.class);
        when(tx1.succeeded()).thenReturn(true);
        when(tx2.succeeded()).thenReturn(true);
        transactions.add(tx1);
        transactions.add(tx2);
        
        ListTransaction<ItemStackTransaction> mockListTx = mock(ListTransaction.class);
        when(mockListTx.getList()).thenReturn(transactions);
        when(mockContainer.addItemStacks(any())).thenReturn(mockListTx);
        
        Object result = handle.addItems(jsArray);
        
        assertInstanceOf(NativeArray.class, result);
        verify(mockContainer).addItemStacks(any());
    }

    @Test
    void canAddItemsChecksCapacity() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemStackHandle handle1 = new ItemStackHandle(mockStack, scope);
        NativeArray jsArray = new NativeArray(new Object[]{handle1});
        
        when(mockContainer.canAddItemStacks(any())).thenReturn(true);
        
        boolean result = handle.canAddItems(jsArray);
        
        assertTrue(result);
        verify(mockContainer).canAddItemStacks(any());
    }

    @Test
    void removeItemRemovesFromContainer() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemStackHandle itemHandle = new ItemStackHandle(mockStack, scope);
        ItemStackTransaction mockTx = mock(ItemStackTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.removeItemStack(any())).thenReturn(mockTx);
        
        TransactionResultHandle result = handle.removeItem(itemHandle);
        
        assertTrue(result.isSuccess());
        verify(mockContainer).removeItemStack(mockStack);
    }

    @Test
    void canRemoveItemChecksRemovability() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemStackHandle itemHandle = new ItemStackHandle(mockStack, scope);
        when(mockContainer.canRemoveItemStack(any())).thenReturn(true);
        
        boolean result = handle.canRemoveItem(itemHandle);
        
        assertTrue(result);
        verify(mockContainer).canRemoveItemStack(mockStack);
    }

    @Test
    void clearClearsContainer() {
        ClearTransaction mockTx = mock(ClearTransaction.class);
        when(mockTx.succeeded()).thenReturn(true);
        when(mockContainer.clear()).thenReturn(mockTx);
        
        TransactionResultHandle result = handle.clear();
        
        assertTrue(result.isSuccess());
        verify(mockContainer).clear();
    }

    // ===== Search and Query Tests =====

    @Test
    void countRejectsNonFunctionPredicate() {
        assertThrows(IllegalArgumentException.class, () -> handle.count("not a function"));
    }

    @Test
    void findSlotRejectsNonFunctionPredicate() {
        int result = handle.findSlot("not a function");
        assertEquals(-1, result);
    }

    @Test
    void findSlotsRejectsNonFunctionPredicate() {
        Object result = handle.findSlots("not a function");
        assertInstanceOf(NativeArray.class, result);
        assertEquals(0, ((NativeArray) result).getLength());
    }

    @Test
    void containsStackableChecksForStackableItems() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemStackHandle itemHandle = new ItemStackHandle(mockStack, scope);
        when(mockContainer.containsItemStacksStackableWith(any())).thenReturn(true);
        
        boolean result = handle.containsStackable(itemHandle);
        
        assertTrue(result);
        verify(mockContainer).containsItemStacksStackableWith(mockStack);
    }

    @Test
    void containsChecksByItemId() {
        ItemStack stone = mock(ItemStack.class);
        when(stone.isEmpty()).thenReturn(false);
        when(stone.getItemId()).thenReturn("Stone");
        when(stone.getQuantity()).thenReturn(10);
        
        when(mockContainer.getCapacity()).thenReturn((short) 1);
        when(mockContainer.getItemStack((short) 0)).thenReturn(stone);
        
        assertTrue(handle.contains("Stone", 5));
        assertTrue(handle.contains("Stone", 10));
        assertFalse(handle.contains("Stone", 15));
        assertFalse(handle.contains("Wood", 1));
    }

    @Test
    void getQuantityCountsTotalQuantity() {
        ItemStack stone1 = mock(ItemStack.class);
        when(stone1.isEmpty()).thenReturn(false);
        when(stone1.getItemId()).thenReturn("Stone");
        when(stone1.getQuantity()).thenReturn(32);
        
        ItemStack stone2 = mock(ItemStack.class);
        when(stone2.isEmpty()).thenReturn(false);
        when(stone2.getItemId()).thenReturn("Stone");
        when(stone2.getQuantity()).thenReturn(20);
        
        when(mockContainer.getCapacity()).thenReturn((short) 2);
        when(mockContainer.getItemStack((short) 0)).thenReturn(stone1);
        when(mockContainer.getItemStack((short) 1)).thenReturn(stone2);
        
        assertEquals(52, handle.getQuantity("Stone"));
        assertEquals(0, handle.getQuantity("Wood"));
    }

    @Test
    void hasChecksForItemExistence() {
        ItemStack stone = mock(ItemStack.class);
        when(stone.isEmpty()).thenReturn(false);
        when(stone.getItemId()).thenReturn("Stone");
        when(stone.getQuantity()).thenReturn(1);
        
        when(mockContainer.getCapacity()).thenReturn((short) 1);
        when(mockContainer.getItemStack((short) 0)).thenReturn(stone);
        
        assertTrue(handle.has("Stone"));
        assertFalse(handle.has("Wood"));
    }

    // ===== Iteration Tests =====

    @Test
    void forEachRejectsNonFunctionCallback() {
        assertThrows(IllegalArgumentException.class, () -> handle.forEach("not a function"));
    }

    @Test
    void mapRejectsNonFunctionCallback() {
        assertThrows(IllegalArgumentException.class, () -> handle.map("not a function"));
    }

    @Test
    void filterRejectsNonFunctionPredicate() {
        assertThrows(IllegalArgumentException.class, () -> handle.filter("not a function"));
    }

    @Test
    void getAllReturnsAllNonEmptyItems() {
        ItemStack stone = mock(ItemStack.class);
        when(stone.isEmpty()).thenReturn(false);
        when(stone.getItemId()).thenReturn("Stone");
        
        when(mockContainer.getCapacity()).thenReturn((short) 2);
        when(mockContainer.getItemStack((short) 0)).thenReturn(stone);
        when(mockContainer.getItemStack((short) 1)).thenReturn(null);
        
        Object result = handle.getAll();
        
        assertInstanceOf(NativeArray.class, result);
        NativeArray items = (NativeArray) result;
        assertEquals(1, items.getLength());
    }

    @Test
    void getAllSlotsReturnsSlotMap() {
        ItemStack stone = mock(ItemStack.class);
        when(stone.isEmpty()).thenReturn(false);
        when(stone.getItemId()).thenReturn("Stone");
        
        when(mockContainer.getCapacity()).thenReturn((short) 2);
        when(mockContainer.getItemStack((short) 0)).thenReturn(stone);
        when(mockContainer.getItemStack((short) 1)).thenReturn(null);
        
        Object result = handle.getAllSlots();
        
        assertInstanceOf(Scriptable.class, result);
        Scriptable slotMap = (Scriptable) result;
        assertNotNull(slotMap.get("0", slotMap));
    }

    // ===== toString Test =====

    @Test
    void toStringShowsContainerInfo() {
        when(mockContainer.getCapacity()).thenReturn((short) 10);
        when(mockContainer.getItemStack(anyShort())).thenReturn(null);
        
        String result = handle.toString();
        
        assertTrue(result.contains("ItemContainer"));
        assertTrue(result.contains("capacity=10"));
    }
}
