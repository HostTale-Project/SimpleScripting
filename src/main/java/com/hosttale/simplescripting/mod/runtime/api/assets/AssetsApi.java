package com.hosttale.simplescripting.mod.runtime.api.assets;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Placeholder wrapper to avoid exposing raw AssetRegistry until a safe facade is designed.
 */
public final class AssetsApi {

    private final HytaleLogger logger;

    public AssetsApi(HytaleLogger logger) {
        this.logger = logger.getSubLogger("assets");
    }

    public void info(String message) {
        logger.atInfo().log(message);
    }

    public void warnUnsupported() {
        logger.atWarning().log("Asset registry is not exposed to JS yet.");
    }
}
