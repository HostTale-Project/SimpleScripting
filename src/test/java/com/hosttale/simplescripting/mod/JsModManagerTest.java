package com.hosttale.simplescripting.mod;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JsModManagerTest {

    @TempDir
    Path tempDir;

    private HytaleLogger logger;

    @BeforeEach
    void setUp() {
        logger = HytaleLogger.get("test-logger");
    }

    @Test
    void discoverAndLoadModsLoadsValidMods() throws Exception {
        createMod("alpha", null, false, null);

        JsModManager manager = new JsModManager(tempDir, logger, pluginServices());
        manager.discoverAndLoadMods();

        assertTrue(manager.getLoadedMods().containsKey("alpha"));
        assertTrue(manager.getLoadedMods().get("alpha").isEnabled());
    }

    @Test
    void skipsDuplicateIds() throws Exception {
        createMod("dup", null, false, null);
        createMod("dup-folder", null, false, "dup"); // different folder, same id

        JsModManager manager = new JsModManager(tempDir, logger, pluginServices());
        manager.discoverAndLoadMods();

        assertEquals(1, manager.getLoadedMods().size());
        assertTrue(manager.getLoadedMods().containsKey("dup"));
    }

    @Test
    void reloadModHandlesExistingAndUnknownIds() throws Exception {
        createMod("alpha", null, false, null);
        JsModManager manager = new JsModManager(tempDir, logger, pluginServices());
        manager.discoverAndLoadMods();

        assertTrue(manager.reloadMod("alpha"));
        assertFalse(manager.reloadMod("missing"));
        assertTrue(manager.getLoadedMods().get("alpha").isEnabled());
    }

    @Test
    void dependencyFailuresPreventLoading() throws Exception {
        createMod("needs-other", "missing", false, null);
        JsModManager manager = new JsModManager(tempDir, logger, pluginServices());

        manager.discoverAndLoadMods();

        assertTrue(manager.getLoadedMods().isEmpty());
    }

    @Test
    void disableAllClearsLoadedMods() throws Exception {
        createMod("alpha", null, false, null);
        createMod("beta", null, false, null);
        JsModManager manager = new JsModManager(tempDir, logger, pluginServices());
        manager.discoverAndLoadMods();

        manager.disableAll();

        assertTrue(manager.getLoadedMods().isEmpty());
    }

    private void createMod(String folderName, String dependency, boolean preload, String idOverride) throws IOException {
        Path modDir = Files.createDirectories(tempDir.resolve(folderName));
        Path main = modDir.resolve("main.js");
        Files.writeString(main, "function onEnable() {}");
        String id = idOverride == null ? folderName : idOverride;
        String deps = dependency == null ? "" : "\"dependencies\":[\"" + dependency + "\"],";
        String preloadField = preload ? "\"preload\":true," : "";
        String manifest = """
                {
                  "id":"%s",
                  "name":"Test",
                  "version":"1.0.0",
                  "entrypoint":"main.js",
                  %s
                  %s
                  "requiredAssetPacks":[],
                  "permissions":[]
                }
                """.formatted(id, preloadField, deps);
        Files.writeString(modDir.resolve("mod.json"), manifest);
    }

    private JsPluginServices pluginServices() {
        return JsPluginServices.of(
                mock(CommandRegistry.class),
                mock(EventRegistry.class),
                mock(TaskRegistry.class),
                mock(AssetRegistry.class),
                logger
        );
    }
}
