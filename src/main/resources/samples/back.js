/**
 * Back System
 * Allows players to return to their previous location, especially after death.
 * 
 * Commands:
 *   /back  - Teleport to your previous location
 */

(function() {
    'use strict';
    
    var DB_FILE = 'back_locations';
    var cooldowns = Utils.createCooldownTracker('cooldowns', 'back');
    
    // In-memory cache for faster access (also persisted for restart survival)
    var locationHistory = {};
    
    // ========================================================================
    // LOCATION HISTORY MANAGEMENT
    // ========================================================================
    
    /**
     * Gets location history for a player.
     * @param {string} playerId - Player UUID
     * @returns {Array} Array of locations (most recent first)
     */
    function getHistory(playerId) {
        // Check in-memory first
        if (locationHistory[playerId]) {
            return locationHistory[playerId];
        }
        
        // Load from DB
        var data = DB.get(DB_FILE, playerId);
        if (!data) {
            locationHistory[playerId] = [];
            return [];
        }
        try {
            var history = JSON.parse(data);
            locationHistory[playerId] = history;
            return history;
        } catch (e) {
            locationHistory[playerId] = [];
            return [];
        }
    }
    
    /**
     * Saves location history for a player.
     * @param {string} playerId - Player UUID
     * @param {Array} history - Array of locations
     */
    function saveHistory(playerId, history) {
        locationHistory[playerId] = history;
        DB.save(DB_FILE, playerId, JSON.stringify(history));
    }
    
    /**
     * Adds a location to a player's history.
     * @param {string} playerId - Player UUID
     * @param {Object} location - Location to add
     */
    function addToHistory(playerId, location) {
        var history = getHistory(playerId);
        var maxHistory = Config.get('back.historySize');
        
        // Add to beginning (most recent first)
        history.unshift({
            x: location.x,
            y: location.y,
            z: location.z,
            pitch: location.pitch || 0,
            yaw: location.yaw || 0,
            worldName: location.worldName,
            timestamp: java.lang.System.currentTimeMillis(),
            reason: location.reason || 'unknown'
        });
        
        // Trim to max size
        if (history.length > maxHistory) {
            history = history.slice(0, maxHistory);
        }
        
        saveHistory(playerId, history);
    }
    
    /**
     * Gets and removes the most recent location from history.
     * @param {string} playerId - Player UUID
     * @returns {Object|null} Location or null
     */
    function popFromHistory(playerId) {
        var history = getHistory(playerId);
        if (history.length === 0) {
            return null;
        }
        
        var location = history.shift();
        saveHistory(playerId, history);
        return location;
    }
    
    // ========================================================================
    // /BACK COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('back')
        .setDescription('Return to your previous location')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            
            // Get history
            var history = getHistory(playerId);
            
            if (history.length === 0) {
                ctx.sendMessage('&cYou have no previous locations to return to.');
                return;
            }
            
            // Check cooldown
            var cooldownSeconds = Config.getPlayerSetting(playerId, 'back.cooldownSeconds');
            if (cooldowns.isOnCooldown(playerId, 'teleport', cooldownSeconds)) {
                var remaining = cooldowns.getRemainingSeconds(playerId, 'teleport', cooldownSeconds);
                ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before using /back again.');
                return;
            }
            
            // Pop the location
            var location = popFromHistory(playerId);
            if (!location) {
                ctx.sendMessage('&cCould not retrieve your previous location.');
                return;
            }
            
            var warmupSeconds = Config.getPlayerSetting(playerId, 'back.warmupSeconds');
            
            // Set cooldown
            cooldowns.setCooldown(playerId, 'teleport');
            
            // Teleport with warmup
            var reasonText = location.reason === 'death' ? 'your death location' : 'your previous location';
            Utils.teleportWithWarmup(player, location, warmupSeconds, reasonText, null);
        });
    
    // ========================================================================
    // TRACK DEATHS
    // ========================================================================
    
    Events.on('playerDeath', function(event) {
        var player = Players.getByUuid(event.playerUuid);
        if (!player) return;
        
        var playerId = event.playerUuid;
        
        // Get location on world thread
        Players.runOnWorldThread(player, function() {
            var location = Players.getFullLocation(player);
            
            if (location) {
                location.reason = 'death';
                addToHistory(playerId, location);
                Logger.debug('Saved death location for ' + Players.getUsername(player));
            }
        });
    });
    
    // ========================================================================
    // TRACK TELEPORTS (for non-death back locations)
    // ========================================================================
    
    // Note: We track significant location changes via world changes
    Events.on('playerChangeWorld', function(event) {
        var playerId = event.playerUuid;
        
        // Save their location in the old world
        if (event.fromWorld) {
            var player = Players.getByUuid(playerId);
            if (player) {
                // Get location on world thread
                Players.runOnWorldThread(player, function() {
                    var currentLocation = Players.getFullLocation(player);
                    if (currentLocation) {
                        // This is the location they left from (current position after world change)
                        // We'll store their pre-teleport location if available
                        // For now, just mark it as world change
                        currentLocation.reason = 'world_change';
                        addToHistory(playerId, currentLocation);
                    }
                });
            }
        }
    });
    
    Logger.info('Back command loaded: /back');
})();
