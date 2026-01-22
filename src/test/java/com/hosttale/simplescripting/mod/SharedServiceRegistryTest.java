package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mozilla.javascript.Scriptable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedServiceRegistryTest {

    @Test
    void exposesAndRetrievesService() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = Mockito.mock(JsModRuntime.class);
        Scriptable api = Mockito.mock(Scriptable.class);

        boolean added = registry.expose("service", "owner", runtime, api);

        assertTrue(added);
        assertTrue(registry.get("service").isPresent());
    }

    @Test
    void preventsTakeoverByAnotherOwner() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = Mockito.mock(JsModRuntime.class);
        Scriptable api = Mockito.mock(Scriptable.class);

        assertTrue(registry.expose("service", "ownerA", runtime, api));
        assertFalse(registry.expose("service", "ownerB", runtime, api));
        assertTrue(registry.get("service").isPresent());
        assertTrue(registry.get("service").get().ownerId().equals("ownerA"));
    }

    @Test
    void removeOwnerClearsServices() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = Mockito.mock(JsModRuntime.class);
        Scriptable api = Mockito.mock(Scriptable.class);
        registry.expose("service", "ownerA", runtime, api);

        registry.removeOwner("ownerA");

        assertTrue(registry.get("service").isEmpty());
    }

    @Test
    void rejectsBlankOrNullInputs() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = Mockito.mock(JsModRuntime.class);
        Scriptable api = Mockito.mock(Scriptable.class);

        assertFalse(registry.expose("", "owner", runtime, api));
        assertFalse(registry.expose(null, "owner", runtime, api));
        assertFalse(registry.expose("service", "owner", runtime, null));
    }
}
