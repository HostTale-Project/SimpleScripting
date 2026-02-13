package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.extension.ExtensionRegistry;
import com.hosttale.simplescripting.extension.SimpleScriptingExtension;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class SampleModInstaller {

    private SampleModInstaller() {
    }

    /**
     * Install core example mods on first run.
     * @return true if examples were installed (first run), false if skipped
     */
    public static boolean installCoreExamplesIfFirstRun(Path modsRoot, HytaleLogger logger, ClassLoader resourceLoader, ModTemplateService templateService) {
        if (Files.exists(modsRoot)) {
            return false;
        }

        try {
            Files.createDirectories(modsRoot);
        } catch (IOException e) {
            logger.atSevere().log("Failed to create mods directory %s: %s", modsRoot, e.getMessage());
            return false;
        }

        // Install core examples
        installExample(resourceLoader, modsRoot, logger, templateService, "welcome-rewards");
        installExample(resourceLoader, modsRoot, logger, templateService, "home-warps");
        installExample(resourceLoader, modsRoot, logger, templateService, "afk-manager");
        installExample(resourceLoader, modsRoot, logger, templateService, "simple-stats", "stats-api.js");
        
        return true;
    }
    
    /**
     * Update types for core example mods after extensions are registered.
     * Should be called after extensions register but before loading mods.
     */
    public static void updateCoreExampleTypes(Path modsRoot, HytaleLogger logger, ModTemplateService templateService) {
        String[] coreExamples = {"welcome-rewards", "home-warps", "afk-manager", "simple-stats"};
        
        for (String exampleName : coreExamples) {
            Path exampleDir = modsRoot.resolve(exampleName);
            if (Files.exists(exampleDir)) {
                try {
                    templateService.copyTypesInternal(exampleDir);
                    logger.atInfo().log("Updated types for core example mod: %s", exampleName);
                } catch (IOException e) {
                    logger.atWarning().log("Failed to update types for core example %s: %s", exampleName, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Install extension example mods.
     * Should be called after extensions are registered.
     */
    public static void installExtensionExamples(Path modsRoot, HytaleLogger logger, ModTemplateService templateService, ExtensionRegistry extensionRegistry) {
        Map<String, String[]> extensionExamples = extensionRegistry.collectExampleMods();
        
        for (Map.Entry<String, String[]> entry : extensionExamples.entrySet()) {
            String extensionId = entry.getKey();
            String[] examplePaths = entry.getValue();
            
            for (String examplePath : examplePaths) {
                installExtensionExample(modsRoot, logger, templateService, extensionRegistry, extensionId, examplePath);
            }
        }
    }

    private static void installExample(ClassLoader loader, Path modsRoot, HytaleLogger logger, ModTemplateService templateService, String name, String... extraFiles) {
        try {
            Path exampleDir = modsRoot.resolve(name);
            List<String> files = new ArrayList<>(Arrays.asList("mod.json", "main.js"));
            files.addAll(Arrays.asList(extraFiles));
            for (String file : files) {
                copyResource(loader, "examples/" + name + "/" + file, exampleDir.resolve(file));
            }
            
            // Copy types from core and extensions
            try {
                templateService.copyTypesInternal(exampleDir);
                logger.atInfo().log("Installed example JS mod with types into %s.", exampleDir);
            } catch (IOException e) {
                logger.atWarning().log("Installed example mod %s but failed to copy types: %s", name, e.getMessage());
            }
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
    
    private static void installExtensionExample(Path modsRoot, HytaleLogger logger, ModTemplateService templateService, ExtensionRegistry extensionRegistry, String extensionId, String resourceBasePath) {
        // Extract mod name from path (e.g., "examples/player-shops" -> "player-shops")
        String modName = resourceBasePath.substring(resourceBasePath.lastIndexOf('/') + 1);
        Path exampleDir = modsRoot.resolve(modName);
        
        try {
            // Find the extension's class loader
            ClassLoader extensionLoader = findExtensionClassLoader(extensionRegistry.getExtensions(), extensionId);
            if (extensionLoader == null) {
                logger.atWarning().log("Could not find class loader for extension %s", extensionId);
                return;
            }
            
            // Copy all files from the example directory
            copyExampleFiles(extensionLoader, resourceBasePath, exampleDir);
            
            // Copy types
            try {
                templateService.copyTypesInternal(exampleDir);
                logger.atInfo().log("Installed example mod '%s' from extension '%s' with types.", modName, extensionId);
            } catch (IOException e) {
                logger.atWarning().log("Installed example mod %s from %s but failed to copy types: %s", modName, extensionId, e.getMessage());
            }
        } catch (IOException e) {
            logger.atSevere().log("Failed to install example mod %s from extension %s: %s", modName, extensionId, e.getMessage());
        }
    }
    
    private static ClassLoader findExtensionClassLoader(List<SimpleScriptingExtension> extensions, String extensionId) {
        for (SimpleScriptingExtension ext : extensions) {
            if (ext.getExtensionId().equals(extensionId)) {
                return ext.getClass().getClassLoader();
            }
        }
        return null;
    }
    
    private static void copyExampleFiles(ClassLoader loader, String resourceBasePath, Path targetDir) throws IOException {
        // Standard files that examples should have
        String[] standardFiles = {"mod.json", "main.js"};
        
        for (String file : standardFiles) {
            String resourcePath = resourceBasePath + "/" + file;
            Path targetPath = targetDir.resolve(file);
            
            try {
                copyResource(loader, resourcePath, targetPath);
            } catch (IOException e) {
                // If main.js or mod.json is missing, this is a problem
                if (file.equals("mod.json") || file.equals("main.js")) {
                    throw e;
                }
                // Otherwise just skip the file
            }
        }
        
        // Try to copy common optional files
        String[] optionalFiles = {"README.md", "config.json"};
        for (String file : optionalFiles) {
            String resourcePath = resourceBasePath + "/" + file;
            Path targetPath = targetDir.resolve(file);
            
            try {
                copyResource(loader, resourcePath, targetPath);
            } catch (IOException e) {
                // Optional files - ignore if missing
            }
        }
    }
}
