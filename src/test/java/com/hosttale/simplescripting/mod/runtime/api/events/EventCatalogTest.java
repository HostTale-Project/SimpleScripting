package com.hosttale.simplescripting.mod.runtime.api.events;

import com.hypixel.hytale.logger.HytaleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for EventCatalog to verify all events can be resolved.
 */
public class EventCatalogTest {

    private EventCatalog catalog;
    private HytaleLogger mockLogger;

    @BeforeEach
    public void setUp() {
        mockLogger = mock(HytaleLogger.class);
        when(mockLogger.getSubLogger(anyString())).thenReturn(mockLogger);
        catalog = new EventCatalog(mockLogger);
    }

    @Test
    public void testServerLifecycleEventsResolve() {
        assertNotNull(catalog.resolve("boot"));
        assertNotNull(catalog.resolve("shutdown"));
        assertNotNull(catalog.resolve("prepareuniverse"));
    }

    @Test
    public void testInventoryEventsResolve() {
        // Priority 1: Critical inventory events
        assertNotNull(catalog.resolve("livingentityinventorychange"));
        assertNotNull(catalog.resolve("dropitem"));
        assertNotNull(catalog.resolve("interactivelypickupitem"));
        assertNotNull(catalog.resolve("switchactiveslot"));
        assertNotNull(catalog.resolve("craftrecipe"));
        assertNotNull(catalog.resolve("playercraft"));
    }

    @Test
    public void testEntityEventsResolve() {
        // Priority 2: Entity events
        assertNotNull(catalog.resolve("entity"));
        assertNotNull(catalog.resolve("entityremove"));
        assertNotNull(catalog.resolve("livingentityuseblock"));
    }

    @Test
    public void testPlayerLifecycleEventsResolve() {
        // Priority 3: Player lifecycle
        assertNotNull(catalog.resolve("playerconnect"));
        assertNotNull(catalog.resolve("playerdisconnect"));
        assertNotNull(catalog.resolve("playerready"));
        assertNotNull(catalog.resolve("playersetupconnect"));
        assertNotNull(catalog.resolve("playersetupdisconnect"));
        assertNotNull(catalog.resolve("addplayertoworld"));
        assertNotNull(catalog.resolve("drainplayerfromworld"));
        assertNotNull(catalog.resolve("player"));
        assertNotNull(catalog.resolve("playerref"));
    }

    @Test
    public void testPlayerInteractionEventsResolve() {
        // Priority 4: Player interactions
        assertNotNull(catalog.resolve("playerchat"));
        assertNotNull(catalog.resolve("playerinteract"));
        assertNotNull(catalog.resolve("playermousebutton"));
        assertNotNull(catalog.resolve("playermousemotion"));
    }

    @Test
    public void testBlockEventsResolve() {
        // Priority 5: Block events
        assertNotNull(catalog.resolve("breakblock"));
        assertNotNull(catalog.resolve("placeblock"));
        assertNotNull(catalog.resolve("useblock"));
        assertNotNull(catalog.resolve("damageblock"));
    }

    @Test
    public void testWorldEventsResolve() {
        // Priority 6: World events
        assertNotNull(catalog.resolve("allworldsloaded"));
        assertNotNull(catalog.resolve("startworld"));
        assertNotNull(catalog.resolve("addworld"));
        assertNotNull(catalog.resolve("removeworld"));
    }

    @Test
    public void testPermissionEventsResolve() {
        // Priority 7: Permission events
        assertNotNull(catalog.resolve("grouppermissionchange"));
        assertNotNull(catalog.resolve("playergroup"));
        assertNotNull(catalog.resolve("playerpermissionchange"));
    }

    @Test
    public void testMiscEventsResolve() {
        // Priority 8: Miscellaneous
        assertNotNull(catalog.resolve("changegamemode"));
        assertNotNull(catalog.resolve("discoverzone"));
    }

    @Test
    public void testKnownEventsIncludesNewEvents() {
        var knownEvents = catalog.knownEventNames();
        
        // Should have at least 34+ events now
        assertTrue(knownEvents.size() >= 34, 
            "Should have at least 34 events, but found: " + knownEvents.size());
        
        // Check some critical inventory events are listed
        assertTrue(knownEvents.contains("livingentityinventorychange"));
        assertTrue(knownEvents.contains("dropitem"));
        assertTrue(knownEvents.contains("interactivelypickupitem"));
    }

    @Test
    public void testEventNameNormalization() {
        // Test that various name formats work
        assertNotNull(catalog.resolve("DropItem")); // PascalCase
        assertNotNull(catalog.resolve("dropitem")); // lowercase
        assertNotNull(catalog.resolve("drop_item")); // snake_case
        assertNotNull(catalog.resolve("drop-item")); // kebab-case
        assertNotNull(catalog.resolve("DropItemEvent")); // With Event suffix
    }

    @Test
    public void testUnknownEventThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            catalog.resolve("nonexistentevent");
        });
    }

    @Test
    public void testNullEventNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            catalog.resolve(null);
        });
    }

    @Test
    public void testBlankEventNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            catalog.resolve("  ");
        });
    }

    @Test
    public void testFullClassNameStillWorks() {
        // Should still support full class name format
        assertNotNull(catalog.resolve("com.hypixel.hytale.server.core.event.events.BootEvent"));
    }
}
