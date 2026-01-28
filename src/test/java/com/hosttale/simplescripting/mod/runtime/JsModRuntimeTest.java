package com.hosttale.simplescripting.mod.runtime;

import com.hosttale.simplescripting.mod.JsModDefinition;
import com.hosttale.simplescripting.mod.SharedServiceRegistry;
import com.hosttale.simplescripting.mod.model.JsModManifest;
import com.hosttale.simplescripting.mod.runtime.JsPluginServices;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.plugin.registry.AssetRegistry;
import com.hypixel.hytale.server.core.task.TaskRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JsModRuntimeTest {

    @TempDir
    Path tempDir;

    private HytaleLogger logger;

    @BeforeEach
    void setUp() {
        logger = HytaleLogger.get("test-logger");
    }

    @Test
    void loadValidScriptMarksRuntimeLoaded() throws Exception {
        JsModRuntime runtime = runtimeWithScript("function onEnable() {}");
        try {
            runtime.load();
            assertTrue(runtime.isLoaded());
        } finally {
            runtime.close();
        }
    }

    @Test
    void loadWithSyntaxErrorCleansStateAndThrows() throws Exception {
        JsModRuntime runtime = runtimeWithScript("function onEnable(");
        assertThrows(IOException.class, runtime::load);
        assertFalse(runtime.isLoaded());
        runtime.close();
    }

    @Test
    void enableDoesNotPropagateHookExceptions() throws Exception {
        JsModRuntime runtime = runtimeWithScript("function onEnable(){ throw new Error('boom'); }");
        try {
            runtime.load();
            runtime.enable();
            assertTrue(runtime.isLoaded());
        } finally {
            runtime.close();
        }
    }

    @Test
    void reloadKeepsRuntimeLoadedEvenWhenHookErrors() throws Exception {
        JsModRuntime runtime = runtimeWithScript("function onReload(){ throw new Error('reload'); }");
        try {
            runtime.load();
            runtime.reload();

            assertTrue(runtime.isLoaded());
        } finally {
            runtime.close();
        }
    }

    @Test
    void disableHandlesHookErrorsAndUnloads() throws Exception {
        JsModRuntime runtime = runtimeWithScript("function onDisable(){ throw new Error('nope'); }");
        try {
            runtime.load();
            runtime.disable();

            assertFalse(runtime.isLoaded());
        } finally {
            runtime.close();
        }
    }

    private JsModRuntime runtimeWithScript(String script) throws IOException {
        Path modRoot = Files.createDirectories(tempDir.resolve("mod-" + System.nanoTime()));
        Path entrypoint = modRoot.resolve("main.js");
        Files.writeString(entrypoint, script, StandardCharsets.UTF_8);

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

        JsModDefinition definition = new JsModDefinition(modRoot, manifest);
        JsPluginServices services = JsPluginServices.of(
                mock(CommandRegistry.class),
                mock(EventRegistry.class),
                mock(TaskRegistry.class),
                mock(AssetRegistry.class),
                logger
        );
        return new JsModRuntime(definition, logger, new SharedServiceRegistry(), services);
    }
}
