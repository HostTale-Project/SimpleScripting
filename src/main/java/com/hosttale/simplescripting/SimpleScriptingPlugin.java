package com.hosttale.simplescripting;

import com.hosttale.simplescripting.managers.EventManager;
import com.hosttale.simplescripting.managers.ModsDirectoryManager;
import com.hosttale.simplescripting.managers.ScriptRegistry;
import com.hosttale.simplescripting.script.JavaScriptContextBuilder;
import com.hosttale.simplescripting.script.ScriptLoader;
import com.hosttale.simplescripting.task.Scheduler;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Main plugin class for SimpleScripting.
 * Orchestrates the loading and execution of JavaScript mods.
 * 
 * This plugin enables server administrators to create custom functionality
 * using JavaScript, similar to how Spigot plugins allow Java customization
 * for Minecraft servers.
 */
public class SimpleScriptingPlugin extends JavaPlugin {
    private static SimpleScriptingPlugin instance;
    
    // Script management
    private ScriptLoader scriptLoader;
    private ScriptRegistry scriptRegistry;
    
    // Directory management
    private ModsDirectoryManager directoryManager;
    
    // JavaScript context
    private JavaScriptContextBuilder contextBuilder;

    public SimpleScriptingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    /**
     * Gets the plugin instance.
     * @return The plugin instance
     */
    public static SimpleScriptingPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();
        
        Path modsFolderPath = getModsFolderPath();

        // Initialize script registry for hot reload support
        scriptRegistry = new ScriptRegistry();

        // Initialize components
        directoryManager = new ModsDirectoryManager(modsFolderPath, getLogger());
        contextBuilder = new JavaScriptContextBuilder(this, getLogger(), scriptRegistry);
        scriptLoader = new ScriptLoader(directoryManager, contextBuilder, scriptRegistry, getLogger());

        // Load all scripts (will copy samples on first run)
        try {
            scriptLoader.loadAllScripts(getClass().getClassLoader());

            // Start event polling system using Scheduler
            EventManager eventManager = contextBuilder.getEventManager();
            Scheduler scheduler = contextBuilder.getScheduler();
            if (eventManager != null && scheduler != null) {
                eventManager.startEventPolling(scheduler);
                getLogger().atInfo().log("Started event polling system");
            }

        } catch (IOException e) {
            getLogger().atSevere().log("Error loading scripts: " + e.getMessage());
        }
    }
    
    @Override
    protected void shutdown() {
        super.shutdown();
        
        // Shutdown context resources
        if (contextBuilder != null) {
            contextBuilder.shutdown();
        }
        
        getLogger().atInfo().log("SimpleScripting shutdown complete");
    }

    /**
     * Resolves the mods folder path.
     * @return Path to the mods directory
     */
    private Path getModsFolderPath() {
        return Constants.UNIVERSE_PATH
                .resolve("SimpleScripting")
                .resolve("mods");
    }
    
    /**
     * Gets the script loader for reload functionality.
     * @return The script loader
     */
    public ScriptLoader getScriptLoader() {
        return scriptLoader;
    }
    
    /**
     * Gets the script registry.
     * @return The script registry
     */
    public ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }
    
    /**
     * Reloads all scripts.
     * @return Number of scripts reloaded
     */
    public int reloadScripts() {
        return scriptLoader.reloadAllScripts();
    }
}
