/**
 * SimpleScripting Utility Library
 * Common utilities for cooldowns, warmups, formatting, and teleportation.
 * 
 * This file is loaded first (from lib/ folder) and provides shared utilities
 * that can be used by all feature scripts.
 */

// ============================================================================
// GLOBAL UTILITIES NAMESPACE
// ============================================================================

var Utils = (function() {
    'use strict';
    
    // ========================================================================
    // TIME FORMATTING
    // ========================================================================
    
    /**
     * Formats seconds into a human-readable string.
     * @param {number} seconds - Number of seconds
     * @returns {string} Formatted time string (e.g., "1m 30s")
     */
    function formatTime(seconds) {
        if (seconds < 60) {
            return seconds + 's';
        }
        var minutes = Math.floor(seconds / 60);
        var secs = seconds % 60;
        if (secs === 0) {
            return minutes + 'm';
        }
        return minutes + 'm ' + secs + 's';
    }
    
    /**
     * Formats a position for display.
     * @param {number} x - X coordinate
     * @param {number} y - Y coordinate
     * @param {number} z - Z coordinate
     * @returns {string} Formatted position string
     */
    function formatPosition(x, y, z) {
        return Math.floor(x) + ', ' + Math.floor(y) + ', ' + Math.floor(z);
    }
    
    /**
     * Formats a location object for display.
     * @param {Object} location - Location with x, y, z properties
     * @returns {string} Formatted position string
     */
    function formatLocation(location) {
        return formatPosition(location.x, location.y, location.z);
    }
    
    // ========================================================================
    // COOLDOWN TRACKING
    // ========================================================================
    
    /**
     * Creates a cooldown tracker that persists to the database.
     * @param {string} dbFile - Database file name (without .json)
     * @param {string} prefix - Prefix for cooldown keys
     * @returns {Object} Cooldown tracker with check, set, and remaining methods
     */
    function createCooldownTracker(dbFile, prefix) {
        return {
            /**
             * Checks if a player is on cooldown.
             * @param {string} playerId - Player UUID
             * @param {string} action - Action name
             * @param {number} cooldownSeconds - Cooldown duration in seconds
             * @returns {boolean} True if on cooldown
             */
            isOnCooldown: function(playerId, action, cooldownSeconds) {
                var key = prefix + ':' + playerId + ':' + action;
                var lastUse = DB.get(dbFile, key);
                
                if (!lastUse) {
                    return false;
                }
                
                var lastTime = parseInt(lastUse, 10);
                var now = java.lang.System.currentTimeMillis();
                var elapsed = (now - lastTime) / 1000;
                
                return elapsed < cooldownSeconds;
            },
            
            /**
             * Gets remaining cooldown time in seconds.
             * @param {string} playerId - Player UUID
             * @param {string} action - Action name
             * @param {number} cooldownSeconds - Cooldown duration in seconds
             * @returns {number} Remaining seconds, or 0 if not on cooldown
             */
            getRemainingSeconds: function(playerId, action, cooldownSeconds) {
                var key = prefix + ':' + playerId + ':' + action;
                var lastUse = DB.get(dbFile, key);
                
                if (!lastUse) {
                    return 0;
                }
                
                var lastTime = parseInt(lastUse, 10);
                var now = java.lang.System.currentTimeMillis();
                var elapsed = (now - lastTime) / 1000;
                var remaining = cooldownSeconds - elapsed;
                
                return remaining > 0 ? Math.ceil(remaining) : 0;
            },
            
            /**
             * Starts/resets a cooldown for a player.
             * @param {string} playerId - Player UUID
             * @param {string} action - Action name
             */
            setCooldown: function(playerId, action) {
                var key = prefix + ':' + playerId + ':' + action;
                var now = java.lang.System.currentTimeMillis();
                DB.save(dbFile, key, String(now));
            },
            
            /**
             * Clears a cooldown for a player.
             * @param {string} playerId - Player UUID
             * @param {string} action - Action name
             */
            clearCooldown: function(playerId, action) {
                var key = prefix + ':' + playerId + ':' + action;
                DB.delete(dbFile, key);
            }
        };
    }
    
    // ========================================================================
    // WARMUP MANAGEMENT
    // ========================================================================
    
    // Track active warmups (in-memory)
    var activeWarmups = {};
    
    /**
     * Creates a warmup manager for delayed teleportation with movement cancellation.
     * Uses World thread execution for safe position checking.
     * @returns {Object} Warmup manager
     */
    function createWarmupManager() {
        return {
            /**
             * Starts a warmup for a player.
             * @param {Object} player - PlayerRef
             * @param {number} seconds - Warmup duration in seconds
             * @param {Function} onComplete - Called when warmup completes successfully
             * @param {Function} onCancel - Called if warmup is cancelled (movement)
             * @param {string} message - Message to show when starting warmup
             * @returns {string} Warmup ID for cancellation
             */
            start: function(player, seconds, onComplete, onCancel, message) {
                var playerId = Players.getUuidString(player);
                var warmupId = playerId + ':' + java.lang.System.currentTimeMillis();
                var self = this;
                
                // Cancel any existing warmup
                this.cancel(playerId);
                
                // We need to get the starting position on the world thread
                Players.runOnWorldThread(player, function() {
                    var startPos = Players.getPosition(player);
                    if (!startPos) {
                        if (onCancel) onCancel('Could not get position');
                        return;
                    }
                    
                    // Show warmup message
                    if (message) {
                        player.sendMessage(MessageHelper.raw('&e' + message));
                    }
                    
                    // Store warmup info (can be accessed from any thread)
                    var warmupInfo = {
                        id: warmupId,
                        player: player,
                        startX: startPos.x,
                        startY: startPos.y,
                        startZ: startPos.z,
                        onComplete: onComplete,
                        onCancel: onCancel,
                        schedulerId: null,
                        moveCheckId: null,
                        cancelled: false
                    };
                    
                    activeWarmups[playerId] = warmupInfo;
                    
                    // Schedule completion
                    warmupInfo.schedulerId = Scheduler.runLater(function() {
                        if (warmupInfo.cancelled) return;
                        
                        // Clean up movement check
                        if (warmupInfo.moveCheckId) {
                            Scheduler.cancel(warmupInfo.moveCheckId);
                        }
                        delete activeWarmups[playerId];
                        
                        if (onComplete) {
                            onComplete();
                        }
                    }, seconds * 20); // Convert to ticks
                    
                    // Schedule movement checks - run on world thread for safe position access
                    warmupInfo.moveCheckId = Scheduler.runRepeating(function() {
                        if (warmupInfo.cancelled) return;
                        
                        // Check position on world thread
                        Players.runOnWorldThread(player, function() {
                            if (warmupInfo.cancelled) return;
                            
                            var currentPos = Players.getPosition(player);
                            if (!currentPos) {
                                // Player might have disconnected
                                self.cancel(playerId);
                                return;
                            }
                            
                            var dx = Math.abs(currentPos.x - warmupInfo.startX);
                            var dy = Math.abs(currentPos.y - warmupInfo.startY);
                            var dz = Math.abs(currentPos.z - warmupInfo.startZ);
                            
                            // Movement threshold of 0.5 blocks
                            if (dx > 0.5 || dy > 0.5 || dz > 0.5) {
                                // Player moved - cancel warmup
                                warmupInfo.cancelled = true;
                                Scheduler.cancel(warmupInfo.schedulerId);
                                Scheduler.cancel(warmupInfo.moveCheckId);
                                delete activeWarmups[playerId];
                                
                                player.sendMessage(MessageHelper.raw('&cTeleport cancelled - you moved!'));
                                
                                if (warmupInfo.onCancel) {
                                    warmupInfo.onCancel('moved');
                                }
                            }
                        });
                    }, 4, 4); // Check every 4 ticks (200ms)
                });
                
                return warmupId;
            },
            
            /**
             * Cancels an active warmup for a player.
             * @param {string} playerId - Player UUID
             * @returns {boolean} True if a warmup was cancelled
             */
            cancel: function(playerId) {
                var warmup = activeWarmups[playerId];
                if (warmup) {
                    warmup.cancelled = true;
                    if (warmup.schedulerId) {
                        Scheduler.cancel(warmup.schedulerId);
                    }
                    if (warmup.moveCheckId) {
                        Scheduler.cancel(warmup.moveCheckId);
                    }
                    delete activeWarmups[playerId];
                    return true;
                }
                return false;
            },
            
            /**
             * Checks if a player has an active warmup.
             * @param {string} playerId - Player UUID
             * @returns {boolean} True if warmup is active
             */
            hasActiveWarmup: function(playerId) {
                return !!activeWarmups[playerId];
            }
        };
    }
    
    // ========================================================================
    // PLAYER UTILITIES
    // ========================================================================
    
    /**
     * Gets a player from command context, ensuring it's a player (not console).
     * @param {Object} ctx - Command context wrapper
     * @returns {Object|null} PlayerRef or null if not a player
     */
    function getPlayerFromContext(ctx) {
        // With AbstractPlayerCommand, playerRef is directly available
        var player = ctx.getPlayer();
        
        if (!player) {
            ctx.sendMessage('&cThis command can only be used by players.');
            return null;
        }
        
        return player;
    }
    
    /**
     * Checks if a player has permission and sends error if not.
     * @param {Object} player - PlayerRef
     * @param {string} permission - Permission node
     * @returns {boolean} True if has permission
     */
    function requirePermission(player, permission) {
        return Permissions.require(player, permission);
    }
    
    // ========================================================================
    // LOCATION UTILITIES
    // ========================================================================
    
    /**
     * Saves a location to the database.
     * @param {string} dbFile - Database file name
     * @param {string} key - Storage key
     * @param {Object} location - Location with x, y, z, pitch, yaw, worldName
     */
    function saveLocation(dbFile, key, location) {
        var data = JSON.stringify({
            x: location.x,
            y: location.y,
            z: location.z,
            pitch: location.pitch || 0,
            yaw: location.yaw || 0,
            worldName: location.worldName,
            timestamp: java.lang.System.currentTimeMillis()
        });
        DB.save(dbFile, key, data);
    }
    
    /**
     * Loads a location from the database.
     * @param {string} dbFile - Database file name
     * @param {string} key - Storage key
     * @returns {Object|null} Location object or null if not found
     */
    function loadLocation(dbFile, key) {
        var data = DB.get(dbFile, key);
        if (!data) {
            return null;
        }
        try {
            return JSON.parse(data);
        } catch (e) {
            Logger.warning('Failed to parse location data: ' + e.message);
            return null;
        }
    }
    
    /**
     * Teleports a player to a location with optional warmup.
     * @param {Object} player - PlayerRef
     * @param {Object} location - Location with x, y, z, worldName
     * @param {number} warmupSeconds - Warmup time (0 for instant)
     * @param {string} destinationName - Name for display
     * @param {Function} onComplete - Called after successful teleport
     */
    function teleportWithWarmup(player, location, warmupSeconds, destinationName, onComplete) {
        var warmupManager = createWarmupManager();
        
        if (warmupSeconds <= 0) {
            // Instant teleport - use thread-safe version
            Teleport.teleportFromAnyThread(
                player,
                location.x, location.y, location.z,
                location.pitch || 0, location.yaw || 0,
                location.worldName
            );
            
            player.sendMessage(MessageHelper.raw('&aTeleported to ' + destinationName + '!'));
            if (onComplete) onComplete();
            return;
        }
        
        // Warmup teleport
        var message = 'Teleporting to ' + destinationName + ' in ' + warmupSeconds + ' seconds... Don\'t move!';
        
        warmupManager.start(player, warmupSeconds, function() {
            // Use thread-safe teleport since we're called from scheduler thread
            Teleport.teleportFromAnyThread(
                player,
                location.x, location.y, location.z,
                location.pitch || 0, location.yaw || 0,
                location.worldName
            );
            
            player.sendMessage(MessageHelper.raw('&aTeleported to ' + destinationName + '!'));
            if (onComplete) onComplete();
        }, null, message);
    }
    
    // ========================================================================
    // PUBLIC API
    // ========================================================================
    
    return {
        // Time formatting
        formatTime: formatTime,
        formatPosition: formatPosition,
        formatLocation: formatLocation,
        
        // Cooldowns
        createCooldownTracker: createCooldownTracker,
        
        // Warmups
        createWarmupManager: createWarmupManager,
        
        // Player utilities
        getPlayerFromContext: getPlayerFromContext,
        requirePermission: requirePermission,
        
        // Location utilities
        saveLocation: saveLocation,
        loadLocation: loadLocation,
        teleportWithWarmup: teleportWithWarmup
    };
})();

// Log that utilities are loaded
Logger.info('Utils library loaded');
