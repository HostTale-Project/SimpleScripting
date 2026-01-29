package com.hosttale.simplescripting.mod.runtime.api.common;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import org.mozilla.javascript.Context;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared helpers for JS-facing APIs to avoid duplicated thread/universe/context checks.
 */
public final class ApiUtils {

    private ApiUtils() {
    }

    public static Universe universeOrNull(HytaleLogger logger, String action) {
        Universe universe = Universe.get();
        if (universe == null) {
            logger.atWarning().log("Universe not ready; cannot %s.", action);
        }
        return universe;
    }

    public static <T> T withUniverse(HytaleLogger logger, String action, Function<Universe, T> fn, Supplier<T> fallback) {
        Universe universe = universeOrNull(logger, action);
        if (universe == null) {
            return fallback.get();
        }
        try {
            return fn.apply(universe);
        } catch (Exception e) {
            logger.atWarning().log("Failed to %s: %s", action, e.getMessage());
            return fallback.get();
        }
    }

    public static <T> T withContext(JsModRuntime runtime, Supplier<T> supplier) {
        Context cx = Context.getCurrentContext();
        boolean entered = false;
        if (cx == null) {
            cx = runtime.enterContext();
            entered = true;
        }
        try {
            return supplier.get();
        } finally {
            if (entered) {
                Context.exit();
            }
        }
    }

    public static void withContext(JsModRuntime runtime, Runnable runnable) {
        withContext(runtime, () -> {
            runnable.run();
            return null;
        });
    }
}
