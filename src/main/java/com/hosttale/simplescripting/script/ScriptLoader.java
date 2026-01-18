package com.hosttale.simplescripting.script;

import com.hosttale.simplescripting.managers.ModsDirectoryManager;
import com.hosttale.simplescripting.managers.ScriptRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads and executes JavaScript files.
 * Responsible for reading script files and executing them in the JavaScript context.
 * Supports hot reload functionality.
 */
public class ScriptLoader {
    private final ModsDirectoryManager directoryManager;
    private final JavaScriptContextBuilder contextBuilder;
    private final ScriptRegistry scriptRegistry;
    private final HytaleLogger logger;
    
    // Current JavaScript context and scope for reload support
    private Context currentContext;
    private Scriptable currentScope;
    private int loadedScriptCount;

    public ScriptLoader(ModsDirectoryManager directoryManager,
                        JavaScriptContextBuilder contextBuilder,
                        ScriptRegistry scriptRegistry,
                        HytaleLogger logger) {
        this.directoryManager = directoryManager;
        this.contextBuilder = contextBuilder;
        this.scriptRegistry = scriptRegistry;
        this.logger = logger;
        this.loadedScriptCount = 0;
    }

    /**
     * Loads and executes all JavaScript files from the mods directory.
     * If the directory was just created, copies sample files first.
     * @param classLoader ClassLoader to access bundled resources
     * @throws IOException if file operations fail
     */
    public void loadAllScripts(ClassLoader classLoader) throws IOException {
        boolean isNewInstallation = directoryManager.ensureDirectoryExists();
        
        // Copy sample files on first installation
        if (isNewInstallation) {
            directoryManager.copySampleFiles(classLoader);
        }

        currentContext = Context.enter();
        try {
            currentScope = contextBuilder.buildScope(currentContext);
            loadedScriptCount = 0;

            // Get all script files sorted (lib/ first, then root files)
            List<Path> scriptFiles = getScriptFilesInOrder();
            
            for (Path file : scriptFiles) {
                processFile(file, currentContext, currentScope);
            }
            
            logger.atInfo().log("Loaded " + loadedScriptCount + " scripts");
        } finally {
            Context.exit();
        }
    }
    
    /**
     * Reloads all scripts.
     * Unregisters all existing commands/events and reloads from disk.
     * @return Number of scripts reloaded
     */
    public int reloadAllScripts() {
        logger.atInfo().log("Reloading all scripts...");
        
        // Unregister all existing resources
        scriptRegistry.unregisterAll();
        
        // Shutdown old context resources
        contextBuilder.shutdown();
        
        // Exit old context if exists
        if (currentContext != null) {
            try {
                Context.exit();
            } catch (IllegalStateException e) {
                // Context not entered, ignore
            }
        }
        
        // Create new context and scope
        currentContext = Context.enter();
        try {
            currentScope = contextBuilder.buildScope(currentContext);
            loadedScriptCount = 0;
            
            // Get all script files sorted
            List<Path> scriptFiles = getScriptFilesInOrder();
            
            for (Path file : scriptFiles) {
                processFile(file, currentContext, currentScope);
            }
            
            logger.atInfo().log("Reloaded " + loadedScriptCount + " scripts");
            return loadedScriptCount;
        } catch (Exception e) {
            logger.atSevere().log("Error reloading scripts: " + e.getMessage());
            return 0;
        } finally {
            Context.exit();
        }
    }
    
    /**
     * Gets all script files in the correct loading order.
     * Library files (in lib/ subdirectory) are loaded first.
     * @return Sorted list of script file paths
     */
    private List<Path> getScriptFilesInOrder() {
        List<Path> libFiles = new ArrayList<>();
        List<Path> rootFiles = new ArrayList<>();
        
        try (Stream<Path> stream = Files.walk(directoryManager.getModsPath(), 2)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".js"))
                  .forEach(path -> {
                      Path relative = directoryManager.getModsPath().relativize(path);
                      if (relative.startsWith("lib")) {
                          libFiles.add(path);
                      } else {
                          rootFiles.add(path);
                      }
                  });
        } catch (IOException e) {
            logger.atWarning().log("Error listing script files: " + e.getMessage());
        }
        
        // Sort each list alphabetically
        libFiles.sort(Comparator.comparing(Path::getFileName));
        rootFiles.sort(Comparator.comparing(Path::getFileName));
        
        // Combine: lib files first, then root files
        List<Path> allFiles = new ArrayList<>();
        allFiles.addAll(libFiles);
        allFiles.addAll(rootFiles);
        
        return allFiles;
    }

    /**
     * Processes a single file, executing it if it's a script file.
     * @param file The file to process
     * @param context The JavaScript context
     * @param scope The JavaScript scope
     */
    private void processFile(Path file, Context context, Scriptable scope) {
        String fileName = file.getFileName().toString();
        
        if (directoryManager.isDirectory(file)) {
            return; // Skip directories
        }
        
        if (!fileName.endsWith(".js")) {
            return; // Skip non-JS files
        }
        
        logger.atInfo().log("[LOADING] " + getRelativePath(file));
        executeScript(file, context, scope);
    }
    
    /**
     * Gets the relative path from mods directory.
     */
    private String getRelativePath(Path file) {
        return directoryManager.getModsPath().relativize(file).toString();
    }

    /**
     * Reads and executes a JavaScript file.
     * @param file The script file to execute
     * @param context The JavaScript context
     * @param scope The JavaScript scope
     */
    private void executeScript(Path file, Context context, Scriptable scope) {
        String scriptName = getRelativePath(file);
        
        try {
            // Set current script for resource tracking
            scriptRegistry.setCurrentScript(scriptName);
            
            String source = Files.readString(file);
            context.evaluateString(scope, source, scriptName, 1, null);
            loadedScriptCount++;
            
        } catch (IOException e) {
            logger.atSevere().log("Error reading file: " + scriptName + " - " + e.getMessage());
        } catch (Exception e) {
            logger.atSevere().log("Error executing script: " + scriptName + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            scriptRegistry.clearCurrentScript();
        }
    }
    
    /**
     * Gets the number of loaded scripts.
     * @return Script count
     */
    public int getLoadedScriptCount() {
        return loadedScriptCount;
    }
    
    /**
     * Gets the context builder for access to managers.
     * @return The context builder
     */
    public JavaScriptContextBuilder getContextBuilder() {
        return contextBuilder;
    }
}
