package com.hosttale.simplescripting.mod.runtime.api.net;

import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * High-level network helpers without exposing native packet types.
 */
public final class NetApi {

    private final PlayersApi playersApi;
    private final HytaleLogger logger;

    public NetApi(PlayersApi playersApi, HytaleLogger logger) {
        this.playersApi = playersApi;
        this.logger = logger.getSubLogger("net");
    }

    public void broadcast(Object text) {
        playersApi.broadcast(text);
    }

    public boolean send(String username, Object text) {
        return playersApi.message(username, text);
    }

    public boolean kick(String username, String reason) {
        return playersApi.disconnect(username, reason);
    }

    public void warn(String message) {
        logger.atWarning().log(message);
    }
}
