package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.mod.model.JsModManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsModInstanceTest {

    @TempDir
    Path tempDir;

    private SharedServiceRegistry registry;
    private HytaleLogger logger;

    @BeforeEach
    void setUp() {
        registry = new SharedServiceRegistry();
        logger = HytaleLogger.get("test-logger");
    }

    @Test
    void loadAndEnableMarksEnabled() throws Exception {
        JsModDefinition definition = definitionWithScript(safeLifecycleScript());
        JsModRuntime runtime = new com.hosttale.simplescripting.mod.runtime.JsModRuntime(definition, logger, registry);
        JsModInstance instance = new JsModInstance(definition, runtime, logger, registry);

        instance.loadAndEnable();

        assertTrue(instance.isEnabled());
        assertTrue(runtime.isLoaded());

        runtime.close();
    }

    @Test
    void disableRemovesServicesAndMarksDisabled() throws Exception {
        JsModDefinition definition = definitionWithScript(safeLifecycleScript());
        JsModRuntime runtime = new com.hosttale.simplescripting.mod.runtime.JsModRuntime(definition, logger, registry);
        JsModInstance instance = new JsModInstance(definition, runtime, logger, registry);
        instance.loadAndEnable();
        org.mozilla.javascript.Scriptable api = org.mockito.Mockito.mock(org.mozilla.javascript.Scriptable.class);
        registry.expose("svc", "test-id", runtime, api);

        instance.disable();

        assertFalse(instance.isEnabled());
        assertTrue(registry.get("svc").isEmpty());
        assertFalse(runtime.isLoaded());

        runtime.close();
    }

    @Test
    void reloadReenablesRuntime() throws Exception {
        JsModDefinition definition = definitionWithScript(safeLifecycleScript());
        JsModRuntime runtime = new com.hosttale.simplescripting.mod.runtime.JsModRuntime(definition, logger, registry);
        JsModInstance instance = new JsModInstance(definition, runtime, logger, registry);
        instance.loadAndEnable();

        instance.reload();

        assertTrue(instance.isEnabled());
        assertTrue(runtime.isLoaded());

        runtime.close();
    }

    private JsModDefinition definitionWithScript(String script) throws IOException {
        Path modRoot = Files.createDirectories(tempDir.resolve("mod-" + System.nanoTime()));
        JsModManifest manifest = new JsModManifest(
                "test-id",
                "Test",
                "1.0.0",
                "main.js",
                List.of(),
                Set.of(),
                null,
                false,
                List.of()
        );
        Files.writeString(modRoot.resolve("main.js"), script);
        return new JsModDefinition(modRoot, manifest);
    }

    private String safeLifecycleScript() {
        return """
                function onEnable() {}
                function onDisable() {}
                function onReload() {}
                """;
    }
}
