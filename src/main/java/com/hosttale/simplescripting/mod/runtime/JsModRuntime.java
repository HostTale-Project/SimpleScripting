package com.hosttale.simplescripting.mod.runtime;

import com.hosttale.simplescripting.mod.JsModDefinition;
import com.hosttale.simplescripting.mod.SharedServiceRegistry;
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

    private final JsModDefinition definition;
    private final HytaleLogger logger;
    private final SharedServiceRegistry sharedServiceRegistry;

    private Context context;
    private ScriptableObject scope;
    private Function onEnable;
    private Function onDisable;
    private Function onReload;
    private boolean loaded;

    public JsModRuntime(JsModDefinition definition, HytaleLogger logger, SharedServiceRegistry sharedServiceRegistry) {
        this.definition = definition;
        this.logger = logger.getSubLogger("mod-" + definition.getManifest().getId());
        this.sharedServiceRegistry = sharedServiceRegistry;
    }

    public void load() throws IOException {
        if (loaded) {
            return;
        }

        Path entrypoint = definition.getEntrypoint();
        boolean success = false;
        context = Context.enter();
        try {
            context.setLanguageVersion(Context.VERSION_ES6);
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
        Scriptable modInfo = Context.toObject(definition.getManifest(), scope);
        ScriptableObject.putProperty(scope, "modManifest", modInfo);
        ScriptableObject.putProperty(scope, "console", Context.javaToJS(new LoggerBridge(logger), scope));
        ScriptableObject.putProperty(scope, "SharedServices", Context.javaToJS(
                new SharedServicesApi(definition.getManifest().getId(), sharedServiceRegistry, this, logger),
                scope
        ));
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
        close();
    }

    public void reload() throws IOException {
        if (onReload != null) {
            invokeHook(onReload, "onReload");
        }
        close();
        loaded = false;
        load();
    }

    @Override
    public void close() {
        if (context != null) {
            // Ensure the context is current on this thread before exiting to avoid Rhino assertions.
            if (Context.getCurrentContext() != context) {
                Context.enter(context);
            }
            Context.exit();
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

    public Object invokeFunction(Scriptable target, String methodName, Object[] args) {
        if (target == null || methodName == null) {
            throw new IllegalArgumentException("Missing target or method name.");
        }
        Context cx = Context.enter(context);
        try {
            Object value = ScriptableObject.getProperty(target, methodName);
            if (!(value instanceof Function function)) {
                throw new IllegalArgumentException("Method " + methodName + " not found on service.");
            }
            return function.call(cx, scope, target, args);
        } finally {
            Context.exit();
        }
    }

    private void invokeHook(Function hook, String hookName) {
        Context cx = Context.enter(context);
        try {
            hook.call(cx, scope, scope, Context.emptyArgs);
        } catch (Exception e) {
            logger.atSevere().log("Error during %s for mod %s: %s", hookName, definition.getManifest().getId(), e.getMessage());
        } finally {
            Context.exit();
        }
    }

    private void cleanupState() {
        onEnable = null;
        onDisable = null;
        onReload = null;
        scope = null;
        context = null;
        loaded = false;
    }

    private IOException toIOException(Exception e, String message) {
        if (e instanceof IOException ioException) {
            return ioException;
        }
        return new IOException(message, e);
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
