package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.scripts.ScriptBrowser;
import com.hosttale.simplescripting.ui.ScriptsListPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ScriptsCommandTest {

    private ScriptBrowser browser;
    private ScriptsCommand command;

    @BeforeEach
    void setUp() {
        browser = mock(ScriptBrowser.class);
        command = new ScriptsCommand(browser);
    }

    @Test
    void permissionIsSet() {
        assertEquals("simplescripting.commands.scripts", command.getPermission());
    }

    @Test
    void nonPlayerSenderGetsMessage() {
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.isPlayer()).thenReturn(false);

        command.executeSync(ctx);

        verify(ctx).sendMessage(any());
        verifyNoInteractions(browser);
    }

    @SuppressWarnings("unchecked")
    @Test
    void playerOpensScriptsPage() {
        CommandContext ctx = mock(CommandContext.class);
        Player player = mock(Player.class);
        Ref<EntityStore> ref = mock(Ref.class);
        Store<EntityStore> store = mock(Store.class);
        PlayerRef playerRef = mock(PlayerRef.class);
        PageManager pageManager = mock(PageManager.class);

        when(ctx.isPlayer()).thenReturn(true);
        when(ctx.senderAs(Player.class)).thenReturn(player);
        when(ctx.senderAsPlayerRef()).thenReturn(ref);
        when(player.getReference()).thenReturn(ref);
        when(ref.getStore()).thenReturn(store);
        when(player.getPlayerRef()).thenReturn(playerRef);
        when(playerRef.isValid()).thenReturn(true);
        when(player.getPageManager()).thenReturn(pageManager);

        command.executeSync(ctx);

        ArgumentCaptor<ScriptsListPage> pageCaptor = ArgumentCaptor.forClass(ScriptsListPage.class);
        verify(pageManager).openCustomPage(eq(ref), eq(store), pageCaptor.capture());
        // Basic sanity: page was constructed with the expected player ref
        assertEquals(playerRef, pageCaptor.getValue().getOwner());
    }
}
