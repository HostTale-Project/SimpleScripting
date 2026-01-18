package com.hosttale.simplescripting.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for player-related operations.
 * Provides player lookup, position access, and iteration.
 */
public class PlayerHelper {
    private final Logger logger;
    private Scriptable scope;

    public PlayerHelper(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Sets the JavaScript scope for callback execution.
     * @param scope The Rhino scope
     */
    public void setScope(Scriptable scope) {
        this.scope = scope;
    }

    // ========================================================================
    // WORLD THREAD EXECUTION
    // ========================================================================
    
    /**
     * Executes a JavaScript callback on the World thread for a specific player.
     * This is necessary for operations that access player store/components.
     * Can be called from any thread (scheduler, command handlers, etc.).
     * 
     * @param player The player whose world thread to use
     * @param callback The JavaScript function to execute
     */
    public void runOnWorldThread(@Nonnull PlayerRef player, @Nonnull Function callback) {
        World world = getWorldSafe(player);
        if (world == null) {
            logger.warning("Could not get world for player - callback not executed");
            return;
        }
        
        world.execute(() -> {
            try {
                Context cx = Context.enter();
                try {
                    callback.call(cx, scope, scope, new Object[]{player});
                } finally {
                    Context.exit();
                }
            } catch (Exception e) {
                logger.severe("Error executing callback on world thread: " + e.getMessage());
            }
        });
    }
    
    /**
     * Executes a JavaScript callback on a specific world's thread.
     * Can be called from any thread.
     * 
     * @param worldName The world name
     * @param callback The JavaScript function to execute
     */
    public void runOnWorldThread(@Nonnull String worldName, @Nonnull Function callback) {
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            // Try to get any world as fallback
            for (World w : Universe.get().getWorlds().values()) {
                world = w;
                break;
            }
        }
        
        if (world == null) {
            logger.warning("No world found - callback not executed");
            return;
        }
        
        final World finalWorld = world;
        finalWorld.execute(() -> {
            try {
                Context cx = Context.enter();
                try {
                    callback.call(cx, scope, scope, new Object[]{});
                } finally {
                    Context.exit();
                }
            } catch (Exception e) {
                logger.severe("Error executing callback on world thread: " + e.getMessage());
            }
        });
    }
    
    /**
     * Executes a JavaScript callback on the default world's thread.
     * Can be called from any thread.
     * 
     * @param callback The JavaScript function to execute
     */
    public void runOnWorldThread(@Nonnull Function callback) {
        runOnWorldThread("default", callback);
    }
    
