/**
 * Rank Management System
 * Admin commands for managing player ranks.
 * 
 * Commands:
 *   /rank set <player> <rank>  - Set a player's rank
 *   /rank get <player>         - Get a player's current rank
 *   /rank list                 - List all available ranks
 *   /rank remove <player>      - Remove a player's rank (set to default)
 */

(function() {
    'use strict';
    
    // ========================================================================
    // /RANK COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('rank')
        .setDescription('Manage player ranks (admin only)')
        .addRequiredStringArg('action', 'Action: set, get, list, or remove')
        .addOptionalStringArg('player', 'Player name')
        .addOptionalStringArg('rank', 'Rank name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to manage ranks.');
                return;
            }
            
            var action = ctx.getArgAsString('action');
            if (!action) {
                showUsage(ctx);
                return;
            }
            
            action = action.toLowerCase();
            
            switch (action) {
                case 'set':
                    handleSetRank(ctx);
                    break;
                case 'get':
                    handleGetRank(ctx);
                    break;
                case 'list':
                    handleListRanks(ctx);
                    break;
                case 'remove':
                    handleRemoveRank(ctx);
                    break;
                default:
                    showUsage(ctx);
            }
        });
    
    function showUsage(ctx) {
        ctx.sendMessage('&6=== Rank Commands ===');
        ctx.sendMessage('&f/rank set <player> <rank>&7 - Set a player\'s rank');
        ctx.sendMessage('&f/rank get <player>&7 - Get a player\'s rank');
        ctx.sendMessage('&f/rank list&7 - List all ranks');
        ctx.sendMessage('&f/rank remove <player>&7 - Remove a player\'s rank');
    }
    
    function handleSetRank(ctx) {
        var playerName = ctx.getArgAsString('player');
        var rankName = ctx.getArgAsString('rank');
        
        if (!playerName || !rankName) {
            ctx.sendMessage('&cUsage: /rank set <player> <rank>');
            return;
        }
        
        // Find the target player (can be offline, we'll use their name to look up UUID)
        var target = Players.getByNameFuzzy(playerName);
        if (!target) {
            ctx.sendMessage('&cPlayer \'' + playerName + '\' not found. Player must be online.');
            return;
        }
        
        var targetId = Players.getUuidString(target);
        var targetName = Players.getUsername(target);
        
        // Validate rank exists
        var allRanks = Config.getAllRanks();
        if (!allRanks.hasOwnProperty(rankName)) {
            var rankList = Object.keys(allRanks).join(', ');
            ctx.sendMessage('&cRank \'' + rankName + '\' not found.');
            ctx.sendMessage('&7Available ranks: &f' + rankList);
            return;
        }
        
        // Set the rank
        Config.setPlayerRank(targetId, rankName);
        
        ctx.sendMessage('&aSet &f' + targetName + '&a\'s rank to &f' + rankName + '&a.');
        MessageHelper.send(target, '&aYour rank has been set to &f' + rankName + '&a.');
    }
    
    function handleGetRank(ctx) {
        var playerName = ctx.getArgAsString('player');
        
        if (!playerName) {
            ctx.sendMessage('&cUsage: /rank get <player>');
            return;
        }
        
        var target = Players.getByNameFuzzy(playerName);
        if (!target) {
            ctx.sendMessage('&cPlayer \'' + playerName + '\' not found. Player must be online.');
            return;
        }
        
        var targetId = Players.getUuidString(target);
        var targetName = Players.getUsername(target);
        var rank = Config.getPlayerRank(targetId);
        
        ctx.sendMessage('&f' + targetName + '&7\'s rank: &f' + rank);
    }
    
    function handleListRanks(ctx) {
        var allRanks = Config.getAllRanks();
        var rankNames = Object.keys(allRanks);
        
        ctx.sendMessage('&6=== Available Ranks (' + rankNames.length + ') ===');
        
        for (var i = 0; i < rankNames.length; i++) {
            var name = rankNames[i];
            var rank = allRanks[name];
            
            // Show some rank info
            var info = [];
            if (rank.homes && rank.homes.maxHomes !== undefined) {
                info.push('homes: ' + rank.homes.maxHomes);
            }
            if (rank.homes && rank.homes.cooldownSeconds !== undefined) {
                info.push('cooldown: ' + rank.homes.cooldownSeconds + 's');
            }
            
            var infoStr = info.length > 0 ? '&7 (' + info.join(', ') + ')' : '';
            ctx.sendMessage('&f  ' + name + infoStr);
        }
    }
    
    function handleRemoveRank(ctx) {
        var playerName = ctx.getArgAsString('player');
        
        if (!playerName) {
            ctx.sendMessage('&cUsage: /rank remove <player>');
            return;
        }
        
        var target = Players.getByNameFuzzy(playerName);
        if (!target) {
            ctx.sendMessage('&cPlayer \'' + playerName + '\' not found. Player must be online.');
            return;
        }
        
        var targetId = Players.getUuidString(target);
        var targetName = Players.getUsername(target);
        
        // Set to default rank
        Config.setPlayerRank(targetId, 'default');
        
        ctx.sendMessage('&aRemoved &f' + targetName + '&a\'s rank (set to default).');
        MessageHelper.send(target, '&eYour rank has been reset to default.');
    }
    
    // ========================================================================
    // /MYRANK COMMAND (for players to check their own rank)
    // ========================================================================
    
    Commands.register()
        .setName('myrank')
        .setDescription('Check your current rank')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var rank = Config.getPlayerRank(playerId);
            
            ctx.sendMessage('&7Your rank: &f' + rank);
            
            // Show some benefits of their rank
            var maxHomes = Config.getRankSetting(rank, 'homes.maxHomes');
            var homeCooldown = Config.getRankSetting(rank, 'homes.cooldownSeconds');
            
            ctx.sendMessage('&7Benefits:');
            ctx.sendMessage('&7  Max homes: &f' + maxHomes);
            ctx.sendMessage('&7  Home cooldown: &f' + homeCooldown + 's');
        });
    
    Logger.info('Rank commands loaded: /rank, /myrank');
})();
