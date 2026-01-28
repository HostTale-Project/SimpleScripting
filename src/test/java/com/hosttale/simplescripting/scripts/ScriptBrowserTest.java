package com.hosttale.simplescripting.scripts;

import com.hosttale.simplescripting.mod.JsModManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScriptBrowserTest {

    @TempDir
    Path tempDir;

    @Test
    void listReturnsFoldersThenScriptsAlphabetically() throws IOException {
        Path mods = tempDir.resolve("mods");
        Path modA = mods.resolve("alpha");
        Files.createDirectories(modA);
        Files.writeString(modA.resolve("b.js"), "// b");
        Files.writeString(modA.resolve("a.js"), "// a");
        Files.createDirectories(modA.resolve("sub"));

        ScriptBrowser browser = new ScriptBrowser(mods, mock(JsModManager.class), mockLogger());

        var entries = browser.list("alpha");
        assertEquals(3, entries.size());
        assertTrue(entries.get(0).isFolder());
        assertEquals("sub", entries.get(0).name());
        assertEquals("a.js", entries.get(1).name());
        assertEquals("b.js", entries.get(2).name());
    }

    @Test
    void writeAndReadRoundTrip() throws IOException {
        Path mods = tempDir.resolve("mods");
        Path modA = mods.resolve("alpha");
        Files.createDirectories(modA);

        ScriptBrowser browser = new ScriptBrowser(mods, mock(JsModManager.class), mockLogger());
        browser.write("alpha/test.js", "hello");

        assertEquals("hello", browser.read("alpha/test.js"));
    }

    @Test
    void reloadContainingModUsesModManager() {
        JsModManager manager = mock(JsModManager.class);
        when(manager.getLoadedMods()).thenReturn(Map.of("alpha", mock()));
        when(manager.reloadMod("alpha")).thenReturn(true);

        ScriptBrowser browser = new ScriptBrowser(tempDir, manager, mockLogger());
        ScriptBrowser.ReloadResult result = browser.reloadContainingMod("alpha/main.js");

        assertTrue(result.success());
        assertEquals("alpha", result.modId());
        verify(manager).reloadMod("alpha");
    }

    @Test
    void rejectsTraversalOutsideModsRoot() {
        ScriptBrowser browser = new ScriptBrowser(tempDir, mock(JsModManager.class), mockLogger());
        assertThrows(IllegalArgumentException.class, () -> browser.read("../other.js"));
    }

    private com.hypixel.hytale.logger.HytaleLogger mockLogger() {
        com.hypixel.hytale.logger.HytaleLogger logger = mock(com.hypixel.hytale.logger.HytaleLogger.class, RETURNS_DEEP_STUBS);
        when(logger.getSubLogger(anyString())).thenReturn(logger);
        return logger;
    }
}
