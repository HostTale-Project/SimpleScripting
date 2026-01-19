package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.SimpleScriptingPlugin;
import com.hosttale.simplescripting.ui.ScriptsListUI;
import com.hosttale.simplescripting.util.MessageHelper;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Command to open the Scripts Manager UI.
 * Allows players with permission to view and reload scripts.
 */
public class ScriptsCommand extends AbstractCommand {
    
    private static ScriptsCommand instance;
    
    public ScriptsCommand() {
        super("scripts", "Opens the Scripts Manager UI to view and reload scripts");
    }
    
    /**
     * Registers the /scripts command.
     */
    public static void register() {
        if (instance == null) {
            instance = new ScriptsCommand();
            SimpleScriptingPlugin.getInstance().getCommandRegistry().registerCommand(new ScriptsCommand());
            SimpleScriptingPlugin.getInstance().getLogger().atInfo()
                .log("Registered /scripts command");
        }
    }
    
    @Override
    public CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        Player player = (Player) ctx.sender();
        if (player == null) {
            ctx.sendMessage(MessageHelper.raw("§cThis command can only be used by players!"));
            return null;
        }
        
        // Check for operator permission
        if (!player.hasPermission("simplescripting.commands.scripts")) {
            ctx.sendMessage(MessageHelper.raw("§cYou don't have permission to use this command!"));
            return null;
        }
        
        // Open the Scripts List UI
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        player.getPageManager().openCustomPage(
            ctx.senderAsPlayerRef(),
            ((Player) ctx.sender()).getReference().getStore(),
            new ScriptsListUI(playerRef, CustomPageLifetime.CanDismiss)
        );
        return null;
    }
}
