package com.hosttale.simplescripting.mod;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SampleModInstaller {

    private SampleModInstaller() {
    }

    public static void installIfFirstRun(Path modsRoot, HytaleLogger logger, ClassLoader resourceLoader) {
        if (Files.exists(modsRoot)) {
            return;
        }

        try {
            Files.createDirectories(modsRoot);
        } catch (IOException e) {
            logger.atSevere().log("Failed to create mods directory %s: %s", modsRoot, e.getMessage());
            return;
        }

        installExample(resourceLoader, modsRoot, logger, "hello-world");
        installExample(resourceLoader, modsRoot, logger, "shared-provider");
        installExample(resourceLoader, modsRoot, logger, "shared-consumer");
    }

    private static void installExample(ClassLoader loader, Path modsRoot, HytaleLogger logger, String name) {
        try {
            Path exampleDir = modsRoot.resolve(name);
            copyResource(loader, "examples/" + name + "/mod.json", exampleDir.resolve("mod.json"));
            copyResource(loader, "examples/" + name + "/main.js", exampleDir.resolve("main.js"));
            logger.atInfo().log("Installed example JS mod into %s.", exampleDir);
        } catch (IOException e) {
            logger.atSevere().log("Failed to install example mod %s: %s", name, e.getMessage());
        }
    }

    private static void copyResource(ClassLoader loader, String resourcePath, Path targetPath) throws IOException {
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.createDirectories(targetPath.getParent());
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
