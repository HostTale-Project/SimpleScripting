package com.hosttale.simplescripting.mod.runtime;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.registry.AssetRegistry;
import com.hypixel.hytale.server.core.task.TaskRegistry;
import com.hypixel.hytale.event.EventRegistry;

/**
 * Captures the plugin-provided services that JS mods should be able to reach without exposing the entire plugin.
 */
public final class JsPluginServices {

    private final CommandRegistry commandRegistry;
    private final EventRegistry eventRegistry;
    private final TaskRegistry taskRegistry;
    private final AssetRegistry assetRegistry;
    private final HytaleLogger logger;

    private JsPluginServices(
            CommandRegistry commandRegistry,
            EventRegistry eventRegistry,
            TaskRegistry taskRegistry,
            AssetRegistry assetRegistry,
            HytaleLogger logger
    ) {
        this.commandRegistry = commandRegistry;
        this.eventRegistry = eventRegistry;
        this.taskRegistry = taskRegistry;
        this.assetRegistry = assetRegistry;
        this.logger = logger;
    }

    public static JsPluginServices fromPlugin(PluginBase plugin) {
        return new JsPluginServices(
                plugin.getCommandRegistry(),
                plugin.getEventRegistry(),
                plugin.getTaskRegistry(),
                plugin.getAssetRegistry(),
                plugin.getLogger()
        );
    }

    public static JsPluginServices of(CommandRegistry commandRegistry,
                                      EventRegistry eventRegistry,
                                      TaskRegistry taskRegistry,
                                      AssetRegistry assetRegistry,
                                      HytaleLogger logger) {
        return new JsPluginServices(commandRegistry, eventRegistry, taskRegistry, assetRegistry, logger);
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public TaskRegistry getTaskRegistry() {
        return taskRegistry;
    }

    public AssetRegistry getAssetRegistry() {
        return assetRegistry;
    }

    public HytaleLogger getLogger() {
        return logger;
    }
}
