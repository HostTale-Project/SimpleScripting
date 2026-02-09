package com.hosttale.simplescripting.mod.runtime.api.worlds;

import com.hosttale.simplescripting.mod.runtime.api.common.ApiUtils;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.Collections;
import java.util.List;

public final class WorldsApi {

    private final HytaleLogger logger;
    private final PlayersApi playersApi;
    private final com.hosttale.simplescripting.mod.runtime.JsModRuntime runtime;

    public WorldsApi(HytaleLogger logger, PlayersApi playersApi, com.hosttale.simplescripting.mod.runtime.JsModRuntime runtime) {
        this.logger = logger.getSubLogger("worlds");
        this.playersApi = playersApi;
        this.runtime = runtime;
    }

    public Scriptable list() {
        return ApiUtils.withUniverse(logger, "list worlds",
                universe -> toJsArray(universe.getWorlds().keySet().stream()
                        .map(Object::toString)
                        .toList()),
                () -> toJsArray(Collections.emptyList()));
    }

    public WorldHandle get(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return ApiUtils.withUniverse(logger, "get world " + name,
                universe -> universe.isWorldLoadable(name) ? new WorldHandle(name, playersApi) : null,
                () -> null);
    }

    public WorldHandle getDefaultWorld() {
        return ApiUtils.withUniverse(logger, "get default world",
                universe -> universe.getDefaultWorld() == null ? null
                        : new WorldHandle(universe.getDefaultWorld().getName(), playersApi),
                () -> null);
    }

    public boolean message(String worldName, Object text) {
        WorldHandle world = get(worldName);
        if (world == null) {
            return false;
        }
        world.sendMessage(text);
        return true;
    }

    public boolean hasWorld(String name) {
        return get(name) != null;
    }

    /**
     * Execute a callback on the world thread.
     * This is necessary for ECS operations (like ecs.getPosition) from scheduler threads.
     * 
     * @param worldName The world name, or null for default world
     * @param callback The JavaScript function to execute on the world thread
     */
    public void runOnWorldThread(String worldName, Function callback) {
        Universe universe = Universe.get();
        if (universe == null) {
            logger.atWarning().log("Universe not ready");
            return;
        }
        
        World world;
        if (worldName == null || worldName.isBlank()) {
            world = universe.getDefaultWorld();
        } else {
            world = universe.getWorld(worldName);
        }
        
        if (world == null) {
            logger.atWarning().log("World not found: %s", worldName);
            return;
        }
        
        world.execute(() -> {
            Context cx = Context.enter();
            try {
                callback.call(cx, runtime.getScope(), runtime.getScope(), new Object[]{});
            } catch (Exception e) {
                logger.atSevere().log("Error executing callback on world thread: %s", e.getMessage());
            } finally {
                Context.exit();
            }
        });
    }

    private Scriptable toJsArray(List<String> values) {
        return ApiUtils.withContext(runtime,
                () -> Context.getCurrentContext().newArray(runtime.getScope(), values.toArray(new Object[0])));
    }
}
