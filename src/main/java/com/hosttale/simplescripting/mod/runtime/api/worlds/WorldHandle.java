package com.hosttale.simplescripting.mod.runtime.api.worlds;

import com.hosttale.simplescripting.mod.runtime.api.players.PlayerHandle;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hosttale.simplescripting.mod.runtime.api.ui.UiMessageRenderer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Collections;
import java.util.List;

/**
 * JS-friendly wrapper around a world name; resolves the world on use.
 */
public final class WorldHandle {

    private final String worldName;
    private final PlayersApi playersApi;

    WorldHandle(String worldName, PlayersApi playersApi) {
        this.worldName = worldName;
        this.playersApi = playersApi;
    }

    public String getName() {
        return worldName;
    }

    public boolean isLoaded() {
        return world() != null;
    }

    public List<PlayerHandle> players() {
        World world = world();
        if (world == null) {
            return Collections.emptyList();
        }
        return world.getPlayerRefs().stream()
                .map(playersApi::wrap)
                .toList();
    }

    public List<String> playerNames() {
        return players().stream().map(PlayerHandle::getUsername).toList();
    }

    public void sendMessage(Object text) {
        World world = world();
        if (world != null) {
            world.sendMessage(UiMessageRenderer.toMessage(text));
        }
    }

    private World world() {
        Universe universe = Universe.get();
        return universe == null ? null : universe.getWorld(worldName);
    }
}
