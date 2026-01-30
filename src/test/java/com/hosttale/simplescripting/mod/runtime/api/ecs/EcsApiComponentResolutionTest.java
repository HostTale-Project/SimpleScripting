package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hosttale.simplescripting.mod.runtime.ModRegistrationTracker;
import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.component.ComponentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EcsApiComponentResolutionTest {

    @Test
    void resolvesTransformComponentByCommonNames() {
        HytaleLogger logger = Mockito.mock(HytaleLogger.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(logger.getSubLogger(Mockito.anyString())).thenReturn(logger);
        ModRegistrationTracker tracker = new ModRegistrationTracker();
        JsModRuntime runtime = Mockito.mock(JsModRuntime.class);

        EcsApi ecs = new EcsApi(logger, tracker, runtime);

        @SuppressWarnings("unchecked")
        ComponentType<?, ?> expected = Mockito.mock(ComponentType.class);
        ecs.components().put("TransformComponent", (ComponentType) expected);
        ecs.components().put("transformcomponent", (ComponentType) expected);
        ecs.components().put("Transform", (ComponentType) expected);
        ecs.components().put("transform", (ComponentType) expected);

        assertNotNull(expected);
        assertSame(expected, ecs.component("TransformComponent"));
        assertSame(expected, ecs.component("transformcomponent"));
        assertSame(expected, ecs.component("Transform"));
        assertSame(expected, ecs.component("transform"));
    }
}
