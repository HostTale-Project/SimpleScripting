package com.hosttale.simplescripting.managers;

import com.hosttale.simplescripting.commands.CommandManager;
import com.hosttale.simplescripting.task.Scheduler;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking script resources.
 * Enables hot reload by tracking commands, events, and tasks per script.
 */
public class ScriptRegistry {
    // Track commands registered by each script
    private final Map<String, Set<String>> scriptCommands;
    
    // Track event handlers registered by each script
    private final Map<String, Map<String, Set<String>>> scriptEventHandlers;
    
    // Track scheduler tasks by each script
    private final Map<String, Set<Long>> scriptTasks;
    
    // Current script being loaded (set during script execution)
    private String currentScript;
    
    // References to managers for cleanup
    private CommandManager commandManager;
    private EventManager eventManager;
    private Scheduler scheduler;

    public ScriptRegistry() {
        this.scriptCommands = new ConcurrentHashMap<>();
        this.scriptEventHandlers = new ConcurrentHashMap<>();
        this.scriptTasks = new ConcurrentHashMap<>();
        this.currentScript = null;
    }

    /**
     * Sets the current script being loaded.
     * @param scriptName The script filename
     */
    public void setCurrentScript(@Nonnull String scriptName) {
        this.currentScript = scriptName;
        // Initialize tracking sets for this script
        scriptCommands.computeIfAbsent(scriptName, k -> ConcurrentHashMap.newKeySet());
        scriptEventHandlers.computeIfAbsent(scriptName, k -> new ConcurrentHashMap<>());
        scriptTasks.computeIfAbsent(scriptName, k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * Clears the current script context.
     */
    public void clearCurrentScript() {
        this.currentScript = null;
    }

    /**
     * Gets the current script name.
     * @return Current script name, or null if none
     */
    public String getCurrentScript() {
        return currentScript;
    }

    /**
     * Sets the manager references for cleanup.
     */
    public void setManagers(CommandManager commandManager, EventManager eventManager, Scheduler scheduler) {
        this.commandManager = commandManager;
        this.eventManager = eventManager;
        this.scheduler = scheduler;
    }

    /**
     * Records a command registration.
     * @param commandName The command name
     */
    public void recordCommand(@Nonnull String commandName) {
        if (currentScript != null) {
            scriptCommands.get(currentScript).add(commandName);
        }
    }

    /**
     * Records an event handler registration.
     * @param eventName The event name
     * @param handlerId The handler ID
     */
    public void recordEventHandler(@Nonnull String eventName, @Nonnull String handlerId) {
        if (currentScript != null) {
            scriptEventHandlers.get(currentScript)
                .computeIfAbsent(eventName, k -> ConcurrentHashMap.newKeySet())
                .add(handlerId);
        }
    }

    /**
     * Records a scheduled task.
     * @param taskId The task ID
     */
    public void recordTask(long taskId) {
        if (currentScript != null) {
            scriptTasks.get(currentScript).add(taskId);
        }
    }

    /**
     * Unregisters all resources for a specific script.
     * @param scriptName The script filename
     */
    public void unregisterScript(@Nonnull String scriptName) {
        // Unregister commands
        Set<String> commands = scriptCommands.remove(scriptName);
        if (commands != null && commandManager != null) {
            for (String cmd : commands) {
                commandManager.unregisterCommand(cmd);
            }
        }
        
        // Unregister event handlers
        Map<String, Set<String>> events = scriptEventHandlers.remove(scriptName);
        if (events != null && eventManager != null) {
            for (Map.Entry<String, Set<String>> entry : events.entrySet()) {
                String eventName = entry.getKey();
                for (String handlerId : entry.getValue()) {
                    eventManager.off(eventName, handlerId);
                }
            }
        }
        
        // Cancel scheduled tasks
        Set<Long> tasks = scriptTasks.remove(scriptName);
        if (tasks != null && scheduler != null) {
            for (Long taskId : tasks) {
                scheduler.cancel(taskId);
            }
        }
    }

    /**
     * Unregisters all resources for all scripts.
     */
    public void unregisterAll() {
        Set<String> allScripts = new HashSet<>(scriptCommands.keySet());
        for (String script : allScripts) {
            unregisterScript(script);
        }
        
        // Clear any remaining event handlers
        if (eventManager != null) {
            eventManager.clear();
        }
        
        // Cancel all remaining tasks
        if (scheduler != null) {
            scheduler.cancelAll();
        }
    }

    /**
     * Gets all registered script names.
     * @return Set of script names
     */
    public Set<String> getRegisteredScripts() {
        return new HashSet<>(scriptCommands.keySet());
    }

    /**
     * Gets the commands registered by a script.
     * @param scriptName The script filename
     * @return Set of command names, or empty set if none
     */
    public Set<String> getScriptCommands(@Nonnull String scriptName) {
        return new HashSet<>(scriptCommands.getOrDefault(scriptName, Collections.emptySet()));
    }

    /**
     * Gets the event handlers registered by a script.
     * @param scriptName The script filename
     * @return Map of event name to handler IDs
     */
    public Map<String, Set<String>> getScriptEventHandlers(@Nonnull String scriptName) {
        Map<String, Set<String>> handlers = scriptEventHandlers.get(scriptName);
        if (handlers == null) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : handlers.entrySet()) {
            result.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return result;
    }

    /**
     * Gets the scheduled tasks for a script.
     * @param scriptName The script filename
     * @return Set of task IDs
     */
    public Set<Long> getScriptTasks(@Nonnull String scriptName) {
        return new HashSet<>(scriptTasks.getOrDefault(scriptName, Collections.emptySet()));
    }

    /**
     * Gets summary information about all registered resources.
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptRegistry Summary:\n");
        
        for (String script : scriptCommands.keySet()) {
            Set<String> cmds = scriptCommands.get(script);
            Map<String, Set<String>> events = scriptEventHandlers.get(script);
            Set<Long> tasks = scriptTasks.get(script);
            
            int eventCount = events != null ? events.values().stream().mapToInt(Set::size).sum() : 0;
            int taskCount = tasks != null ? tasks.size() : 0;
            
            sb.append(String.format("  %s: %d commands, %d event handlers, %d tasks\n",
                script,
                cmds != null ? cmds.size() : 0,
                eventCount,
                taskCount));
        }
        
        return sb.toString();
    }
}
