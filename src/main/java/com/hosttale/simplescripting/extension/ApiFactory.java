package com.hosttale.simplescripting.extension;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Factory for creating per-mod API instances.
 * Each JS mod gets its own API instance to maintain isolation.
 */
@FunctionalInterface
public interface ApiFactory {
    
    /**
     * Create an API instance for a specific mod.
     * 
     * @param modId The mod ID this API instance belongs to
     * @param runtime The mod's runtime (for context/scope access)
     * @param logger Logger for the mod
     * @return API instance to be injected into the mod's scope
     */
    Object createInstance(String modId, JsModRuntime runtime, HytaleLogger logger);
}
