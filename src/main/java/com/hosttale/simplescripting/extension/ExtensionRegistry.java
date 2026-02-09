package com.hosttale.simplescripting.extension;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.JsPluginServices;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing SimpleScripting extension plugins.
 * Extensions can register global APIs, emit events, and interact with the JS environment.
 */
public final class ExtensionRegistry {
    
    private final List<SimpleScriptingExtension> extensions = new ArrayList<>();
    private final JsPluginServices pluginServices;
    private final HytaleLogger logger;
    private final ExtensionEventBus eventBus;
    private final Map<String, List<ApiRegistration>> globalApis = new LinkedHashMap<>();
    private boolean initialized = false;
    
    public ExtensionRegistry(JsPluginServices pluginServices, HytaleLogger logger) {
        this.pluginServices = pluginServices;
        this.logger = logger.getSubLogger("extensions");
        this.eventBus = new ExtensionEventBus(logger);
    }
    
    /**
     * Register an extension plugin.
     * Must be called before initializeExtensions().
     * 
     * @param extension Extension to register
     */
    public void registerExtension(SimpleScriptingExtension extension) {
        if (initialized) {
            throw new IllegalStateException("Cannot register extensions after initialization");
        }
        if (extension == null) {
            throw new IllegalArgumentException("Extension cannot be null");
        }
        
        String id = extension.getExtensionId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Extension ID cannot be null or blank");
        }
        
        // Check for duplicate IDs
        for (SimpleScriptingExtension existing : extensions) {
            if (existing.getExtensionId().equals(id)) {
                logger.atWarning().log("Extension %s already registered, skipping duplicate", id);
                return;
            }
        }
        
        extensions.add(extension);
        extensions.sort(Comparator.comparingInt(SimpleScriptingExtension::getPriority));
        logger.atInfo().log("Registered extension: %s (priority: %d)",
                extension.getExtensionId(), extension.getPriority());
    }
    
    /**
     * Initialize all registered extensions.
     * Calls onRegister() for each extension in priority order.
     * Should be called after all extensions are registered but before mods are loaded.
     */
    public void initializeExtensions() {
        if (initialized) {
            logger.atWarning().log("Extensions already initialized");
            return;
        }
        
        initialized = true;
        logger.atInfo().log("Initializing %d extensions", extensions.size());
        
        for (SimpleScriptingExtension extension : extensions) {
            try {
                ExtensionContextImpl context = new ExtensionContextImpl(
                        extension.getExtensionId(),
                        this,
                        pluginServices,
                        logger.getSubLogger(extension.getExtensionId())
                );
                extension.onRegister(context);
                logger.atInfo().log("Initialized extension: %s", extension.getExtensionId());
            } catch (Exception e) {
                logger.atSevere().log("Failed to initialize extension %s: %s",
                        extension.getExtensionId(), e.getMessage());
            }
        }
        
        logger.atInfo().log("Extension initialization complete. %d APIs registered.", globalApis.size());
    }
    
    /**
     * Inject APIs from all extensions into a mod's runtime.
     * Called during mod initialization.
     * 
     * @param runtime Mod's runtime
     * @param scope Mod's scope
     */
    public void injectApisIntoMod(JsModRuntime runtime, ScriptableObject scope) {
        String modId = runtime.getDefinition().getManifest().getId();
        HytaleLogger modLogger = runtime.getLogger();
        
        for (Map.Entry<String, List<ApiRegistration>> entry : globalApis.entrySet()) {
            String apiName = entry.getKey();
            List<ApiRegistration> registrations = entry.getValue();
            
            // If multiple extensions register same name, last wins
            // Could implement merge/override logic here if needed
            ApiRegistration registration = registrations.get(registrations.size() - 1);
            
            if (registrations.size() > 1) {
                logger.atWarning().log("Multiple extensions registered API '%s', using extension '%s'",
                        apiName, registration.extensionId);
            }
            
            try {
                Object apiInstance = registration.factory.createInstance(modId, runtime, modLogger);
                runtime.defineConstant(apiName, Context.javaToJS(apiInstance, scope));
                logger.atFine().log("Injected API '%s' into mod %s", apiName, modId);
            } catch (Exception e) {
                logger.atSevere().log("Failed to inject API '%s' into mod %s: %s",
                        apiName, modId, e.getMessage());
            }
        }
    }
    
    /**
     * Disable all extensions and clean up resources.
     * Called during plugin shutdown.
     */
    public void disableAll() {
        logger.atInfo().log("Disabling %d extensions", extensions.size());
        
        // Disable in reverse order
        List<SimpleScriptingExtension> reversed = new ArrayList<>(extensions);
        Collections.reverse(reversed);
        
        for (SimpleScriptingExtension extension : reversed) {
            try {
                extension.onDisable();
                logger.atInfo().log("Disabled extension: %s", extension.getExtensionId());
            } catch (Exception e) {
                logger.atSevere().log("Error disabling extension %s: %s",
                        extension.getExtensionId(), e.getMessage());
            }
        }
        
        extensions.clear();
        globalApis.clear();
        eventBus.shutdown();
        initialized = false;
    }
    
    /**
     * Get the extension event bus.
     */
    public ExtensionEventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * Get list of registered extensions.
     */
    public List<SimpleScriptingExtension> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }
    
    /**
     * Check if extensions have been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Collect all TypeScript type definitions from registered extensions.
     * Returns a map of extension ID to type definition content.
     * 
     * @return Map of extension IDs to their TypeScript definitions (non-null, non-empty only)
     */
    public Map<String, String> collectTypeDefinitions() {
        Map<String, String> types = new LinkedHashMap<>();
        
        for (SimpleScriptingExtension extension : extensions) {
            String typeDefs = extension.getTypeDefinitions();
            if (typeDefs != null && !typeDefs.isBlank()) {
                types.put(extension.getExtensionId(), typeDefs);
            }
        }
        
        return types;
    }
    
    /**
     * Collect all example mod resource paths from registered extensions.
     * Returns a map of extension ID to array of resource paths.
     * 
     * @return Map of extension IDs to their example resource paths
     */
    public Map<String, String[]> collectExampleMods() {
        Map<String, String[]> examples = new LinkedHashMap<>();
        
        for (SimpleScriptingExtension extension : extensions) {
            String[] examplePaths = extension.getExampleModPaths();
            if (examplePaths != null && examplePaths.length > 0) {
                examples.put(extension.getExtensionId(), examplePaths);
            }
        }
        
        return examples;
    }
    
    /**
     * Internal method for extensions to register APIs.
     * Called via ExtensionContext.
     */
    void registerApiInternal(String extensionId, String name, ApiFactory factory) {
        if (!initialized) {
            throw new IllegalStateException("Cannot register API before initialization");
        }
        
        globalApis.computeIfAbsent(name, k -> new ArrayList<>())
                .add(new ApiRegistration(extensionId, name, factory));
        
        logger.atInfo().log("Extension %s registered API: %s", extensionId, name);
    }
    
    /**
     * Internal record for tracking API registrations.
     */
    private record ApiRegistration(String extensionId, String apiName, ApiFactory factory) {
    }
}
