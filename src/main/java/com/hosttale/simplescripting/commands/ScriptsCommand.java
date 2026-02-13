package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.scripts.ScriptBrowser;
import com.hosttale.simplescripting.ui.ScriptsListPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ScriptsCommand extends CommandBase {

    private final ScriptBrowser browser;

    public ScriptsCommand(ScriptBrowser browser) {
        super("scripts", "Open the Scripts Manager UI.");
        this.browser = browser;
        requirePermission("simplescripting.commands.scripts");
    }

    @Override
    @SuppressWarnings("removal") // TODO: Replace getPlayerRef() when Hytale provides alternative API
    protected void executeSync(CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.raw("This command can only be used by players."));
            return;
        }

        Player player = commandContext.senderAs(Player.class);
        if (player == null) {
            commandContext.sendMessage(Message.raw("Unable to resolve player for this command."));
            return;
        }

        com.hypixel.hytale.component.Ref<EntityStore> ref = commandContext.senderAsPlayerRef();
        if (ref == null) {
            commandContext.sendMessage(Message.raw("Unable to open scripts UI right now."));
            return;
        }

        com.hypixel.hytale.component.Store<EntityStore> store = ref.getStore();
        if (store == null) {
            commandContext.sendMessage(Message.raw("Unable to open scripts UI right now."));
            return;
        }

        @SuppressWarnings("removal") // getUuid()/getPlayerRef() deprecated, but needed for Player -> PlayerRef lookup
        PlayerRef playerRef = null;
        
        // Try Universe lookup first (proper non-deprecated approach)
        com.hypixel.hytale.server.core.universe.Universe universe = com.hypixel.hytale.server.core.universe.Universe.get();
        if (universe != null) {
            playerRef = universe.getPlayer(player.getUuid());
        }
        
        // Fallback to deprecated method for test environments
        if (playerRef == null) {
            playerRef = player.getPlayerRef();
        }
        
        if (playerRef == null || !playerRef.isValid()) {
            commandContext.sendMessage(Message.raw("Unable to open scripts UI right now."));
            return;
        }

        player.getPageManager().openCustomPage(ref, store,
                new ScriptsListPage(playerRef, browser, CustomPageLifetime.CanDismiss));
    }
}
