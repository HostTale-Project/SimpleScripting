package com.hosttale.simplescripting.mod.runtime.api;

import com.hosttale.simplescripting.mod.runtime.api.commands.JsCommandContext;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsCommandContextTest {

    private final PlayersApi playersApi = new PlayersApi(HytaleLogger.get("test"));

    @Test
    void senderNamePrefersPlayerUsername() {
        PlayerRef ref = mock(PlayerRef.class);
        when(ref.getUsername()).thenReturn("invboy");

        Player player = mock(Player.class);
        when(player.getPlayerRef()).thenReturn(ref);

        CommandContext delegate = mock(CommandContext.class);
        when(delegate.isPlayer()).thenReturn(true);
        when(delegate.senderAs(Player.class)).thenReturn(player);

        JsCommandContext ctx = new JsCommandContext(delegate, playersApi, new String[0], "");

        assertEquals("invboy", ctx.senderName());
    }

    @Test
    void senderNameFallsBackToDisplayNameForNonPlayer() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.getDisplayName()).thenReturn("Console");

        CommandContext delegate = mock(CommandContext.class);
        when(delegate.isPlayer()).thenReturn(false);
        when(delegate.sender()).thenReturn(sender);

        JsCommandContext ctx = new JsCommandContext(delegate, playersApi, new String[0], "");

        assertEquals("Console", ctx.senderName());
    }

    @Test
    void senderNameFallsBackToDisplayNameWhenUsernameMissing() {
        PlayerRef ref = mock(PlayerRef.class);
        when(ref.getUsername()).thenReturn(null);

        Player player = mock(Player.class);
        when(player.getPlayerRef()).thenReturn(ref);

        CommandSender sender = mock(CommandSender.class);
        when(sender.getDisplayName()).thenReturn("Mystery");

        CommandContext delegate = mock(CommandContext.class);
        when(delegate.isPlayer()).thenReturn(true);
        when(delegate.senderAs(Player.class)).thenReturn(player);
        when(delegate.sender()).thenReturn(sender);

        JsCommandContext ctx = new JsCommandContext(delegate, playersApi, new String[0], "");

        assertEquals("Mystery", ctx.senderName());
    }
}
