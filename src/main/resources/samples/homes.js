/**
 * Home System
 * Allows players to set, teleport to, and manage personal home locations.
 * 
 * Commands:
 *   /sethome [name]  - Set a home at current location
 *   /home [name]     - Teleport to a home
 *   /delhome <name>  - Delete a home
 *   /homes           - List all homes
 */

(function() {
    'use strict';
    
    var DB_FILE = 'player_homes';
    var cooldowns = Utils.createCooldownTracker('cooldowns', 'home');
    
    // ========================================================================
    // HOME DATA MANAGEMENT
    // ========================================================================
    
    /**
     * Gets all homes for a player.
     * @param {string} playerId - Player UUID
     * @returns {Object} Map of home name to location data
     */
    function getPlayerHomes(playerId) {
        var data = DB.get(DB_FILE, playerId);
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
     * Saves all homes for a player.
     * @param {string} playerId - Player UUID
     * @param {Object} homes - Map of home name to location data
     */
    function savePlayerHomes(playerId, homes) {
        DB.save(DB_FILE, playerId, JSON.stringify(homes));
    }
    
    /**
     * Gets the number of homes a player has.
     * @param {string} playerId - Player UUID
     * @returns {number} Home count
     */
    function getHomeCount(playerId) {
        var homes = getPlayerHomes(playerId);
        return Object.keys(homes).length;
    }
    
    /**
     * Gets the max homes allowed for a player.
     * @param {string} playerId - Player UUID
     * @returns {number} Max homes
     */
    function getMaxHomes(playerId) {
        return Config.getPlayerSetting(playerId, 'homes.maxHomes');
    }
    
    // ========================================================================
    // /SETHOME COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('sethome')
        .setDescription('Set a home at your current location')
        .addOptionalStringArg('name', 'Home name (default: home)')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var homeName = ctx.getArgAsString('name', 'home');
            
            // Get current homes
            var homes = getPlayerHomes(playerId);
            var homeCount = Object.keys(homes).length;
            var maxHomes = getMaxHomes(playerId);
            
            // Check if updating existing home or creating new
            var isUpdate = homes.hasOwnProperty(homeName);
            
            if (!isUpdate && homeCount >= maxHomes) {
                ctx.sendMessage('&cYou have reached your home limit (' + maxHomes + '). Delete a home first.');
                return;
            }
            
            // Get player location
            var location = Players.getFullLocation(player);
            if (!location) {
                ctx.sendMessage('&cCould not get your location.');
                return;
            }
            
            // Save home
            homes[homeName] = {
                x: location.x,
                y: location.y,
                z: location.z,
                pitch: location.pitch,
                yaw: location.yaw,
                worldName: location.worldName,
                createdAt: java.lang.System.currentTimeMillis()
            };
            
            savePlayerHomes(playerId, homes);
            
            var posStr = Utils.formatPosition(location.x, location.y, location.z);
            if (isUpdate) {
                ctx.sendMessage('&aHome \'&f' + homeName + '&a\' updated at: &f' + posStr);
            } else {
                ctx.sendMessage('&aHome \'&f' + homeName + '&a\' set at: &f' + posStr);
            }
        });
    
    // ========================================================================
    // /HOME COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('home')
        .setDescription('Teleport to a home')
        .addOptionalStringArg('name', 'Home name (default: home)')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var homeName = ctx.getArgAsString('name', 'home');
            
            // Get player's homes
            var homes = getPlayerHomes(playerId);
            
            if (!homes.hasOwnProperty(homeName)) {
                // Check if they have any homes
                var homeNames = Object.keys(homes);
                if (homeNames.length === 0) {
                    ctx.sendMessage('&cYou don\'t have any homes. Use &f/sethome&c to create one.');
                } else {
                    ctx.sendMessage('&cHome \'' + homeName + '\' not found. Your homes: &f' + homeNames.join(', '));
                }
                return;
            }
            
            // Check cooldown
            var cooldownSeconds = Config.getPlayerSetting(playerId, 'homes.cooldownSeconds');
            if (cooldowns.isOnCooldown(playerId, 'teleport', cooldownSeconds)) {
                var remaining = cooldowns.getRemainingSeconds(playerId, 'teleport', cooldownSeconds);
                ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before teleporting again.');
                return;
            }
            
            var home = homes[homeName];
            var warmupSeconds = Config.getPlayerSetting(playerId, 'homes.warmupSeconds');
            
            // Set cooldown before teleport
            cooldowns.setCooldown(playerId, 'teleport');
            
            // Teleport with warmup
            Utils.teleportWithWarmup(player, home, warmupSeconds, 'home \'' + homeName + '\'', null);
        });
    
    // ========================================================================
    // /DELHOME COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('delhome')
        .setDescription('Delete a home')
        .addRequiredStringArg('name', 'Home name to delete')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var homeName = ctx.getArgAsString('name');
            
            if (!homeName) {
                ctx.sendMessage('&cUsage: /delhome <name>');
                return;
            }
            
            // Get player's homes
            var homes = getPlayerHomes(playerId);
            
            if (!homes.hasOwnProperty(homeName)) {
                ctx.sendMessage('&cHome \'' + homeName + '\' not found.');
                return;
            }
            
            // Delete home
            delete homes[homeName];
            savePlayerHomes(playerId, homes);
            
            ctx.sendMessage('&aHome \'&f' + homeName + '&a\' deleted.');
        });
    
    // ========================================================================
    // /HOMES COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('homes')
        .setDescription('List all your homes')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var homes = getPlayerHomes(playerId);
            var homeNames = Object.keys(homes);
            var maxHomes = getMaxHomes(playerId);
            
            if (homeNames.length === 0) {
                ctx.sendMessage('&eYou don\'t have any homes. Use &f/sethome&e to create one.');
                return;
            }
            
            ctx.sendMessage('&6=== Your Homes (' + homeNames.length + '/' + maxHomes + ') ===');
            
            for (var i = 0; i < homeNames.length; i++) {
                var name = homeNames[i];
                var home = homes[name];
                var posStr = Utils.formatPosition(home.x, home.y, home.z);
                ctx.sendMessage('&f  ' + name + '&7 - ' + posStr + ' (' + home.worldName + ')');
            }
        });
    
    Logger.info('Homes commands loaded: /sethome, /home, /delhome, /homes');
})();
