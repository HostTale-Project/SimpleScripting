package com.hosttale.simplescripting.mod.runtime.api.commands;

import com.hosttale.simplescripting.mod.runtime.api.players.PlayerHandle;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hosttale.simplescripting.mod.runtime.api.ui.UiMessageRenderer;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.Arrays;

/**
 * JS-friendly wrapper around CommandContext to avoid exposing native types directly.
 */
public final class JsCommandContext {

    private final CommandContext delegate;
    private final PlayersApi playersApi;
    private final String[] args;
    private final String rawInput;

    public JsCommandContext(CommandContext delegate, PlayersApi playersApi, String[] args, String rawInput) {
        this.delegate = delegate;
        this.playersApi = playersApi;
        this.args = args;
        this.rawInput = rawInput;
    }

    public boolean isPlayer() {
        return delegate.isPlayer();
    }

    public PlayerHandle sender() {
        if (!delegate.isPlayer()) {
            return null;
        }
        Player player = delegate.senderAs(Player.class);
        if (player == null || player.getPlayerRef() == null) {
            return null;
        }
        return playersApi.wrap(player.getPlayerRef());
    }

    public String[] args() {
        return args;
    }

    public String rawInput() {
        return rawInput;
    }

    public void reply(Object text) {
        delegate.sendMessage(UiMessageRenderer.toMessage(text));
    }

    public String senderName() {
        PlayerHandle player = sender();
        if (player != null && player.getUsername() != null) {
            return player.getUsername();
        }
        CommandSender sender = delegate.sender();
        if (sender == null) {
            return "";
        }
        String displayName = sender.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return sender.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return "CommandContext{sender=%s,args=%s}".formatted(senderName(), Arrays.toString(args()));
    }
}
