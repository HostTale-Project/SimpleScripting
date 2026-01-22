package com.hosttale.simplescripting.mod;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Pattern;

public final class ModTemplateService {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_-]+$");

    private final Path modsRoot;
    private final ClassLoader resourceLoader;
    private final HytaleLogger logger;

    public ModTemplateService(Path modsRoot, ClassLoader resourceLoader, HytaleLogger logger) {
        this.modsRoot = modsRoot;
        this.resourceLoader = resourceLoader;
        this.logger = logger.getSubLogger("mod-template");
    }

    public void createMod(String modName, CommandContext context) {
        if (!isValidId(modName)) {
            context.sendMessage(Message.raw("Invalid mod name. Use lowercase letters, numbers, hyphens or underscores."));
            return;
        }
        Path targetDir = modsRoot.resolve(modName);
        if (Files.exists(targetDir)) {
            context.sendMessage(Message.raw("Mod '" + modName + "' already exists."));
            return;
        }

        try {
            Files.createDirectories(targetDir);
            copyTemplate("mod-template/mod.json", targetDir.resolve("mod.json"),
                    Map.of("__MOD_ID__", modName, "__MOD_NAME__", modName));
            copyTemplate("mod-template/main.js", targetDir.resolve("main.js"),
                    Map.of("__MOD_ID__", modName, "__MOD_NAME__", modName));
            copyTypes(targetDir);
            context.sendMessage(Message.raw("Created mod '" + modName + "' at " + targetDir.getFileName() + "."));
        } catch (IOException e) {
            logger.atSevere().log("Failed to create mod %s: %s", modName, e.getMessage());
            context.sendMessage(Message.raw("Failed to create mod '" + modName + "'. Check logs."));
        }
    }

    public void updateTypes(String modName, CommandContext context) {
        Path targetDir = modsRoot.resolve(modName);
        if (!Files.isDirectory(targetDir)) {
            context.sendMessage(Message.raw("Mod '" + modName + "' does not exist."));
            return;
        }
        try {
            copyTypes(targetDir);
            context.sendMessage(Message.raw("Updated index.d.ts for mod '" + modName + "'."));
        } catch (IOException e) {
            logger.atSevere().log("Failed to update types for %s: %s", modName, e.getMessage());
            context.sendMessage(Message.raw("Failed to update types for '" + modName + "'. Check logs."));
        }
    }

    private boolean isValidId(String modName) {
        return modName != null && ID_PATTERN.matcher(modName).matches();
    }

    private void copyTemplate(String resourcePath, Path targetPath, Map<String, String> replacements) throws IOException {
        String content = readResource(resourcePath);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, content, StandardCharsets.UTF_8);
    }

    private void copyTypes(Path targetDir) throws IOException {
        Path target = targetDir.resolve("index.d.ts");
        try (InputStream in = resourceLoader.getResourceAsStream("index.d.ts")) {
            if (in == null) {
                throw new IOException("index.d.ts resource not found");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream in = resourceLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
