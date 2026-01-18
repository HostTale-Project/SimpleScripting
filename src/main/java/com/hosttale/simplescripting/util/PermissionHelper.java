package com.hosttale.simplescripting.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Helper class for permission checking.
 * Wraps Hytale's native PermissionsModule for JavaScript access.
 */
public class PermissionHelper {
    private final Logger logger;

    public PermissionHelper(Logger logger) {
        this.logger = logger;
    }

    /**
     * Checks if a player has a specific permission.
     * @param player The player to check
     * @param permission The permission node (e.g., "simplescripting.admin")
     * @return true if the player has the permission, false otherwise
     */
    public boolean has(@Nonnull PlayerRef player, @Nonnull String permission) {
        try {
            return PermissionsModule.get().hasPermission(player.getUuid(), permission);
        } catch (Exception e) {
            logger.warning("Error checking permission '" + permission + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player has a specific permission by UUID.
     * @param uuid The player's UUID as string
     * @param permission The permission node
     * @return true if the player has the permission, false otherwise
     */
    public boolean has(@Nonnull String uuid, @Nonnull String permission) {
        try {
            UUID playerUuid = UUID.fromString(uuid);
            return PermissionsModule.get().hasPermission(playerUuid, permission);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid UUID format: " + uuid);
            return false;
        } catch (Exception e) {
            logger.warning("Error checking permission '" + permission + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a command sender has a specific permission.
     * Works with both Player and console senders.
     * @param sender The command sender
     * @param permission The permission node
     * @return true if the sender has the permission (console always returns true)
     */
    public boolean hasSender(@Nonnull CommandSender sender, @Nonnull String permission) {
        PlayerRef playerRef = asPlayer(sender);
        if (playerRef != null) {
            return has(playerRef, permission);
        }
        // Console/non-player senders have all permissions
        return true;
    }

    /**
     * Requires a player to have a specific permission.
     * Sends an error message and returns false if they don't have it.
     * @param player The player to check
     * @param permission The permission node
     * @return true if the player has the permission, false otherwise
     */
    public boolean require(@Nonnull PlayerRef player, @Nonnull String permission) {
        if (has(player, permission)) {
            return true;
        }
        player.sendMessage(Message.raw("§cYou don't have permission to do this."));
        return false;
    }

    /**
     * Requires a command sender to have a specific permission.
     * Sends an error message and returns false if they don't have it.
     * @param sender The command sender
     * @param permission The permission node
     * @return true if the sender has the permission, false otherwise
     */
    public boolean requireSender(@Nonnull CommandSender sender, @Nonnull String permission) {
        if (hasSender(sender, permission)) {
            return true;
        }
        sender.sendMessage(Message.raw("§cYou don't have permission to do this."));
        return false;
    }

    /**
     * Requires a player to have a specific permission with a custom error message.
     * @param player The player to check
     * @param permission The permission node
     * @param errorMessage Custom error message to send
     * @return true if the player has the permission, false otherwise
     */
    public boolean require(@Nonnull PlayerRef player, @Nonnull String permission, @Nonnull String errorMessage) {
        if (has(player, permission)) {
            return true;
        }
        player.sendMessage(Message.raw(errorMessage));
        return false;
    }

    /**
     * Checks if a player is an operator/admin.
     * Uses a common admin permission node.
     * @param player The player to check
     * @return true if the player is an admin
     */
    public boolean isAdmin(@Nonnull PlayerRef player) {
        return has(player, "simplescripting.admin") || 
               has(player, "operator") ||
               has(player, "*");
    }

    /**
     * Checks if a command sender is an operator/admin.
     * @param sender The command sender
     * @return true if the sender is an admin (console always returns true)
     */
    public boolean isAdminSender(@Nonnull CommandSender sender) {
        PlayerRef playerRef = asPlayer(sender);
        if (playerRef != null) {
            return isAdmin(playerRef);
        }
        return true; // Console is always admin
    }

    /**
     * Checks if the sender is a player (not console).
     * @param sender The command sender
     * @return true if the sender is a player
     */
    public boolean isPlayer(@Nonnull CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Gets the PlayerRef from a CommandSender if it's a player.
     * Uses the component system to extract PlayerRef from the Player entity.
     * @param sender The command sender
     * @return The PlayerRef, or null if not a player
     */
    public PlayerRef asPlayer(@Nonnull CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                return (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
            }
        }
        return null;
    }

    /**
     * Gets the Player entity from a CommandSender if it's a player.
     * @param sender The command sender
     * @return The Player entity, or null if not a player
     */
    public Player asPlayerEntity(@Nonnull CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    /**
     * Requires the sender to be a player.
     * Sends an error message if they're not.
     * @param sender The command sender
     * @return The PlayerRef, or null if not a player
     */
    public PlayerRef requirePlayer(@Nonnull CommandSender sender) {
        PlayerRef playerRef = asPlayer(sender);
        if (playerRef == null) {
            sender.sendMessage(Message.raw("§cThis command can only be used by players."));
            return null;
        }
        return playerRef;
    }
}
