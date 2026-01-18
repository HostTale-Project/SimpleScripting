/**
 * Random Teleport (RTP) System
 * Teleports players to a random location in the world.
 * 
 * Commands:
 *   /rtp  - Teleport to a random location (aliases: /randomtp, /wild)
 */

(function() {
    'use strict';
    
    var cooldowns = Utils.createCooldownTracker('cooldowns', 'rtp');
    
    // ========================================================================
    // RANDOM LOCATION GENERATION
    // ========================================================================
    
    /**
     * Generates a random number between min and max (inclusive).
     * @param {number} min - Minimum value
     * @param {number} max - Maximum value
     * @returns {number} Random number
     */
    function randomBetween(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }
    
    /**
     * Generates a random coordinate with a sign (positive or negative).
     * @param {number} minDistance - Minimum distance from 0
     * @param {number} maxDistance - Maximum distance from 0
     * @returns {number} Random coordinate
     */
    function randomCoord(minDistance, maxDistance) {
        var distance = randomBetween(minDistance, maxDistance);
        return Math.random() < 0.5 ? distance : -distance;
    }
    
    /**
     * Attempts to find a safe location for RTP.
     * @param {Object} player - The player entity
     * @param {number} minDistance - Minimum distance from spawn/player
     * @param {number} maxDistance - Maximum distance from spawn/player
     * @param {number} maxAttempts - Maximum attempts to find safe location
     * @param {function} callback - Called with (success, location)
     */
    function findRandomLocation(player, minDistance, maxDistance, maxAttempts, callback) {
        var world = Players.getWorld(player);
        if (!world) {
            callback(false, null);
            return;
        }
        
        var worldName = Worlds.getWorldName(world);
        var attempt = 0;
        
        function tryLocation() {
            if (attempt >= maxAttempts) {
                callback(false, null);
                return;
            }
            
            attempt++;
            
            // Generate random X and Z coordinates
            var x = randomCoord(minDistance, maxDistance);
            var z = randomCoord(minDistance, maxDistance);
            
            // Find safe Y coordinate
            Worlds.findSafeTeleportY(world, x, z, function(safeY) {
                if (safeY !== null && safeY > 0) {
                    // Found a safe location
                    callback(true, {
                        x: x + 0.5,  // Center of block
                        y: safeY,
                        z: z + 0.5,
                        pitch: 0,
                        yaw: Math.random() * 360,  // Random facing direction
                        worldName: worldName
                    });
                } else {
                    // Try again
                    tryLocation();
                }
            });
        }
        
        tryLocation();
    }
    
    // ========================================================================
    // /RTP COMMAND
    // ========================================================================
    
    function rtpHandler(ctx) {
        var player = Utils.getPlayerFromContext(ctx);
        if (!player) return;
        
        var playerId = Players.getUuidString(player);
        
        // Check cooldown
        var cooldownSeconds = Config.getPlayerSetting(playerId, 'rtp.cooldownSeconds');
        if (cooldowns.isOnCooldown(playerId, 'teleport', cooldownSeconds)) {
            var remaining = cooldowns.getRemainingSeconds(playerId, 'teleport', cooldownSeconds);
            ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before using /rtp again.');
            return;
        }
        
        var minDistance = Config.get('rtp.minDistance');
        var maxDistance = Config.get('rtp.maxDistance');
        var maxAttempts = Config.get('rtp.maxAttempts');
        var warmupSeconds = Config.getPlayerSetting(playerId, 'rtp.warmupSeconds');
        
        ctx.sendMessage('&eFinding a random location...');
        
        // Set cooldown immediately to prevent spam
        cooldowns.setCooldown(playerId, 'teleport');
        
        findRandomLocation(player, minDistance, maxDistance, maxAttempts, function(success, location) {
            // Re-verify player is still online
            var currentPlayer = Players.getByUuid(playerId);
            if (!currentPlayer) {
                return; // Player logged off
            }
            
            if (!success || !location) {
                currentPlayer.sendMessage(MessageHelper.raw('&cCould not find a safe location after ' + maxAttempts + ' attempts. Please try again.'));
                return;
            }
            
            var posStr = Utils.formatPosition(location.x, location.y, location.z);
            currentPlayer.sendMessage(MessageHelper.raw('&aFound a safe location at: &f' + posStr));
            
            // Teleport with warmup
            Utils.teleportWithWarmup(currentPlayer, location, warmupSeconds, 'random location', null);
        });
    }
    
    // Register main command
    Commands.register()
        .setName('rtp')
        .setDescription('Teleport to a random location')
        .setHandler(rtpHandler);
    
    // Register aliases
    Commands.register()
        .setName('randomtp')
        .setDescription('Teleport to a random location (alias for /rtp)')
        .setHandler(rtpHandler);
    
    Commands.register()
        .setName('wild')
        .setDescription('Teleport to a random location (alias for /rtp)')
        .setHandler(rtpHandler);
    
    Logger.info('RTP commands loaded: /rtp, /randomtp, /wild');
})();
