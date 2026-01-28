package com.hosttale.simplescripting.mod.runtime.api.players;

import com.hosttale.simplescripting.mod.runtime.api.common.ApiUtils;
import com.hosttale.simplescripting.mod.runtime.api.ui.UiMessageRenderer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PlayersApi {

    private final HytaleLogger logger;

    public PlayersApi(HytaleLogger logger) {
        this.logger = logger.getSubLogger("players");
    }

    public List<PlayerHandle> all() {
        return ApiUtils.withUniverse(logger, "list players",
                universe -> universe.getPlayers().stream().map(this::wrap).toList(),
                Collections::emptyList);
    }

    public PlayerHandle find(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return ApiUtils.withUniverse(logger, "find player " + username, universe -> {
            PlayerRef ref = universe.getPlayerByUsername(username, NameMatching.EXACT_IGNORE_CASE);
            return ref == null ? null : wrap(ref);
        }, () -> null);
    }

    public int count() {
        return ApiUtils.withUniverse(logger, "count players", Universe::getPlayerCount, () -> 0);
    }

    public boolean message(String username, Object text) {
        PlayerHandle player = find(username);
        if (player == null) {
            return false;
        }
        player.sendMessage(text);
        return true;
    }

    public void broadcast(Object text) {
        ApiUtils.withUniverse(logger, "broadcast message",
                universe -> {
                    universe.sendMessage(UiMessageRenderer.toMessage(text));
                    return null;
                },
                () -> null);
    }

    public boolean disconnect(String username, String reason) {
        PlayerHandle player = find(username);
        if (player == null) {
            return false;
        }
        player.kick(reason);
        return true;
    }

    public PlayerHandle require(String username) {
        PlayerHandle ref = find(username);
        if (ref == null) {
            throw new IllegalArgumentException("Player '" + username + "' is not online.");
        }
        return ref;
    }

    public List<String> names() {
        return all().stream()
                .map(PlayerHandle::getUsername)
                .map(name -> name == null ? "" : name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
    }

    public PlayerHandle wrap(PlayerRef ref) {
        return new PlayerHandle(ref);
    }
}
