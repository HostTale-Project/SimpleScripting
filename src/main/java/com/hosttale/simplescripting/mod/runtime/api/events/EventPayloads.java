package com.hosttale.simplescripting.mod.runtime.api.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hosttale.simplescripting.mod.runtime.api.events.GenericEvent;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayerHandle;

import java.util.List;

public final class EventPayloads {

    private EventPayloads() {
    }

    public static Object adapt(Object event) {
        if (event instanceof PlayerChatEvent chat) {
            return new PlayerChat(chat);
        }
        if (event instanceof PlayerConnectEvent connect) {
            return new PlayerRefPayload("playerConnect", connect.getPlayerRef());
        }
        if (event instanceof PlayerDisconnectEvent disconnect) {
            return new PlayerRefPayload("playerDisconnect", disconnect.getPlayerRef());
        }
        if (event instanceof PlayerReadyEvent ready) {
            var player = ready.getPlayer();
            var ref = player == null ? null : player.getPlayerRef();
            return new PlayerRefPayload("playerReady", ref);
        }
        return new GenericEvent(event);
    }

    public static final class PlayerChat {
        private final PlayerChatEvent delegate;

        public PlayerChat(PlayerChatEvent delegate) {
            this.delegate = delegate;
        }

        public String getType() {
            return "playerChat";
        }

        public PlayerHandle getSender() {
            return new PlayerHandle(delegate.getSender());
        }

        /**
         * Alias for getSender() to keep names predictable in JS.
         */
        public PlayerHandle getPlayer() {
            return new PlayerHandle(delegate.getSender());
        }

        /**
         * Alias for getSender() for backwards compatibility.
         */
        public PlayerHandle getPlayerRef() {
            return new PlayerHandle(delegate.getSender());
        }

        public List<PlayerHandle> getTargets() {
            return delegate.getTargets().stream()
                    .map(PlayerHandle::new)
                    .toList();
        }

        public String getMessage() {
            return delegate.getContent();
        }

        public void setMessage(String message) {
            delegate.setContent(message);
        }

        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        public void cancel() {
            delegate.setCancelled(true);
        }

    }

    public static final class PlayerRefPayload {
        private final String type;
        private final PlayerHandle player;

        public PlayerRefPayload(String type, com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
            this.type = type;
            this.player = new PlayerHandle(playerRef);
        }

        public String getType() {
            return type;
        }

        public PlayerHandle getPlayer() {
            return player;
        }

        public PlayerHandle getPlayerRef() {
            return player;
        }
    }
}
