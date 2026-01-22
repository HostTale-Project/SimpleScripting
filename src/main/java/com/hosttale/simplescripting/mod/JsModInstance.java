package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;

public final class JsModInstance {

    private final JsModDefinition definition;
    private final JsModRuntime runtime;
    private final HytaleLogger logger;
    private final SharedServiceRegistry sharedServiceRegistry;
    private boolean enabled;

    public JsModInstance(JsModDefinition definition, JsModRuntime runtime, HytaleLogger logger, SharedServiceRegistry sharedServiceRegistry) {
        this.definition = definition;
        this.runtime = runtime;
        this.logger = logger.getSubLogger(definition.getManifest().getId());
        this.sharedServiceRegistry = sharedServiceRegistry;
    }

    public void loadAndEnable() throws IOException {
        runtime.load();
        runtime.enable();
        enabled = true;
        logger.atInfo().log("Enabled JS mod: %s (%s)", definition.getManifest().getName(), definition.getManifest().getVersion());
    }

    public void disable() {
        sharedServiceRegistry.removeOwner(definition.getManifest().getId());
        runtime.disable();
        enabled = false;
        logger.atInfo().log("Disabled JS mod: %s", definition.getManifest().getName());
    }

    public void reload() throws IOException {
        sharedServiceRegistry.removeOwner(definition.getManifest().getId());
        runtime.reload();
        runtime.enable();
        enabled = true;
        logger.atInfo().log("Reloaded JS mod: %s", definition.getManifest().getName());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JsModDefinition getDefinition() {
        return definition;
    }
}
