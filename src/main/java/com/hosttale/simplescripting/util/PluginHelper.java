package com.hosttale.simplescripting.util;

import com.hosttale.simplescripting.SimpleScriptingPlugin;

/**
 * Helper class for plugin-level operations exposed to JavaScript.
 * Provides script reload functionality and plugin information.
 */
public class PluginHelper {
    private final SimpleScriptingPlugin plugin;
    private final Logger logger;

    public PluginHelper(SimpleScriptingPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * Reloads all JavaScript scripts.
     * @return Number of scripts reloaded
     */
    public int reload() {
        logger.info("Reloading all scripts...");
        int count = plugin.reloadScripts();
        logger.info("Reloaded " + count + " scripts");
        return count;
    }

    /**
     * Gets the plugin version.
     * @return The plugin version string
     */
    public String getVersion() {
        return "1.0.0";
    }

    /**
     * Gets the plugin name.
     * @return The plugin name
     */
    public String getName() {
        return "SimpleScripting";
    }

    /**
     * Gets the scripts directory path.
     * @return Path to the scripts directory
     */
    public String getScriptsPath() {
        return "universe/SimpleScripting/mods/";
    }

    /**
     * Gets the database directory path.
     * @return Path to the database directory
     */
    public String getDatabasePath() {
        return "universe/SimpleScripting/db/";
    }

    /**
     * Gets information about the plugin.
     * @return Plugin info string
     */
    public String getInfo() {
        return getName() + " v" + getVersion() + " - JavaScript scripting for Hytale servers";
    }
}
