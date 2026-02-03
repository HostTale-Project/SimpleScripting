package com.hosttale.simplescripting.mod.runtime.api.entities;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LivingEntityHandleTest {

    @Test
    void constructorAcceptsValidRef() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        assertDoesNotThrow(() -> new LivingEntityHandle(mockRef));
    }

    @Test
    void getInventoryThrowsUnsupportedOperationException() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new LivingEntityHandle(mockRef);

        var exception = assertThrows(UnsupportedOperationException.class, handle::getInventory);
        assertTrue(exception.getMessage().contains("Phase 1"));
    }

    @Test
    void hasInventoryReturnsFalseInPhase0() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new LivingEntityHandle(mockRef);

        assertFalse(handle.hasInventory());
    }

    @Test
    void inheritsEntityHandleMethods() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new LivingEntityHandle(mockRef);

        // Should have all EntityHandle methods
        assertNotNull(handle.getEntityRef());
        assertTrue(handle.isValid());
        assertNotNull(handle.getEntityId());
    }

    @Test
    void getLivingEntityThrowsUnsupportedOperationException() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new LivingEntityHandle(mockRef);

        assertThrows(UnsupportedOperationException.class, handle::getLivingEntity);
    }
}
