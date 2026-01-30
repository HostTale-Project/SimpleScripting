package com.hosttale.simplescripting.mod.runtime.api.players;

import com.hosttale.simplescripting.mod.runtime.api.ui.UiMessageRenderer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

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

    public void sendTitle(Object title) {
        sendTitle(title, null, false, EventTitleUtil.DEFAULT_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION, EventTitleUtil.DEFAULT_ZONE);
    }

    public void sendTitle(Object title, Object subtitle) {
        sendTitle(title, subtitle, false, EventTitleUtil.DEFAULT_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION, EventTitleUtil.DEFAULT_ZONE);
    }

    public void sendTitle(Object title, Object subtitle, boolean important, float durationSeconds, float fadeInSeconds, float fadeOutSeconds, String zone) {
        Message main = UiMessageRenderer.toMessage(title);
        Message sub = subtitle == null ? null : UiMessageRenderer.toMessage(subtitle);
        EventTitleUtil.showEventTitleToPlayer(playerRef, main, sub, important, zone, durationSeconds, fadeInSeconds, fadeOutSeconds);
    }

    public void hideTitle(float fadeOutSeconds) {
        EventTitleUtil.hideEventTitleFromPlayer(playerRef, fadeOutSeconds);
    }

    public void hideTitle() {
        EventTitleUtil.hideEventTitleFromPlayer(playerRef, EventTitleUtil.DEFAULT_FADE_DURATION);
    }

    public void kick(String reason) {
        String message = (reason == null || reason.isBlank()) ? "Disconnected by server" : reason;
        playerRef.getPacketHandler().disconnect(message);
    }

    public String getWorldName() {
        return playerRef.getWorldUuid() == null ? "" : playerRef.getWorldUuid().toString();
    }

    public PlayerRef unwrap() {
        return playerRef;
    }

    public Ref<EntityStore> getEntityRef() {
        return playerRef.getReference();
    }
}
