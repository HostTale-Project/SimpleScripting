package com.hosttale.simplescripting.mod.runtime;

import com.hosttale.simplescripting.mod.JsModDefinition;
import com.hosttale.simplescripting.mod.SharedServiceRegistry;
import com.hosttale.simplescripting.mod.runtime.api.assets.AssetsApi;
import com.hosttale.simplescripting.mod.runtime.api.commands.CommandsApi;
import com.hosttale.simplescripting.mod.runtime.api.database.DatabaseApi;
import com.hosttale.simplescripting.mod.runtime.api.events.EventCatalog;
import com.hosttale.simplescripting.mod.runtime.api.events.EventsApi;
import com.hosttale.simplescripting.mod.runtime.api.modules.ModuleImports;
import com.hosttale.simplescripting.mod.runtime.api.net.NetApi;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hosttale.simplescripting.mod.runtime.api.server.ServerApi;
import com.hosttale.simplescripting.mod.runtime.api.ui.UiApi;
import com.hosttale.simplescripting.mod.runtime.api.worlds.WorldsApi;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsModRuntime implements AutoCloseable {

    private static final int LANGUAGE_VERSION = Context.VERSION_ES6;

    private final JsModDefinition definition;
    private final HytaleLogger logger;
    private final SharedServiceRegistry sharedServiceRegistry;
    private final JsPluginServices pluginServices;
    private final ModRegistrationTracker registrationTracker = new ModRegistrationTracker();

    private Context context;
    private ScriptableObject scope;
    private Function onEnable;
    private Function onDisable;
    private Function onReload;
    private boolean loaded;
    private ModuleImports moduleImports;
    private DatabaseApi databaseApi;

    public JsModRuntime(JsModDefinition definition,
                        HytaleLogger logger,
                        SharedServiceRegistry sharedServiceRegistry,
                        JsPluginServices pluginServices) {
        this.definition = definition;
        this.logger = logger.getSubLogger("mod-" + definition.getManifest().getId());
        this.sharedServiceRegistry = sharedServiceRegistry;
        this.pluginServices = pluginServices;
    }

    public void load() throws IOException {
        if (loaded) {
            return;
        }

        Path entrypoint = definition.getEntrypoint();
        boolean success = false;
        context = Context.enter();
        try {
            context.setLanguageVersion(LANGUAGE_VERSION);
            scope = context.initSafeStandardObjects();
            injectGlobals();

            try (Reader reader = new InputStreamReader(Files.newInputStream(entrypoint), StandardCharsets.UTF_8)) {
                context.evaluateReader(scope, reader, entrypoint.getFileName().toString(), 1, null);
            }

            onEnable = extractHook("onEnable");
            onDisable = extractHook("onDisable");
            onReload = extractHook("onReload");
            loaded = true;
            success = true;
        } catch (Exception e) {
            logger.atSevere().log("Failed to load mod %s: %s", definition.getManifest().getId(), e.getMessage());
            throw toIOException(e, "Failed to evaluate entrypoint " + entrypoint);
        } finally {
            Context.exit();
            if (!success) {
                cleanupState();
            }
        }
    }

    private void injectGlobals() {
        String modId = definition.getManifest().getId();
        Scriptable modInfo = Context.toObject(definition.getManifest(), scope);
        defineConstant("modManifest", modInfo);

        LoggerBridge loggerBridge = new LoggerBridge(logger);
        defineConstant("console", Context.javaToJS(loggerBridge, scope));
        defineConstant("log", Context.javaToJS(loggerBridge, scope));

        SharedServicesApi sharedServicesApi = new SharedServicesApi(modId, sharedServiceRegistry, this, logger);
        defineConstant("SharedServices", Context.javaToJS(sharedServicesApi, scope));

        PlayersApi playersApi = new PlayersApi(logger);
        EventCatalog eventCatalog = new EventCatalog(logger);
        defineConstant("events", Context.javaToJS(
                new EventsApi(modId, pluginServices.getEventRegistry(), this, registrationTracker, logger, eventCatalog),
                scope));
        defineConstant("commands", Context.javaToJS(
                new CommandsApi(modId, pluginServices.getCommandRegistry(), this, registrationTracker, logger, playersApi),
                scope));
        defineConstant("players", Context.javaToJS(playersApi, scope));
        defineConstant("worlds", Context.javaToJS(new WorldsApi(logger, playersApi, this), scope));
        defineConstant("server", Context.javaToJS(
                new ServerApi(modId, pluginServices.getTaskRegistry(), registrationTracker, this, logger),
                scope));
        defineConstant("net", Context.javaToJS(new NetApi(playersApi, logger), scope));
        defineConstant("assets", Context.javaToJS(new AssetsApi(logger), scope));
        defineConstant("ui", Context.javaToJS(new UiApi(), scope));
        databaseApi = new DatabaseApi(modId, this, logger);
        defineConstant("db", Context.javaToJS(databaseApi, scope));
        ScriptableObject storage = (ScriptableObject) context.newObject(scope);
        ScriptableObject.defineProperty(storage, "db", databaseApi,
                ScriptableObject.DONTENUM | ScriptableObject.PERMANENT | ScriptableObject.READONLY);
        defineConstant("storage", storage);

        moduleImports = new ModuleImports(definition, logger, this);
        defineConstant("require", new ModuleImports.RequireFunction(moduleImports));
    }

    private void defineConstant(String name, Object value) {
        ScriptableObject.defineProperty(scope, name, value,
                ScriptableObject.DONTENUM | ScriptableObject.PERMANENT | ScriptableObject.READONLY);
    }

    private Function extractHook(String name) {
        Object maybeFn = ScriptableObject.getProperty(scope, name);
        if (maybeFn instanceof Function function) {
            return function;
        }
        return null;
    }

    public void enable() {
        if (!loaded) {
            throw new IllegalStateException("Mod runtime must be loaded before enable.");
        }
        if (onEnable != null) {
            invokeHook(onEnable, "onEnable");
        }
    }

    public void disable() {
        if (!loaded) {
            return;
        }
        if (onDisable != null) {
            invokeHook(onDisable, "onDisable");
        }
        registrationTracker.clearAll(logger);
        close();
    }

    public void reload() throws IOException {
        if (onReload != null) {
            invokeHook(onReload, "onReload");
        }
        registrationTracker.clearAll(logger);
        close();
        loaded = false;
        load();
    }

    @Override
    public void close() {
        registrationTracker.clearAll(logger);
        if (databaseApi != null) {
            databaseApi.close();
            databaseApi = null;
        }
        if (context != null) {
            context = null;
        }
        onEnable = null;
        onDisable = null;
        onReload = null;
        scope = null;
        loaded = false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public HytaleLogger getLogger() {
        return logger;
    }

    public Context getContext() {
        return context;
    }

    public ScriptableObject getScope() {
        return scope;
    }

    public ModRegistrationTracker getRegistrationTracker() {
        return registrationTracker;
    }

    public Object callFunction(Function function, Object... args) {
        if (function == null) {
            throw new IllegalArgumentException("Handler must be a function.");
        }
        if (scope == null) {
            throw new IllegalStateException("JS context is not active for mod " + definition.getManifest().getId());
        }

        Context cx = Context.getCurrentContext();
        boolean entered = false;
        if (cx == null) {
            cx = enterContext();
            entered = true;
        }
        try {
            Object[] safeArgs;
            if (args == null || args.length == 0) {
                safeArgs = Context.emptyArgs;
            } else if (args.length == 1 && args[0] instanceof Object[] nested) {
                safeArgs = nested;
            } else {
                safeArgs = args;
            }
            return function.call(cx, scope, scope, safeArgs);
        } finally {
            if (entered) {
                Context.exit();
            }
        }
    }

    public Object invokeFunction(Scriptable target, String methodName, Object[] args) {
        if (target == null || methodName == null) {
            throw new IllegalArgumentException("Missing target or method name.");
        }
        Context cx = Context.getCurrentContext();
        boolean entered = false;
        if (cx == null) {
            cx = enterContext();
            entered = true;
        }
        try {
            Object value = ScriptableObject.getProperty(target, methodName);
            if (!(value instanceof Function function)) {
                throw new IllegalArgumentException("Method " + methodName + " not found on service.");
            }
            return function.call(cx, scope, target, args);
        } finally {
            if (entered) {
                Context.exit();
            }
        }
    }

    private void invokeHook(Function hook, String hookName) {
        try {
            callFunction(hook, Context.emptyArgs);
        } catch (Exception e) {
            logger.atSevere().log("Error during %s for mod %s: %s", hookName, definition.getManifest().getId(), e.getMessage());
        }
    }

    private void cleanupState() {
        registrationTracker.clearAll(logger);
        if (databaseApi != null) {
            databaseApi.close();
        }
        onEnable = null;
        onDisable = null;
        onReload = null;
        scope = null;
        context = null;
        moduleImports = null;
        databaseApi = null;
        loaded = false;
    }

    private IOException toIOException(Exception e, String message) {
        if (e instanceof IOException ioException) {
            return ioException;
        }
        return new IOException(message, e);
    }

    public Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(LANGUAGE_VERSION);
        return cx;
    }

    public static final class LoggerBridge {
        private final HytaleLogger logger;

        public LoggerBridge(HytaleLogger logger) {
            this.logger = logger;
        }

        public void info(String message) {
            logger.atInfo().log(message);
        }

        public void warn(String message) {
            logger.atWarning().log(message);
        }

        public void error(String message) {
            logger.atSevere().log(message);
        }
    }
}
