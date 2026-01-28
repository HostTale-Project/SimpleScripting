package com.hosttale.simplescripting.mod.runtime.api.events;

import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hosttale.simplescripting.mod.runtime.api.events.GenericEvent;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayerHandle;

import java.util.List;

public final class EventPayloads {

    private EventPayloads() {
    }

    public static Object adapt(IBaseEvent<?> event) {
        if (event instanceof PlayerChatEvent chat) {
            return new PlayerChat(chat);
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
}
