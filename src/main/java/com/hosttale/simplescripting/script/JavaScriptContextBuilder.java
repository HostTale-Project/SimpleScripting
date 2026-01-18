package com.hosttale.simplescripting.script;

import com.hosttale.simplescripting.SimpleScriptingPlugin;
import com.hosttale.simplescripting.api.Colors;
import com.hosttale.simplescripting.commands.CommandManager;
import com.hosttale.simplescripting.managers.EventManager;
import com.hosttale.simplescripting.managers.ScriptRegistry;
import com.hosttale.simplescripting.task.Scheduler;
import com.hosttale.simplescripting.util.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Builds and configures JavaScript execution contexts.
 * Responsible for setting up the JavaScript scope with API objects.
 */
public class JavaScriptContextBuilder {
    private final JavaPlugin plugin;
    private final HytaleLogger logger;
    private final ScriptRegistry scriptRegistry;
    
    // Shared instances for hot reload support
    private CommandManager commandManager;
    private EventManager eventManager;
    private Scheduler scheduler;
    private Logger loggerInstance;

    public JavaScriptContextBuilder(JavaPlugin plugin, HytaleLogger logger, ScriptRegistry scriptRegistry) {
        this.plugin = plugin;
        this.logger = logger;
        this.scriptRegistry = scriptRegistry;
    }

    /**
     * Creates and configures a JavaScript scope with all necessary API objects.
     * @param context The JavaScript context to use
     * @return Configured scriptable scope
     */
    public Scriptable buildScope(Context context) {
        Scriptable scope = context.initStandardObjects();

        // Create Logger instance
        loggerInstance = new Logger(logger);
        
        // Create core API instances
        commandManager = new CommandManager((SimpleScriptingPlugin) plugin, scope, loggerInstance, scriptRegistry);
        eventManager = new EventManager((SimpleScriptingPlugin) plugin, scope, loggerInstance);
        scheduler = new Scheduler(scope, loggerInstance);
        
        // Create helper instances
        TeleportHelper teleportHelper = new TeleportHelper(loggerInstance);
        PlayerHelper playerHelper = new PlayerHelper(loggerInstance);
        playerHelper.setScope(scope); // Enable JavaScript callback execution on world thread
        WorldHelper worldHelper = new WorldHelper(loggerInstance);
        worldHelper.setScope(scope); // Enable JavaScript callback execution
        PermissionHelper permissionHelper = new PermissionHelper(loggerInstance);
        PluginHelper pluginHelper = new PluginHelper((SimpleScriptingPlugin) plugin, loggerInstance);
        
        // Set up script registry with managers for cleanup
        scriptRegistry.setManagers(commandManager, eventManager, scheduler);

        // Expose core APIs to JavaScript
        exposeApi(scope, "Universe", Universe.get());
        exposeApi(scope, "Plugin", pluginHelper);
        exposeApi(scope, "Logger", loggerInstance);
        exposeApi(scope, "Commands", commandManager);
        exposeApi(scope, "MessageHelper", new MessageHelper());
        exposeApi(scope, "DB", new DatabaseHelper());
        
        // Expose new helper APIs
        exposeApi(scope, "Teleport", teleportHelper);
        exposeApi(scope, "Players", playerHelper);
        exposeApi(scope, "Worlds", worldHelper);
        exposeApi(scope, "Scheduler", scheduler);
        exposeApi(scope, "Permissions", permissionHelper);
        exposeApi(scope, "Events", eventManager);
        exposeApi(scope, "Colors", Colors.COLOR_MAP);
        
        // Expose utility classes
        exposeApi(scope, "Transform", Transform.class);
        exposeApi(scope, "Vector3d", Vector3d.class);

        return scope;
    }
    
    /**
     * Gets the event manager for system registration.
     * @return The EventManager instance
     */
    public EventManager getEventManager() {
        return eventManager;
    }
    
    /**
     * Gets the scheduler for shutdown.
     * @return The Scheduler instance
     */
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * Gets the command manager.
     * @return The CommandManager instance
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    /**
     * Shuts down all managed resources.
     */
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (eventManager != null) {
            eventManager.clear();
        }
    }

    /**
     * Exposes a Java object to the JavaScript scope.
     * @param scope The JavaScript scope
     * @param name The name to use in JavaScript
     * @param javaObject The Java object to expose
     */
    private void exposeApi(Scriptable scope, String name, Object javaObject) {
        Object jsObject = Context.javaToJS(javaObject, scope);
        ScriptableObject.putProperty(scope, name, jsObject);
    }
}