    /**
     * Gets the World object for a player safely.
     * Uses the player's EntityStore to get their current world.
     * This is safe to call from any thread as getExternalData() doesn't require thread context.
     */
    @Nullable
    private World getWorldSafe(@Nonnull PlayerRef player) {
        try {
            // Get world from player's stored reference
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                if (store != null) {
                    // getExternalData() is safe to call from any thread
                    EntityStore entityStore = store.getExternalData();
                    if (entityStore != null) {
                        World world = entityStore.getWorld();
                        if (world != null) {
                            return world;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Could not get player's world, falling back to default: " + e.getMessage());
        }
        
        // Fall back to getting first available world
        for (World world : Universe.get().getWorlds().values()) {
            return world;
        }
        return null;
    }

    // ========================================================================
    // PLAYER LOOKUP
    // ========================================================================

    /**
     * Gets a player by their username (exact match).
     * @param username The player's username
     * @return The PlayerRef, or null if not found
     */
    @Nullable
    public PlayerRef getByName(@Nonnull String username) {
        return Universe.get().getPlayerByUsername(username, NameMatching.EXACT);
    }

    /**
     * Gets a player by username with flexible matching (starts with, ignore case).
     * @param username The partial or full username
     * @return The PlayerRef, or null if not found
     */
    @Nullable
    public PlayerRef getByNameFuzzy(@Nonnull String username) {
        return Universe.get().getPlayerByUsername(username, NameMatching.STARTS_WITH_IGNORE_CASE);
    }

    /**
     * Gets a player by their UUID.
     * @param uuid The player's UUID as string
     * @return The PlayerRef, or null if not found
     */
    @Nullable
    public PlayerRef getByUuid(@Nonnull String uuid) {
        try {
            UUID playerUuid = UUID.fromString(uuid);
            return getByUuid(playerUuid);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid UUID format: " + uuid);
            return null;
        }
    }

    /**
     * Gets a player by their UUID.
     * @param uuid The player's UUID
     * @return The PlayerRef, or null if not found
     */
    @Nullable
    public PlayerRef getByUuid(@Nonnull UUID uuid) {
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Gets all online players.
     * @return List of all online PlayerRefs
     */
    public List<PlayerRef> getOnlinePlayers() {
        List<PlayerRef> players = new ArrayList<>();
        for (PlayerRef player : Universe.get().getPlayers()) {
            players.add(player);
        }
        return players;
    }

    /**
     * Gets the number of online players.
     * @return The count of online players
     */
    public int getOnlineCount() {
        int count = 0;
        for (PlayerRef ignored : Universe.get().getPlayers()) {
            count++;
        }
        return count;
    }

    /**
     * Gets a player's current position.
     * @param player The player
     * @return Position object with x, y, z coordinates, or null on error
     */
    @Nullable
    public Position getPosition(@Nonnull PlayerRef player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;

            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return null;

            Vector3d pos = transform.getPosition();
            return new Position(pos.getX(), pos.getY(), pos.getZ());
        } catch (Exception e) {
            logger.severe("Error getting player position: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a player's current rotation (pitch and yaw).
     * @param player The player
     * @return Rotation object with pitch and yaw, or null on error
     */
    @Nullable
    public Rotation getRotation(@Nonnull PlayerRef player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;

            HeadRotation headRot = store.getComponent(ref, HeadRotation.getComponentType());
            if (headRot == null) return null;

            Vector3f rot = headRot.getRotation();
            return new Rotation(rot.getX(), rot.getY());
        } catch (Exception e) {
            logger.severe("Error getting player rotation: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a player's full location (position + rotation + world).
     * @param player The player
     * @return FullLocation object, or null on error
     */
    @Nullable
    public FullLocation getFullLocation(@Nonnull PlayerRef player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;
            
            EntityStore entityStore = store.getExternalData();
            World world = entityStore.getWorld();

            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return null;

            Vector3d pos = transform.getPosition();
            HeadRotation headRot = store.getComponent(ref, HeadRotation.getComponentType());
            Vector3f rot = (headRot != null) ? headRot.getRotation() : new Vector3f(0, 0, 0);

            return new FullLocation(
                pos.getX(), pos.getY(), pos.getZ(),
                rot.getX(), rot.getY(),
                world.getName()
            );
        } catch (Exception e) {
            logger.severe("Error getting player full location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the world a player is currently in.
     * @param player The player
     * @return The player's current world, or null if not found
     */
    @Nullable
    public World getWorld(@Nonnull PlayerRef player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;
            
            EntityStore entityStore = store.getExternalData();
            return entityStore.getWorld();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the world name for a player.
     * @param player The player
     * @return The world name, or null if not found
     */
    @Nullable
    public String getWorldName(@Nonnull PlayerRef player) {
        World world = getWorld(player);
        return (world != null) ? world.getName() : null;
    }

    /**
     * Gets the player's UUID as a string.
     * @param player The player
     * @return UUID string
     */
    public String getUuidString(@Nonnull PlayerRef player) {
        return player.getUuid().toString();
    }

    /**
     * Gets the player's username.
     * @param player The player
     * @return Username
     */
    public String getUsername(@Nonnull PlayerRef player) {
        return player.getUsername();
    }

    /**
     * Checks if a player is online.
     * @param username The player's username
     * @return true if online, false otherwise
     */
    public boolean isOnline(@Nonnull String username) {
        return getByName(username) != null;
    }

    /**
     * Checks if a player with the given UUID is online.
     * @param uuid The player's UUID as string
     * @return true if online, false otherwise
     */
    public boolean isOnlineByUuid(@Nonnull String uuid) {
        return getByUuid(uuid) != null;
    }

    /**
     * Simple position data class.
     */
    public static class Position {
        public final double x;
        public final double y;
        public final double z;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format("{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f}", x, y, z);
        }
    }

    /**
     * Simple rotation data class.
     */
    public static class Rotation {
        public final float pitch;
        public final float yaw;

        public Rotation(float pitch, float yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
        }

        @Override
        public String toString() {
            return String.format("{\"pitch\":%.2f,\"yaw\":%.2f}", pitch, yaw);
        }
    }

    /**
     * Full location with position, rotation, and world.
     */
    public static class FullLocation {
        public final double x;
        public final double y;
        public final double z;
        public final float pitch;
        public final float yaw;
        public final String worldName;

        public FullLocation(double x, double y, double z, float pitch, float yaw, String worldName) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.yaw = yaw;
            this.worldName = worldName;
        }

        @Override
        public String toString() {
            return String.format("{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"pitch\":%.2f,\"yaw\":%.2f,\"worldName\":\"%s\"}",
                x, y, z, pitch, yaw, worldName);
        }
    }
}
