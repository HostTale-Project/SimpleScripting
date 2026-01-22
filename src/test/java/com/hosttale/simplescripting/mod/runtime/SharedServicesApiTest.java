package com.hosttale.simplescripting.mod.runtime;

import com.hosttale.simplescripting.mod.SharedServiceRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SharedServicesApiTest {

    @Test
    void exposeRejectsNonScriptable() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = mock(JsModRuntime.class);
        HytaleLogger logger = HytaleLogger.get("test-logger");
        SharedServicesApi api = new SharedServicesApi("owner", registry, runtime, logger);

        assertFalse(api.expose("svc", new Object()));
        assertTrue(registry.get("svc").isEmpty());
    }

    @Test
    void exposeRegistersService() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = mock(JsModRuntime.class);
        HytaleLogger logger = HytaleLogger.get("test-logger");
        SharedServicesApi api = new SharedServicesApi("owner", registry, runtime, logger);
        Scriptable scriptable = mock(Scriptable.class);

        assertTrue(api.expose("svc", scriptable));
        assertTrue(registry.get("svc").isPresent());
    }

    @Test
    void callReturnsNullWhenServiceMissing() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = mock(JsModRuntime.class);
        HytaleLogger logger = HytaleLogger.get("test-logger");
        SharedServicesApi api = new SharedServicesApi("owner", registry, runtime, logger);

        assertNull(api.call("missing", "method", null));
        verifyNoInteractions(runtime);
    }

    @Test
    void callInvokesTargetFunctionWithConvertedArgs() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = mock(JsModRuntime.class);
        HytaleLogger logger = HytaleLogger.get("test-logger");
        SharedServicesApi api = new SharedServicesApi("owner", registry, runtime, logger);
        Scriptable scriptable = mock(Scriptable.class);
        registry.expose("svc", "ownerB", runtime, scriptable);

        NativeArray array = new NativeArray(new Object[]{1, "two"});
        when(runtime.invokeFunction(eq(scriptable), eq("echo"), any(Object[].class))).thenReturn("ok");

        Object result = api.call("svc", "echo", array);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(runtime).invokeFunction(eq(scriptable), eq("echo"), captor.capture());
        Object[] passedArgs = captor.getValue();
        assertEquals(2, passedArgs.length);
        assertEquals(1, passedArgs[0]);
        assertEquals("two", passedArgs[1]);
        assertEquals("ok", result);
    }

    @Test
    void callReturnsNullWhenInvocationFails() {
        SharedServiceRegistry registry = new SharedServiceRegistry();
        JsModRuntime runtime = mock(JsModRuntime.class);
        HytaleLogger logger = HytaleLogger.get("test-logger");
        SharedServicesApi api = new SharedServicesApi("owner", registry, runtime, logger);
        Scriptable scriptable = mock(Scriptable.class);
        registry.expose("svc", "ownerB", runtime, scriptable);

        when(runtime.invokeFunction(any(Scriptable.class), anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("boom"));

        Object result = api.call("svc", "echo", null);

        assertNull(result);
    }
}
