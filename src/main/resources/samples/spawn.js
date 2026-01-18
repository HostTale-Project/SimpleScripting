/**
 * Spawn System
 * Server spawn point management.
 * 
 * Commands:
 *   /setspawn   - Set the server spawn point (admin only)
 *   /spawn      - Teleport to spawn
 */

(function() {
    'use strict';
    
    var DB_FILE = 'spawn';
    var cooldowns = Utils.createCooldownTracker('cooldowns', 'spawn');
    
    // ========================================================================
    // SPAWN DATA MANAGEMENT
    // ========================================================================
    
    /**
     * Gets the spawn point.
     * @returns {Object|null} Spawn location or null if not set
     */
    function getSpawn() {
        var data = DB.get(DB_FILE, 'spawn');
        if (!data) {
            return null;
        }
        try {
            return JSON.parse(data);
        } catch (e) {
            return null;
        }
    }
    
    /**
     * Saves the spawn point.
     * @param {Object} spawn - Spawn location data
     */
    function saveSpawn(spawn) {
        DB.save(DB_FILE, 'spawn', JSON.stringify(spawn));
    }
    
    // ========================================================================
    // /SETSPAWN COMMAND (Admin)
    // ========================================================================
    
    Commands.register()
        .setName('setspawn')
        .setDescription('Set the server spawn point (admin only)')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to set spawn.');
                return;
            }
            
            // Get player location
            var location = Players.getFullLocation(player);
            if (!location) {
                ctx.sendMessage('&cCould not get your location.');
                return;
            }
            
            // Save spawn
            var spawn = {
                x: location.x,
                y: location.y,
                z: location.z,
                pitch: location.pitch,
                yaw: location.yaw,
                worldName: location.worldName,
                setBy: Players.getUsername(player),
                setAt: java.lang.System.currentTimeMillis()
            };
            
            saveSpawn(spawn);
            
            var posStr = Utils.formatPosition(location.x, location.y, location.z);
            ctx.sendMessage('&aServer spawn point set at: &f' + posStr);
        });
    
    // ========================================================================
    // /SPAWN COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('spawn')
        .setDescription('Teleport to the server spawn')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            
            // Get spawn
            var spawn = getSpawn();
            
            if (!spawn) {
                ctx.sendMessage('&cNo spawn point has been set yet.');
                return;
            }
            
            // Check cooldown
            var cooldownSeconds = Config.getPlayerSetting(playerId, 'spawn.cooldownSeconds');
            if (cooldowns.isOnCooldown(playerId, 'teleport', cooldownSeconds)) {
                var remaining = cooldowns.getRemainingSeconds(playerId, 'teleport', cooldownSeconds);
                ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before teleporting to spawn again.');
                return;
            }
            
            var warmupSeconds = Config.getPlayerSetting(playerId, 'spawn.warmupSeconds');
            
            // Set cooldown
            cooldowns.setCooldown(playerId, 'teleport');
            
            // Teleport with warmup
            Utils.teleportWithWarmup(player, spawn, warmupSeconds, 'spawn', null);
        });
    
    Logger.info('Spawn commands loaded: /setspawn, /spawn');
})();
