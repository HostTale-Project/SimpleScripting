package com.hosttale.simplescripting.managers;

import com.hosttale.simplescripting.util.Logger;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Manages the mods directory structure and file operations.
 * Responsible for creating directories and listing files.
 */
public class ModsDirectoryManager {
    private final Path modsPath;
    private final Path assetsPath;
    private final HytaleLogger logger;
    private final Logger loggerWrapper;

    public ModsDirectoryManager(Path modsPath, HytaleLogger logger) {
        this.modsPath = modsPath;
        this.assetsPath = modsPath.getParent().resolve("assets");
        this.logger = logger;
        this.loggerWrapper = new Logger(logger);
    }

    /**
     * Ensures the mods directory exists, creating it if necessary.
     * Also creates the lib subdirectory and assets folder structure.
     * @return true if the directory was created, false if it already existed
     * @throws IOException if directory creation fails
     */
    public boolean ensureDirectoryExists() throws IOException {
        boolean created = false;
        
        if (!Files.exists(modsPath)) {
            Files.createDirectories(modsPath);
            loggerWrapper.info("Created mods folder: " + modsPath);
            created = true;
        }
        
        // Create lib subdirectory for utility scripts
        Path libPath = modsPath.resolve("lib");
        if (!Files.exists(libPath)) {
            Files.createDirectories(libPath);
            loggerWrapper.info("Created lib folder: " + libPath);
        }
        
        // Create assets folder structure
        createAssetsStructure();
        
        return created;
    }
    
    /**
     * Creates the assets folder structure for configuration, language files, and UI.
     */
    private void createAssetsStructure() throws IOException {
        // Create main assets folder
        if (!Files.exists(assetsPath)) {
            Files.createDirectories(assetsPath);
            loggerWrapper.info("Created assets folder: " + assetsPath);
        }
        
        // Create subfolders
        String[] subfolders = {"config", "lang", "ui"};
        for (String subfolder : subfolders) {
            Path subPath = assetsPath.resolve(subfolder);
            if (!Files.exists(subPath)) {
                Files.createDirectories(subPath);
            }
        }
    }

    /**
     * Lists all files in the mods directory.
     * @return Stream of paths in the mods directory
     * @throws IOException if directory listing fails
     */
    public Stream<Path> listFiles() throws IOException {
        logger.atInfo().log("Listing files in mods folder: " + modsPath);
        return Files.list(modsPath);
    }

    /**
     * Checks if a path is a directory.
     */
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public Path getModsPath() {
        return modsPath;
    }
    
    public Path getAssetsPath() {
        return assetsPath;
    }

    /**
     * Copies sample JavaScript files from resources to the mods directory.
     * Uses ClassLoader to access resources bundled in the JAR.
     * @param classLoader ClassLoader to access bundled resources
     * @throws IOException if copying fails
     */
    public void copySampleFiles(ClassLoader classLoader) throws IOException {
        if (classLoader == null) {
            logger.atWarning().log("ClassLoader is null, skipping sample files copy");
            return;
        }
        
        logger.atInfo().log("Copying sample files to mods folder...");
        
        // Library files (in lib/ subfolder)
        String[] libFiles = {
            "lib/utils.js",
            "lib/config.js"
        };
        
        // Main feature files
        String[] mainFiles = {
            "homes.js",
            "warps.js",
            "spawn.js",
            "tpa.js",
            "back.js",
            "rtp.js",
            "ranks.js",
            "admin.js",
            "auto-stop.js",
        };
        
        // Copy lib files
        Path libPath = modsPath.resolve("lib");
        if (!Files.exists(libPath)) {
            Files.createDirectories(libPath);
        }
        
        for (String fileName : libFiles) {
            copyResourceFile(classLoader, fileName);
        }
        
        // Copy main files
        for (String fileName : mainFiles) {
            copyResourceFile(classLoader, fileName);
        }
        
        // Copy README
        copyResourceFile(classLoader, "README.md");
        
        logger.atInfo().log("Sample files copied successfully!");
    }
    
    /**
     * Copies a single resource file to the mods directory.
     */
    private void copyResourceFile(ClassLoader classLoader, String fileName) {
        try (var inputStream = classLoader.getResourceAsStream("samples/" + fileName)) {
            if (inputStream == null) {
                logger.atWarning().log("Sample file not found in resources: " + fileName);
                return;
            }
            
            Path target = modsPath.resolve(fileName);
            
            // Ensure parent directories exist
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            Files.copy(inputStream, target);
            logger.atInfo().log("Copied sample: " + fileName);
        } catch (IOException e) {
            logger.atSevere().log("Failed to copy sample file: " + fileName + " - " + e.getMessage());
        }
    }
}
