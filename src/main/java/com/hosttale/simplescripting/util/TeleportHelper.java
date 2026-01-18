package com.hosttale.simplescripting.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for teleporting players.
 * Provides safe teleportation with async chunk loading.
 * 
 * NOTE: Most methods in this class need to be called from the World thread.
 * Use teleportFromAnyThread() for teleportation from scheduler callbacks.
 */
public class TeleportHelper {
    private final Logger logger;

    public TeleportHelper(Logger logger) {
        this.logger = logger;
    }

    /**
     * Teleports a player to the specified coordinates - THREAD SAFE.
     * Can be called from any thread (including scheduler callbacks).
     * @param player The player to teleport
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @param pitch Target pitch
     * @param yaw Target yaw
     * @param worldName Target world name (null for current world)
     */
    public void teleportFromAnyThread(@Nonnull PlayerRef player, double x, double y, double z,
                                       float pitch, float yaw, @Nullable String worldName) {
        // Determine target world
        World targetWorld = (worldName != null && !worldName.isEmpty())
            ? Universe.get().getWorld(worldName)
            : null;
        
        // If no specific world, try to get player's current world
        if (targetWorld == null) {
            targetWorld = getPlayerWorld(player);
        }
        
        // Last resort: use first available world
        if (targetWorld == null) {
            for (World w : Universe.get().getWorlds().values()) {
                targetWorld = w;
                break;
            }
        }
        
        if (targetWorld == null) {
            logger.warning("No world found for teleport");
            player.sendMessage(Message.raw("§cTeleport failed: no world found."));
            return;
        }
        
        final World finalTargetWorld = targetWorld;
        
        // Get the player's CURRENT world - this is where we need to execute store operations
        World playerCurrentWorld = getPlayerWorld(player);
        if (playerCurrentWorld == null) {
            playerCurrentWorld = finalTargetWorld; // Fallback
        }
        final World executeWorld = playerCurrentWorld;
        
        // Preload chunk first, then teleport on the PLAYER'S current world thread
        long chunkIndex = ChunkUtil.indexChunkFromBlock((int) x, (int) z);
        finalTargetWorld.getChunkAsync(chunkIndex)
            .thenAccept(chunk -> {
                // Execute teleport on the PLAYER'S CURRENT world thread (not target world)
                // This is required because the player's store belongs to their current world
                executeWorld.execute(() -> {
                    try {
                        Ref<EntityStore> ref = player.getReference();
                        if (ref == null || !ref.isValid()) {
                            player.sendMessage(Message.raw("§cTeleport failed: player not found."));
                            return;
                        }
                        Store<EntityStore> store = ref.getStore();
                        if (store == null) {
                            player.sendMessage(Message.raw("§cTeleport failed: store not found."));
                            return;
                        }

                        Vector3d position = new Vector3d(x, y, z);
                        Vector3f rotation = new Vector3f(pitch, yaw, 0);

                        Teleport teleport = new Teleport(finalTargetWorld, position, rotation);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                    } catch (Exception e) {
                        logger.severe("Error during teleport execution: " + e.getMessage());
                        player.sendMessage(Message.raw("§cTeleport failed."));
                    }
                });
            })
            .exceptionally(ex -> {
                player.sendMessage(Message.raw("§cFailed to load destination chunk."));
                return null;
            });
    }

    /**
     * Teleports a player to the specified coordinates in the same world.
     * NOTE: Must be called from the World thread.
     * @param player The player to teleport
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @return true if teleport initiated, false on error
     */
    public boolean teleport(@Nonnull PlayerRef player, double x, double y, double z) {
        return teleportWithRotation(player, x, y, z, 0, 0, null);
    }

    /**
     * Teleports a player to the specified coordinates in a specific world.
     * @param player The player to teleport
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @param worldName Target world name (null for current world)
     * @return true if teleport initiated, false on error
     */
    public boolean teleport(@Nonnull PlayerRef player, double x, double y, double z, @Nullable String worldName) {
        return teleportWithRotation(player, x, y, z, 0, 0, worldName);
    }

