package com.hosttale.simplescripting.mod.runtime.api.entities;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityHandleTest {

    @Test
    void constructorThrowsOnNullRef() {
        assertThrows(IllegalArgumentException.class, () -> new TestEntityHandle(null));
    }

    @Test
    void getEntityRefReturnsProvidedRef() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new TestEntityHandle(mockRef);

        assertSame(mockRef, handle.getEntityRef());
    }

    @Test
    void isValidReturnsTrueWhenRefIsValid() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new TestEntityHandle(mockRef);

        assertTrue(handle.isValid());
    }

    @Test
    void isValidReturnsFalseWhenRefIsInvalid() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(false);

        var handle = new TestEntityHandle(mockRef);

        assertFalse(handle.isValid());
    }

    @Test
    void getEntityIdReturnsStringRepresentation() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);
        when(mockRef.toString()).thenReturn("Ref<EntityStore>#12345");

        var handle = new TestEntityHandle(mockRef);

        assertEquals("Ref<EntityStore>#12345", handle.getEntityId());
    }

    @Test
    void equalsReturnsTrueForSameRef() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);
        // Ensure the mock's equals method returns true when comparing to itself
        when(mockRef.equals(mockRef)).thenReturn(true);

        var handle1 = new TestEntityHandle(mockRef);
        var handle2 = new TestEntityHandle(mockRef);

        assertEquals(handle1, handle2);
    }

    @Test
    void equalsReturnsFalseForDifferentRefs() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef1 = mock(Ref.class);
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef2 = mock(Ref.class);
        when(mockRef1.isValid()).thenReturn(true);
        when(mockRef2.isValid()).thenReturn(true);

        var handle1 = new TestEntityHandle(mockRef1);
        var handle2 = new TestEntityHandle(mockRef2);

        assertNotEquals(handle1, handle2);
    }

    @Test
    void hashCodeIsConsistent() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);

        var handle = new TestEntityHandle(mockRef);

        assertEquals(handle.hashCode(), handle.hashCode());
    }

    @Test
    void toStringIncludesClassName() {
        @SuppressWarnings("unchecked")
        Ref<EntityStore> mockRef = mock(Ref.class);
        when(mockRef.isValid()).thenReturn(true);
        when(mockRef.toString()).thenReturn("Ref#123");

        var handle = new TestEntityHandle(mockRef);
        String toString = handle.toString();

        assertTrue(toString.contains("TestEntityHandle"));
        assertTrue(toString.contains("Ref#123"));
    }

    // Test concrete subclass for testing abstract EntityHandle
    private static class TestEntityHandle extends EntityHandle {
        public TestEntityHandle(Ref<EntityStore> entityRef) {
            super(entityRef);
        }
    }
}
