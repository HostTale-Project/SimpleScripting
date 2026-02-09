package com.hosttale.simplescripting.mod.runtime.api.players;

import com.hosttale.simplescripting.mod.runtime.api.entities.LivingEntityHandle;
import com.hosttale.simplescripting.mod.runtime.api.inventory.ItemContainerHandle;
import com.hosttale.simplescripting.mod.runtime.api.ui.UiMessageRenderer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Locale;
import java.util.UUID;

/**
 * JS-friendly wrapper around PlayerRef to avoid exposing native classes.
 * 
 * <p>PlayerHandle extends LivingEntityHandle to provide inventory access and other
 * living entity functionality while adding player-specific features like messaging,
 * titles, and kick functionality.</p>
 */
public final class PlayerHandle extends LivingEntityHandle {

    private final PlayerRef playerRef;

    /**
     * Create a new PlayerHandle wrapping the given PlayerRef.
     * 
     * @param playerRef The player reference from the server
     * @throws IllegalArgumentException if playerRef is null or has no entity reference
     */
    public PlayerHandle(PlayerRef playerRef) {
        super(validatePlayerRef(playerRef));
        this.playerRef = playerRef;
    }
    
    private static Ref<EntityStore> validatePlayerRef(PlayerRef playerRef) {
        if (playerRef == null) {
            throw new IllegalArgumentException("PlayerRef cannot be null");
        }
        var entityRef = playerRef.getReference();
        if (entityRef == null) {
            throw new IllegalArgumentException("PlayerRef.getReference() returned null - player entity not yet created");
        }
        return entityRef;
    }

    /**
     * Get the player's username.
     * 
     * @return The player's username
     */
    public String getUsername() {
        return playerRef.getUsername();
    }

    /**
     * Get the player's unique ID as a string.
     * 
     * @return The player's UUID as a string, or empty string if null
     */
    public String getId() {
        UUID uuid = playerRef.getUuid();
        return uuid == null ? "" : uuid.toString();
    }

    /**
     * Get the player's language preference.
     * 
     * @return The player's language code
     */
    public String getLanguage() {
        return playerRef.getLanguage();
    }

    /**
     * Set the player's language preference.
     * 
     * @param language The language code to set (e.g., "en_us")
     */
    public void setLanguage(String language) {
        if (language != null) {
            playerRef.setLanguage(language.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Check if the player is currently online and their reference is valid.
     * This overrides the base isValid() method to use PlayerRef's validity check.
     * 
     * @return true if the player is online and their reference is valid
     */
    public boolean isOnline() {
        return playerRef.isValid();
    }

    /**
     * Get the player's inventory container (combined hotbar + storage).
     * 
     * <p>This overrides the base LivingEntityHandle.getInventory() to provide
     * direct access to the player's combined inventory container.</p>
     * 
     * @return The inventory container handle, or null if player is invalid
     */
    @Override
    public ItemContainerHandle getInventory() {
        if (!isOnline()) {
            return null;
        }

        try {
            // Get Player component from the entity holder
            Player player = playerRef.getComponent(Player.getComponentType());
            if (player == null) {
                return null;
            }

            Inventory inventory = player.getInventory();
            if (inventory == null) {
                return null;
            }

            // Return combined container (hotbar + storage) for convenient access
            Scriptable scope = Context.getCurrentContext().initStandardObjects();
            return new ItemContainerHandle(inventory.getCombinedHotbarFirst(), scope);
        } catch (Exception e) {
            // Log error and return null - don't expose internal errors to JavaScript
            return null;
        }
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

    /**
     * Get the underlying PlayerRef (for internal use).
     * 
     * @return The wrapped PlayerRef
     */
    public PlayerRef unwrap() {
        return playerRef;
    }
}
