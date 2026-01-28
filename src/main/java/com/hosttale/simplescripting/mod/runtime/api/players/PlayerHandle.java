package com.hosttale.simplescripting.mod.runtime.api.players;

import com.hosttale.simplescripting.mod.runtime.api.ui.UiMessageRenderer;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Locale;
import java.util.UUID;

/**
 * JS-friendly wrapper around PlayerRef to avoid exposing native classes.
 */
public final class PlayerHandle {

    private final PlayerRef playerRef;

    public PlayerHandle(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public String getUsername() {
        return playerRef.getUsername();
    }

    public String getId() {
        UUID uuid = playerRef.getUuid();
        return uuid == null ? "" : uuid.toString();
    }

    public String getLanguage() {
        return playerRef.getLanguage();
    }

    public void setLanguage(String language) {
        if (language != null) {
            playerRef.setLanguage(language.toLowerCase(Locale.ROOT));
        }
    }

    public boolean isOnline() {
        return playerRef.isValid();
    }

    public void sendMessage(Object text) {
        playerRef.sendMessage(UiMessageRenderer.toMessage(text));
    }

    public void kick(String reason) {
        String message = (reason == null || reason.isBlank()) ? "Disconnected by server" : reason;
        playerRef.getPacketHandler().disconnect(message);
    }

    public String getWorldName() {
        return playerRef.getWorldUuid() == null ? "" : playerRef.getWorldUuid().toString();
    }
}
