package com.hosttale.simplescripting.mod.runtime.api.economy;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import java.util.UUID;

/**
 * Abstract base class for economy providers.
 * Provides common functionality for plugin detection, UUID handling, and error logging.
 */
public abstract class BaseEconomyProvider implements EconomyProvider {
    
    protected final HytaleLogger logger;
    protected boolean initialized = false;
    
    protected BaseEconomyProvider(HytaleLogger logger) {
        this.logger = logger;
    }
    
    /**
     * Get the plugin identifier string (e.g., "TheNewEconomy:VaultUnlocked").
     */
    protected abstract String getPluginIdentifier();
    
    /**
     * Attempt to initialize the provider after plugin is found.
     * @return true if initialization successful
     */
    protected abstract boolean initializeProvider() throws Exception;
    
    /**
     * Check if provider is currently ready.
     */
    protected abstract boolean isReady();
    
    @Override
    public final boolean isAvailable() {
        if (isReady()) {
            return true;
        }
        
        if (initialized) {
            return false; // Already tried and failed
        }
        
        initialized = true;
        
        try {
            PluginManager pm = PluginManager.get();
            if (pm == null) {
                logger.atInfo().log("PluginManager not available");
                return false;
            }
            
            PluginIdentifier pluginId = PluginIdentifier.fromString(getPluginIdentifier());
            if (pm.getPlugin(pluginId) == null) {
                logger.atInfo().log(getName() + " plugin not found");
                return false;
            }
            
            logger.atInfo().log(getName() + " plugin found, attempting to initialize");
            return initializeProvider();
            
        } catch (ClassNotFoundException e) {
            logger.atInfo().log(getName() + " classes not found");
        } catch (Exception e) {
            logger.atWarning().log("Failed to initialize " + getName() + " economy: " + e.getClass().getName() + " - " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Parse UUID string with proper error handling.
     */
    protected UUID parseUuid(String playerUuid) throws IllegalArgumentException {
        return UUID.fromString(playerUuid);
    }
    
    /**
     * Execute an economy operation with standardized error handling.
     */
    protected <T> T executeOperation(String operation, String playerUuid, OperationCallback<T> callback, T defaultValue) {
        if (!isReady()) {
            return defaultValue;
        }
        
        try {
            return callback.execute();
        } catch (IllegalArgumentException e) {
            logger.atSevere().log("Invalid UUID for " + operation + ": " + playerUuid);
            return defaultValue;
        } catch (Exception e) {
            logger.atSevere().log("Error during " + operation + " for " + playerUuid + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    @FunctionalInterface
    protected interface OperationCallback<T> {
        T execute() throws Exception;
    }
}
