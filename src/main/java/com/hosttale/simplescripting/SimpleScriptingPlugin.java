package com.hosttale.simplescripting;

import com.hosttale.simplescripting.commands.CreateModCommand;
import com.hosttale.simplescripting.commands.ScriptsCommand;
import com.hosttale.simplescripting.commands.UpdateTypesCommand;
import com.hosttale.simplescripting.extension.ExtensionRegistry;
import com.hosttale.simplescripting.mod.JsModManager;
import com.hosttale.simplescripting.mod.ModTemplateService;
import com.hosttale.simplescripting.mod.SampleModInstaller;
import com.hosttale.simplescripting.mod.runtime.JsPluginServices;
import com.hosttale.simplescripting.scripts.ScriptBrowser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class SimpleScriptingPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Path modsRoot;
    private JsModManager jsModManager;
    private ModTemplateService modTemplateService;
    private ScriptBrowser scriptBrowser;
    private ExtensionRegistry extensionRegistry;
    private boolean coreExamplesInstalled = false;

    public SimpleScriptingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        modsRoot = getDataDirectory().resolve("mods-js");
        
        JsPluginServices pluginServices = JsPluginServices.fromPlugin(this);
        
        // Initialize extension registry
        extensionRegistry = new ExtensionRegistry(pluginServices, LOGGER);
        pluginServices.setExtensionRegistry(extensionRegistry);
        
        // Create ModTemplateService with extension registry
        modTemplateService = new ModTemplateService(modsRoot, getClass().getClassLoader(), LOGGER, extensionRegistry);
        
        // Install core sample mods (extensions not registered yet)
        coreExamplesInstalled = SampleModInstaller.installCoreExamplesIfFirstRun(modsRoot, LOGGER, getClass().getClassLoader(), modTemplateService);
        
        jsModManager = new JsModManager(modsRoot, LOGGER, pluginServices);
        scriptBrowser = new ScriptBrowser(modsRoot, jsModManager, LOGGER);
        
        LOGGER.atInfo().log("Extension registry initialized. Other plugins can now register extensions.");
    }

    @Override
    protected void start() {
        super.start();
        
        // Initialize extensions before loading mods
        extensionRegistry.initializeExtensions();
        
        // If core examples were just installed, update their types with extension definitions and install extension examples
        if (coreExamplesInstalled) {
            SampleModInstaller.updateCoreExampleTypes(modsRoot, LOGGER, modTemplateService);
            SampleModInstaller.installExtensionExamples(modsRoot, LOGGER, modTemplateService, extensionRegistry);
        }
        
        // Now load JS mods
        jsModManager.discoverAndLoadMods();
        registerCommands();
    }

    @Override
    protected void shutdown() {
        if (jsModManager != null) {
            jsModManager.disableAll();
        }
        if (extensionRegistry != null) {
            extensionRegistry.disableAll();
        }
        LOGGER.atInfo().log("Shutting down plugin " + this.getName());
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new CreateModCommand(modTemplateService));
        getCommandRegistry().registerCommand(new UpdateTypesCommand(modTemplateService));
        getCommandRegistry().registerCommand(new ScriptsCommand(scriptBrowser));
    }
    
    /**
     * Get the extension registry for other plugins to register extensions.
     * Should be called during plugin setup phase.
     */
    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }
}

