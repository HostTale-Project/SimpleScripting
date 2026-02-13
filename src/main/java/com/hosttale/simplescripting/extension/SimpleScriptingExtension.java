package com.hosttale.simplescripting.extension;

/**
 * Interface for external plugins that extend SimpleScripting's JavaScript environment.
 * Extensions can register APIs, listen to events, and interact with the JS engine.
 */
public interface SimpleScriptingExtension {
    
    /**
     * Unique identifier for this extension.
     * Should follow pattern: lowercase alphanumeric with dashes (e.g., "economy-ss", "permissions-ext")
     */
    String getExtensionId();
    
    /**
     * Priority for loading order. Lower values load first.
     * Default: 100
     * Core extensions should use 0-50, third-party 50-100, optional features 100+
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Called when the extension should register its APIs and set up event listeners.
     * This is called after SimpleScripting setup but before JS mods are loaded.
     * 
     * @param context Context providing access to registration, events, and services
     */
    void onRegister(ExtensionContext context);
    
    /**
     * Called when the extension should clean up resources.
     * This is called during plugin shutdown.
     */
    void onDisable();
    
    /**
     * Provide TypeScript type definitions for this extension.
     * These will be merged with SimpleScripting's core types when creating/updating mods.
     * Return null or empty string if no types to provide.
     * 
     * @return TypeScript definition content, or null
     */
    default String getTypeDefinitions() {
        return null;
    }
    
    /**
     * Provide resource paths to example mods bundled with this extension.
     * Examples will be installed to the mods directory on first run.
     * Return empty array if no examples.
     * 
     * Example: return new String[]{"examples/my-example"};
     * This assumes resources at: src/main/resources/examples/my-example/mod.json, main.js, etc.
     * 
     * @return Array of resource paths (without trailing slash), or empty array
     */
    default String[] getExampleModPaths() {
        return new String[0];
    }
}
