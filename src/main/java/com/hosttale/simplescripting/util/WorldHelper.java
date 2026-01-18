package com.hosttale.simplescripting.util;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for world and block operations.
 * Provides world access, chunk loading, and block information.
 */
public class WorldHelper {
    private final Logger logger;
    private Scriptable scope;

    public WorldHelper(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Sets the JavaScript scope for callback execution.
     * @param scope The Rhino scope
     */
    public void setScope(Scriptable scope) {
        this.scope = scope;
    }

    /**
     * Gets a world by name.
     * @param name The world name
     * @return The World, or null if not found
     */
    @Nullable
    public World getWorld(@Nonnull String name) {
        return Universe.get().getWorld(name);
    }

    /**
     * Gets all loaded worlds.
     * @return List of world names
     */
    public List<String> getWorldNames() {
        List<String> names = new ArrayList<>();
        for (World world : Universe.get().getWorlds().values()) {
            names.add(world.getName());
        }
        return names;
    }

    /**
     * Gets the default world.
     * @return The default world, or null if none
     */
    @Nullable
    public World getDefaultWorld() {
        for (World world : Universe.get().getWorlds().values()) {
            return world; // Return first world as default
        }
        return null;
    }

    /**
     * Gets the name of a world.
     * @param world The world object
     * @return The world name
     */
    public String getWorldName(@Nonnull World world) {
        return world.getName();
    }

    /**
     * Gets block information at a specific location asynchronously.
     * @param worldName The world name
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return CompletableFuture with BlockInfo, or null values on error
     */
    public CompletableFuture<BlockInfo> getBlock(@Nonnull String worldName, int x, int y, int z) {
        World world = getWorld(worldName);
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        return getBlock(world, x, y, z);
    }

    /**
     * Gets block information at a specific location asynchronously.
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return CompletableFuture with BlockInfo
     */
    public CompletableFuture<BlockInfo> getBlock(@Nonnull World world, int x, int y, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        
        return world.getChunkAsync(chunkIndex)
            .thenApply(chunk -> {
                if (chunk == null) {
                    return null;
                }
                
                try {
                    // Calculate local coordinates within chunk (32x32 chunks in Hytale)
                    int localX = x & 0x1F;
                    int localZ = z & 0x1F;
                    
                    // Get block at position (returns block ID as int)
                    int blockId = chunk.getBlock(localX, y, localZ);
                    
                    // Block ID 0 is air
                    if (blockId == 0) {
                        return new BlockInfo("air", true, false, false, x, y, z);
                    }
                    
                    // Extract block information based on ID
                    String blockType = getBlockTypeName(blockId);
                    boolean isAir = blockId == 0;
                    boolean isSolid = isSolidBlockId(blockId);
                    boolean isLiquid = isLiquidBlockId(blockId);
                    
                    return new BlockInfo(blockType, isAir, isSolid, isLiquid, x, y, z);
                } catch (Exception e) {
                    logger.warning("Error getting block at " + x + "," + y + "," + z + ": " + e.getMessage());
                    return null;
                }
            })
            .exceptionally(ex -> {
                logger.warning("Error loading chunk for block access: " + ex.getMessage());
                return null;
            });
    }

    /**
     * Gets block type name from block ID.
     * TODO: Implement proper block registry lookup when available.
     */
    private String getBlockTypeName(int blockId) {
        if (blockId == 0) return "air";
        // Without access to block registry, return generic name
        return "block_" + blockId;
    }

    /**
     * Checks if a block ID represents a solid block.
     * Block ID 0 is air (not solid).
     */
    private boolean isSolidBlockId(int blockId) {
        // For now, assume non-zero, non-liquid blocks are solid
        return blockId != 0 && !isLiquidBlockId(blockId);
    }

    /**
     * Checks if a block ID represents a liquid block.
     * TODO: Implement proper liquid detection when block registry available.
     */
    private boolean isLiquidBlockId(int blockId) {
        // Without block registry, we can't reliably detect liquids
        // This would need to be updated with actual liquid block IDs
        return false;
    }

    /**
     * Checks if a block at the given location is air (async).
     * @param worldName The world name
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return CompletableFuture with true if air, false otherwise
     */
    public CompletableFuture<Boolean> isAir(@Nonnull String worldName, int x, int y, int z) {
        return getBlock(worldName, x, y, z)
            .thenApply(block -> block != null && block.isAir);
    }

    /**
     * Checks if a block at the given location is solid (async).
     * @param worldName The world name
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return CompletableFuture with true if solid, false otherwise
     */
    public CompletableFuture<Boolean> isSolid(@Nonnull String worldName, int x, int y, int z) {
        return getBlock(worldName, x, y, z)
            .thenApply(block -> block != null && block.isSolid);
    }

    /**
     * Checks if a block at the given location is liquid (async).
     * @param worldName The world name
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return CompletableFuture with true if liquid, false otherwise
     */
    public CompletableFuture<Boolean> isLiquid(@Nonnull String worldName, int x, int y, int z) {
        return getBlock(worldName, x, y, z)
            .thenApply(block -> block != null && block.isLiquid);
    }

    /**
     * Preloads a chunk for faster access.
     * @param worldName The world name
     * @param blockX Block X coordinate (any block in the chunk)
     * @param blockZ Block Z coordinate (any block in the chunk)
     * @return CompletableFuture that completes when chunk is loaded
     */
    public CompletableFuture<Boolean> preloadChunk(@Nonnull String worldName, int blockX, int blockZ) {
        World world = getWorld(worldName);
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        return world.getChunkAsync(chunkIndex)
            .thenApply(chunk -> chunk != null)
            .exceptionally(ex -> false);
    }

    /**
     * Gets the highest solid block Y coordinate at the given X,Z position.
     * Useful for finding safe teleport locations.
     * @param worldName The world name
     * @param x Block X coordinate
     * @param z Block Z coordinate
     * @param maxY Maximum Y to search from
     * @return CompletableFuture with the highest solid Y, or -1 if none found
     */
    public CompletableFuture<Integer> getHighestSolidY(@Nonnull String worldName, int x, int z, int maxY) {
        World world = getWorld(worldName);
        if (world == null) {
            return CompletableFuture.completedFuture(-1);
        }

        return preloadChunk(worldName, x, z).thenCompose(loaded -> {
            if (!loaded) {
                return CompletableFuture.completedFuture(-1);
            }
            
            // Search from top to bottom for first solid block
            return searchForSolidBlock(world, x, z, maxY, 0);
        });
    }

    /**
     * Recursively searches for the highest solid block.
     */
    private CompletableFuture<Integer> searchForSolidBlock(World world, int x, int z, int currentY, int minY) {
        if (currentY < minY) {
            return CompletableFuture.completedFuture(-1);
        }
        
        return getBlock(world, x, currentY, z).thenCompose(block -> {
            if (block != null && block.isSolid) {
                return CompletableFuture.completedFuture(currentY);
            }
            return searchForSolidBlock(world, x, z, currentY - 1, minY);
        });
    }

    /**
     * Finds a safe teleport location (solid block below, 2 air blocks above).
     * @param worldName The world name
     * @param x Block X coordinate
     * @param z Block Z coordinate
     * @param maxY Maximum Y to search from
     * @return CompletableFuture with safe Y coordinate, or -1 if none found
     */
    public CompletableFuture<Integer> findSafeTeleportY(@Nonnull String worldName, int x, int z, int maxY) {
        World world = getWorld(worldName);
        if (world == null) {
            return CompletableFuture.completedFuture(-1);
        }

        return preloadChunk(worldName, x, z).thenCompose(loaded -> {
            if (!loaded) {
                return CompletableFuture.completedFuture(-1);
            }
            return searchForSafeLocation(world, x, z, maxY, 1);
        });
    }
    
    /**
     * JavaScript-friendly version: Finds a safe teleport location with callback.
     * @param world The World object
     * @param x Block X coordinate
     * @param z Block Z coordinate
     * @param callback JavaScript function to call with the safe Y (or null if not found)
     */
    public void findSafeTeleportY(@Nonnull World world, int x, int z, @Nonnull Function callback) {
        findSafeTeleportY(world, x, z, 256, callback);
    }
    
    /**
     * JavaScript-friendly version: Finds a safe teleport location with callback.
     * @param world The World object
     * @param x Block X coordinate
     * @param z Block Z coordinate
     * @param maxY Maximum Y to search from
     * @param callback JavaScript function to call with the safe Y (or null if not found)
     */
    public void findSafeTeleportY(@Nonnull World world, int x, int z, int maxY, @Nonnull Function callback) {
        String worldName = world.getName();
        
        preloadChunk(worldName, x, z).thenCompose(loaded -> {
            if (!loaded) {
                return CompletableFuture.completedFuture(-1);
            }
            return searchForSafeLocation(world, x, z, maxY, 1);
        }).thenAccept(safeY -> {
            // Call JavaScript callback with result
            if (scope != null) {
                try {
                    Context cx = Context.enter();
                    try {
                        Object result = (safeY != null && safeY > 0) ? safeY : null;
                        callback.call(cx, scope, scope, new Object[]{result});
                    } finally {
                        Context.exit();
                    }
                } catch (Exception e) {
                    logger.severe("Error calling findSafeTeleportY callback: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Recursively searches for a safe teleport location.
     */
    private CompletableFuture<Integer> searchForSafeLocation(World world, int x, int z, int currentY, int minY) {
        if (currentY < minY + 2) {
            return CompletableFuture.completedFuture(-1);
        }

        // Check: solid at currentY-1, air at currentY, air at currentY+1
        return getBlock(world, x, currentY - 1, z).thenCompose(below -> {
            if (below == null || !below.isSolid) {
                return searchForSafeLocation(world, x, z, currentY - 1, minY);
            }
            
            return getBlock(world, x, currentY, z).thenCompose(feet -> {
                if (feet == null || !feet.isAir) {
                    return searchForSafeLocation(world, x, z, currentY - 1, minY);
                }
                
                return getBlock(world, x, currentY + 1, z).thenCompose(head -> {
                    if (head != null && head.isAir) {
                        return CompletableFuture.completedFuture(currentY);
                    }
                    return searchForSafeLocation(world, x, z, currentY - 1, minY);
                });
            });
        });
    }

    /**
     * Block information data class.
     */
    public static class BlockInfo {
        public final String type;
        public final boolean isAir;
        public final boolean isSolid;
        public final boolean isLiquid;
        public final int x;
        public final int y;
        public final int z;

        public BlockInfo(String type, boolean isAir, boolean isSolid, boolean isLiquid, int x, int y, int z) {
            this.type = type;
            this.isAir = isAir;
            this.isSolid = isSolid;
            this.isLiquid = isLiquid;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format("{\"type\":\"%s\",\"isAir\":%b,\"isSolid\":%b,\"isLiquid\":%b,\"x\":%d,\"y\":%d,\"z\":%d}",
                type, isAir, isSolid, isLiquid, x, y, z);
        }
    }
}