    /**
     * Teleports a player to the specified coordinates with rotation.
     * @param player The player to teleport
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @param pitch Target pitch
     * @param yaw Target yaw
     * @param worldName Target world name (null for current world)
     * @return true if teleport initiated, false on error
     */
    public boolean teleportWithRotation(@Nonnull PlayerRef player, double x, double y, double z, 
                                         float pitch, float yaw, @Nullable String worldName) {
        try {
            // Get current world
            World currentWorld = getPlayerWorld(player);
            if (currentWorld == null) {
                logger.warning("Could not get player's current world");
                return false;
            }

            // Determine target world
            World targetWorld = (worldName != null && !worldName.isEmpty()) 
                ? Universe.get().getWorld(worldName) 
                : currentWorld;
            
            if (targetWorld == null) {
                logger.warning("Target world not found: " + worldName);
                return false;
            }

            // Preload chunk and teleport
            preloadChunkAndTeleport(player, currentWorld, targetWorld, x, y, z, pitch, yaw);
            return true;
        } catch (Exception e) {
            logger.severe("Error teleporting player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Teleports a player to another player's location.
     * @param player The player to teleport
     * @param target The target player
     * @return true if teleport initiated, false on error
     */
    public boolean teleportToPlayer(@Nonnull PlayerRef player, @Nonnull PlayerRef target) {
        try {
            LocationInfo targetLocation = getPlayerLocation(target);
            if (targetLocation == null) {
                return false;
            }
            return teleportWithRotation(player, 
                targetLocation.x, targetLocation.y, targetLocation.z,
                targetLocation.pitch, targetLocation.yaw,
                targetLocation.worldName);
        } catch (Exception e) {
            logger.severe("Error teleporting to player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets a player's current location.
     * @param player The player
     * @return LocationInfo with coordinates and rotation, or null on error
     */
    @Nullable
    public LocationInfo getPlayerLocation(@Nonnull PlayerRef player) {
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

            return new LocationInfo(
                pos.getX(), pos.getY(), pos.getZ(),
                rot.getX(), rot.getY(),
                world.getName()
            );
        } catch (Exception e) {
            logger.severe("Error getting player location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the world a player is currently in.
     * @param player The player
     * @return The player's current world, or null on error
     */
    @Nullable
    public World getPlayerWorld(@Nonnull PlayerRef player) {
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
     * Preloads a chunk asynchronously.
     * @param worldName The world name
     * @param blockX Block X coordinate
     * @param blockZ Block Z coordinate
     * @return CompletableFuture that completes when chunk is loaded
     */
    public CompletableFuture<Boolean> preloadChunk(@Nonnull String worldName, int blockX, int blockZ) {
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }
        return preloadChunk(world, blockX, blockZ);
    }

    /**
     * Preloads a chunk asynchronously.
     * @param world The world
     * @param blockX Block X coordinate
     * @param blockZ Block Z coordinate
     * @return CompletableFuture that completes when chunk is loaded
     */
    public CompletableFuture<Boolean> preloadChunk(@Nonnull World world, int blockX, int blockZ) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        return world.getChunkStore().getChunkReferenceAsync(chunkIndex)
            .thenApply(chunkRef -> chunkRef != null)
            .exceptionally(ex -> {
                logger.warning("Failed to preload chunk: " + ex.getMessage());
                return false;
            });
    }

    /**
     * Preloads chunk and performs teleportation.
     */
    private void preloadChunkAndTeleport(@Nonnull PlayerRef player, @Nonnull World currentWorld, 
                                         @Nonnull World targetWorld, double x, double y, double z,
                                         float pitch, float yaw) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock((int) x, (int) z);

        targetWorld.getChunkStore().getChunkReferenceAsync(chunkIndex)
            .thenAccept(chunkRef -> {
                // Execute teleport on the world thread
                currentWorld.execute(() -> {
                    try {
                        Ref<EntityStore> ref = player.getReference();
                        Store<EntityStore> store = ref.getStore();
                        if (store == null) return;

                        Vector3d position = new Vector3d(x, y, z);
                        Vector3f rotation = new Vector3f(pitch, yaw, 0);

                        Teleport teleport = new Teleport(targetWorld, position, rotation);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                    } catch (Exception e) {
                        logger.severe("Error during teleport execution: " + e.getMessage());
                        player.sendMessage(Message.raw("§cTeleport failed."));
                    }
                });
            })
            .exceptionally(ex -> {
                player.sendMessage(Message.raw("§cFailed to load destination chunk."));
                return null;
            });
    }

    /**
     * Data class containing location information.
     */
    public static class LocationInfo {
        public final double x;
        public final double y;
        public final double z;
        public final float pitch;
        public final float yaw;
        public final String worldName;

        public LocationInfo(double x, double y, double z, float pitch, float yaw, String worldName) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.yaw = yaw;
            this.worldName = worldName;
        }

        /**
         * Converts to a JSON-compatible object for JavaScript.
         */
        @Override
        public String toString() {
            return String.format("{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"pitch\":%.2f,\"yaw\":%.2f,\"worldName\":\"%s\"}",
                x, y, z, pitch, yaw, worldName);
        }
    }
}
