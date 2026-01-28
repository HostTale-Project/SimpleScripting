package com.hosttale.simplescripting.mod.runtime.api.modules;

import com.hosttale.simplescripting.mod.JsModDefinition;
import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuleImports {

    private final JsModDefinition definition;
    private final HytaleLogger logger;
    private final JsModRuntime runtime;
    private final Map<Path, Object> cache = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Path>> loading = ThreadLocal.withInitial(HashSet::new);

    public ModuleImports(JsModDefinition definition, HytaleLogger logger, JsModRuntime runtime) {
        this.definition = definition;
        this.logger = logger.getSubLogger("imports");
        this.runtime = runtime;
    }

    public Object require(String requestedPath) {
        Path target = resolve(requestedPath);
        return cache.computeIfAbsent(target, this::evaluateModule);
    }

    private Path resolve(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("require() needs a relative path.");
        }
        if (requestedPath.contains("..")) {
            throw new IllegalArgumentException("require() cannot traverse outside the mod folder.");
        }
        if (requestedPath.startsWith("/") || requestedPath.startsWith("\\")) {
            throw new IllegalArgumentException("require() expects a relative path, not an absolute path.");
        }

        String normalized = requestedPath.endsWith(".js") ? requestedPath : requestedPath + ".js";
        Path root = definition.getRootDirectory().toAbsolutePath().normalize();
        Path candidate = root.resolve(normalized).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Import path must stay within the mod directory.");
        }
        if (!Files.exists(candidate)) {
            throw new IllegalArgumentException("Module not found: " + requestedPath);
        }
        return candidate;
    }

    private Object evaluateModule(Path path) {
        Set<Path> chain = loading.get();
        if (!chain.add(path)) {
            throw new IllegalStateException("Circular require detected at " + path.getFileName());
        }

        boolean entered = false;
        Context cx = Context.getCurrentContext();
        if (cx == null) {
            cx = runtime.enterContext();
            entered = true;
        }

        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            ScriptableObject moduleScope = (ScriptableObject) cx.newObject(runtime.getScope());
            moduleScope.setPrototype(runtime.getScope());
            moduleScope.setParentScope(runtime.getScope());

            Scriptable exports = cx.newObject(moduleScope);
            ScriptableObject.putProperty(moduleScope, "exports", exports);
            ScriptableObject.putProperty(moduleScope, "module", Context.javaToJS(new Module(exports), moduleScope));
            ScriptableObject.putProperty(moduleScope, "require", new RequireFunction(this));

            cx.evaluateReader(moduleScope, reader, path.getFileName().toString(), 1, null);
            Object exported = ScriptableObject.getProperty(moduleScope, "exports");
            if (exported == Scriptable.NOT_FOUND) {
                return exports;
            }
            return exported;
        } catch (IOException e) {
            logger.atSevere().log("Failed to load %s: %s", path.getFileName(), e.getMessage());
            throw new RuntimeException("Failed to load " + path.getFileName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.atSevere().log("Error in %s: %s", path.getFileName(), e.getMessage());
            throw new RuntimeException("Error evaluating " + path.getFileName() + ": " + e.getMessage(), e);
        } finally {
            chain.remove(path);
            if (entered) {
                Context.exit();
            }
        }
    }

    private record Module(Scriptable exports) {
        public Scriptable getExports() {
            return exports;
        }
    }

    public static final class RequireFunction extends BaseFunction {
        private final ModuleImports delegate;

        public RequireFunction(ModuleImports delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException("require() needs a relative path.");
            }
            Object path = args[0];
            return delegate.require(path == null ? null : path.toString());
        }
    }
}
