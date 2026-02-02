package com.hosttale.simplescripting;

import com.hosttale.simplescripting.commands.CreateModCommand;
import com.hosttale.simplescripting.commands.ScriptsCommand;
import com.hosttale.simplescripting.commands.UpdateTypesCommand;
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

    public SimpleScriptingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        modsRoot = getDataDirectory().resolve("mods-js");
        SampleModInstaller.installIfFirstRun(modsRoot, LOGGER, getClass().getClassLoader());
        modTemplateService = new ModTemplateService(modsRoot, getClass().getClassLoader(), LOGGER);
        JsPluginServices pluginServices = JsPluginServices.fromPlugin(this);
        jsModManager = new JsModManager(modsRoot, LOGGER, pluginServices);
        scriptBrowser = new ScriptBrowser(modsRoot, jsModManager, LOGGER);
    }

    @Override
    protected void start() {
        super.start();
        jsModManager.discoverAndLoadMods();
        registerCommands();
    }

    @Override
    protected void shutdown() {
        if (jsModManager != null) {
            jsModManager.disableAll();
        }
        LOGGER.atInfo().log("Shutting down plugin " + this.getName());
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new CreateModCommand(modTemplateService));
        getCommandRegistry().registerCommand(new UpdateTypesCommand(modTemplateService));
        getCommandRegistry().registerCommand(new ScriptsCommand(scriptBrowser));
    }
}
