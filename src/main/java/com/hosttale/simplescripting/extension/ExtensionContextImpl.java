package com.hosttale.simplescripting.extension;

import com.hosttale.simplescripting.mod.runtime.JsPluginServices;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Implementation of ExtensionContext.
 */
final class ExtensionContextImpl implements ExtensionContext {
    
    private final String extensionId;
    private final ExtensionRegistry registry;
    private final JsPluginServices pluginServices;
    private final HytaleLogger logger;
    
    ExtensionContextImpl(String extensionId,
                        ExtensionRegistry registry,
                        JsPluginServices pluginServices,
                        HytaleLogger logger) {
        this.extensionId = extensionId;
        this.registry = registry;
        this.pluginServices = pluginServices;
        this.logger = logger;
    }
    
    @Override
    public void registerGlobalApi(String name, ApiFactory factory) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("API name cannot be null or blank");
        }
        if (factory == null) {
            throw new IllegalArgumentException("API factory cannot be null");
        }
        registry.registerApiInternal(extensionId, name, factory);
    }
    
    @Override
    public ExtensionEventBus getEventBus() {
        return registry.getEventBus();
    }
    
    @Override
    public JsPluginServices getPluginServices() {
        return pluginServices;
    }
    
    @Override
    public HytaleLogger getLogger() {
        return logger;
    }
    
    @Override
    public String getExtensionId() {
        return extensionId;
    }
}
