package com.hosttale.simplescripting.extension;

import com.hosttale.simplescripting.mod.runtime.JsPluginServices;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Context provided to extensions during registration.
 * Allows extensions to register APIs, access events, and interact with services.
 */
public interface ExtensionContext {
    
    /**
     * Register a global API that will be injected into every JS mod.
     * The factory is called once per mod to create an isolated API instance.
     * 
     * @param name Global variable name (e.g., "economy", "permissions")
     * @param factory Factory to create API instance per mod
     */
    void registerGlobalApi(String name, ApiFactory factory);
    
    /**
     * Get the extension event bus for cross-extension and extension-to-JS events.
     * Use this to emit events or listen to events from other extensions.
     */
    ExtensionEventBus getEventBus();
    
    /**
     * Get access to Hytale plugin services (commands, events, tasks, assets).
     */
    JsPluginServices getPluginServices();
    
    /**
     * Get logger for this extension.
     */
    HytaleLogger getLogger();
    
    /**
     * Get the extension ID this context belongs to.
     */
    String getExtensionId();
}
