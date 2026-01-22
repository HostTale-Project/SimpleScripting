package com.hosttale.simplescripting.mod.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsModManifestValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void validManifestPasses() throws Exception {
        Files.writeString(tempDir.resolve("main.js"), "// ok");
        List<String> errors = validate(baseManifest("main.js"));

        assertTrue(errors.isEmpty());
    }

    @Test
    void rejectsAbsoluteEntrypoint() throws Exception {
        List<String> errors = validate(baseManifest("/tmp/evil.js"));

        assertTrue(errors.stream().anyMatch(msg -> msg.contains("relative path inside the mod directory")));
    }

    @Test
    void rejectsParentTraversalEntrypoint() throws Exception {
        List<String> errors = validate(baseManifest("../evil.js"));

        assertTrue(errors.stream().anyMatch(msg -> msg.contains("parent directory")));
    }

    @Test
    void rejectsSymlinkEscapingModRoot() throws Exception {
        Path outside = Files.createTempFile("outside", ".js");
        Path link = tempDir.resolve("link.js");
        Files.createSymbolicLink(link, outside);

        List<String> errors = validate(baseManifest("link.js"));

        assertTrue(errors.stream().anyMatch(msg -> msg.contains("stay within the mod directory")));
    }

    @Test
    void rejectsSelfDependency() throws Exception {
        Files.writeString(tempDir.resolve("main.js"), "// ok");
        JsModManifest manifest = new JsModManifest(
                "test-id",
                "Test",
                "1.0.0",
                "main.js",
                List.of(),
                Set.of(),
                null,
                false,
                List.of("test-id")
        );

        List<String> errors = validate(manifest);

        assertTrue(errors.stream().anyMatch(msg -> msg.contains("must not include the mod's own id")));
    }

    private List<String> validate(JsModManifest manifest) throws IOException {
        Path manifestPath = tempDir.resolve("mod.json");
        Files.writeString(manifestPath, "{}");
        return JsModManifestValidator.validate(manifestPath, manifest);
    }

    private JsModManifest baseManifest(String entrypoint) {
        return new JsModManifest(
                "test-id",
                "Test",
                "1.0.0",
                entrypoint,
                List.of(),
                Set.of(),
                null,
                false,
                List.of()
        );
    }
}
