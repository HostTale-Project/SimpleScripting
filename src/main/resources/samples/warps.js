/**
 * Warp System
 * Server-wide warp points that any player can use.
 * 
 * Commands:
 *   /setwarp <name>  - Set a warp point (admin only)
 *   /warp <name>     - Teleport to a warp
 *   /delwarp <name>  - Delete a warp (admin only)
 *   /warps           - List all warps
 */

(function() {
    'use strict';
    
    var DB_FILE = 'warps';
    var cooldowns = Utils.createCooldownTracker('cooldowns', 'warp');
    
    // ========================================================================
    // WARP DATA MANAGEMENT
    // ========================================================================
    
    /**
     * Gets all warps.
     * @returns {Object} Map of warp name to location data
     */
    function getWarps() {
        var data = DB.get(DB_FILE, 'warps');
        if (!data) {
            return {};
        }
        try {
            return JSON.parse(data);
        } catch (e) {
            return {};
        }
    }
    
    /**
     * Saves all warps.
     * @param {Object} warps - Map of warp name to location data
     */
    function saveWarps(warps) {
        DB.save(DB_FILE, 'warps', JSON.stringify(warps));
    }
    
    // ========================================================================
    // /SETWARP COMMAND (Admin)
    // ========================================================================
    
    Commands.register()
        .setName('setwarp')
        .setDescription('Set a server warp point (admin only)')
        .addRequiredStringArg('name', 'Warp name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to set warps.');
                return;
            }
            
            var warpName = ctx.getArgAsString('name');
            if (!warpName) {
                ctx.sendMessage('&cUsage: /setwarp <name>');
                return;
            }
            
            // Get player location
            var location = Players.getFullLocation(player);
            if (!location) {
                ctx.sendMessage('&cCould not get your location.');
                return;
            }
            
            // Get existing warps
            var warps = getWarps();
            var isUpdate = warps.hasOwnProperty(warpName);
            
            // Save warp
            warps[warpName] = {
                x: location.x,
                y: location.y,
                z: location.z,
                pitch: location.pitch,
                yaw: location.yaw,
                worldName: location.worldName,
                createdBy: Players.getUsername(player),
                createdAt: java.lang.System.currentTimeMillis()
            };
            
            saveWarps(warps);
            
            var posStr = Utils.formatPosition(location.x, location.y, location.z);
            if (isUpdate) {
                ctx.sendMessage('&aWarp \'&f' + warpName + '&a\' updated at: &f' + posStr);
            } else {
                ctx.sendMessage('&aWarp \'&f' + warpName + '&a\' created at: &f' + posStr);
            }
        });
    
    // ========================================================================
    // /WARP COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('warp')
        .setDescription('Teleport to a server warp')
        .addRequiredStringArg('name', 'Warp name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var warpName = ctx.getArgAsString('name');
            
            if (!warpName) {
                ctx.sendMessage('&cUsage: /warp <name>');
                ctx.sendMessage('&7Use /warps to see available warps.');
                return;
            }
            
            // Get warps
            var warps = getWarps();
            
            if (!warps.hasOwnProperty(warpName)) {
                var warpNames = Object.keys(warps);
                if (warpNames.length === 0) {
                    ctx.sendMessage('&cNo warps have been set yet.');
                } else {
                    ctx.sendMessage('&cWarp \'' + warpName + '\' not found. Available: &f' + warpNames.join(', '));
                }
                return;
            }
            
            // Check cooldown
            var cooldownSeconds = Config.getPlayerSetting(playerId, 'warps.cooldownSeconds');
            if (cooldowns.isOnCooldown(playerId, 'teleport', cooldownSeconds)) {
                var remaining = cooldowns.getRemainingSeconds(playerId, 'teleport', cooldownSeconds);
                ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before warping again.');
                return;
            }
            
            var warp = warps[warpName];
            var warmupSeconds = Config.getPlayerSetting(playerId, 'warps.warmupSeconds');
            
            // Set cooldown
            cooldowns.setCooldown(playerId, 'teleport');
            
            // Teleport with warmup
            Utils.teleportWithWarmup(player, warp, warmupSeconds, 'warp \'' + warpName + '\'', null);
        });
    
    // ========================================================================
    // /DELWARP COMMAND (Admin)
    // ========================================================================
    
    Commands.register()
        .setName('delwarp')
        .setDescription('Delete a server warp (admin only)')
        .addRequiredStringArg('name', 'Warp name to delete')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to delete warps.');
                return;
            }
            
            var warpName = ctx.getArgAsString('name');
            if (!warpName) {
                ctx.sendMessage('&cUsage: /delwarp <name>');
                return;
            }
            
            // Get warps
            var warps = getWarps();
            
            if (!warps.hasOwnProperty(warpName)) {
                ctx.sendMessage('&cWarp \'' + warpName + '\' not found.');
                return;
            }
            
            // Delete warp
            delete warps[warpName];
            saveWarps(warps);
            
            ctx.sendMessage('&aWarp \'&f' + warpName + '&a\' deleted.');
        });
    
    // ========================================================================
    // /WARPS COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('warps')
        .setDescription('List all server warps')
        .setHandler(function(ctx) {
            var warps = getWarps();
            var warpNames = Object.keys(warps);
            
            if (warpNames.length === 0) {
                ctx.sendMessage('&eNo warps have been set yet.');
                return;
            }
            
            ctx.sendMessage('&6=== Server Warps (' + warpNames.length + ') ===');
            
            for (var i = 0; i < warpNames.length; i++) {
                var name = warpNames[i];
                var warp = warps[name];
                var posStr = Utils.formatPosition(warp.x, warp.y, warp.z);
                ctx.sendMessage('&f  ' + name + '&7 - ' + posStr + ' (' + warp.worldName + ')');
            }
        });
    
    Logger.info('Warps commands loaded: /setwarp, /warp, /delwarp, /warps');
})();
