/**
 * Admin Commands
 * Administrative utilities for server management.
 * 
 * Commands:
 *   /simplescripting reload  - Reload all scripts
 *   /simplescripting info    - Show plugin info
 *   /tp <player>             - Teleport to a player
 *   /tphere <player>         - Teleport a player to you
 *   /playerinfo <player>     - Show player information
 */

(function() {
    'use strict';
    
    // ========================================================================
    // /SIMPLESCRIPTING COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('simplescripting')
        .setDescription('SimpleScripting admin commands')
        .addRequiredStringArg('action', 'Action: reload or info')
        .setHandler(function(ctx) {
            var action = ctx.getArgAsString('action');
            
            if (!action) {
                showPluginUsage(ctx);
                return;
            }
            
            action = action.toLowerCase();
            
            if (action === 'reload') {
                handleReload(ctx);
                return;
            }

            if (action === 'info') {
                handleInfo(ctx);
                return;
            }

            showPluginUsage(ctx);
        });
    
    function showPluginUsage(ctx) {
        ctx.sendMessage('&6=== SimpleScripting Commands ===');
        ctx.sendMessage('&f/simplescripting reload&7 - Reload all scripts');
        ctx.sendMessage('&f/simplescripting info&7 - Show plugin info');
    }
    
    function handleReload(ctx) {
        // Check permission
        var player = Utils.getPlayerFromContext(ctx);
        if (player && !Permissions.has(player, 'simplescripting.admin')) {
            ctx.sendMessage('&cYou don\'t have permission to reload scripts.');
            return;
        }
        
        ctx.sendMessage('&eReloading all scripts...');
        
        try {
            // The plugin exposes a reload method via the Plugin object
            if (typeof Plugin !== 'undefined' && Plugin.reload) {
                var count = Plugin.reload();
                ctx.sendMessage('&aReloaded ' + count + ' scripts successfully!');
            } else {
                ctx.sendMessage('&cReload functionality not available.');
            }
        } catch (e) {
            ctx.sendMessage('&cError reloading scripts: ' + e.message);
            Logger.error('Reload error: ' + e.message);
        }
    }
    
    function handleInfo(ctx) {
        ctx.sendMessage('&6=== SimpleScripting ===');
        ctx.sendMessage('&7Version: &f1.0.0');
        ctx.sendMessage('&7JavaScript Engine: &fMozilla Rhino');
        ctx.sendMessage('&7Scripts Directory: &funiverse/SimpleScripting/mods/');
        ctx.sendMessage('&7Database Directory: &funiverse/SimpleScripting/db/');
        
        // Show loaded features
        ctx.sendMessage('&6=== Available APIs ===');
        ctx.sendMessage('&fCommands, Events, DB, Teleport, Players, Worlds');
        ctx.sendMessage('&fScheduler, Permissions, MessageHelper, Logger, Colors');
    }
    
    // ========================================================================
    // /TP COMMAND (Admin)
    // ========================================================================
    
    Commands.register()
        .setName('tp')
        .setDescription('Teleport to a player (admin only)')
        .addRequiredStringArg('player', 'Target player name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to use /tp.');
                return;
            }
            
            var targetName = ctx.getArgAsString('player');
            if (!targetName) {
                ctx.sendMessage('&cUsage: /tp <player>');
                return;
            }
            
            var target = Players.getByNameFuzzy(targetName);
            if (!target) {
                ctx.sendMessage('&cPlayer \'' + targetName + '\' not found.');
                return;
            }
            
            var targetPlayerName = Players.getUsername(target);
            
            // Can't teleport to yourself
            if (Players.getUuidString(target) === Players.getUuidString(player)) {
                ctx.sendMessage('&cYou cannot teleport to yourself.');
                return;
            }
            
            // Teleport immediately (no warmup for admins)
            Teleport.teleportToPlayer(player, target);
            ctx.sendMessage('&aTeleported to &f' + targetPlayerName + '&a.');
        });
    
    // ========================================================================
    // /TPHERE COMMAND (Admin)
    // ========================================================================
    
    Commands.register()
        .setName('tphere')
        .setDescription('Teleport a player to you (admin only)')
        .addRequiredStringArg('player', 'Target player name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to use /tphere.');
                return;
            }
            
            var targetName = ctx.getArgAsString('player');
            if (!targetName) {
                ctx.sendMessage('&cUsage: /tphere <player>');
                return;
            }
            
            var target = Players.getByNameFuzzy(targetName);
            if (!target) {
                ctx.sendMessage('&cPlayer \'' + targetName + '\' not found.');
                return;
            }
            
            var targetPlayerName = Players.getUsername(target);
            
            // Can't teleport yourself
            if (Players.getUuidString(target) === Players.getUuidString(player)) {
                ctx.sendMessage('&cYou cannot teleport yourself to yourself.');
                return;
            }
            
            // Teleport target to us
            Teleport.teleportToPlayer(target, player);
            ctx.sendMessage('&aTeleported &f' + targetPlayerName + '&a to you.');
            MessageHelper.send(target, '&eYou have been teleported to &f' + Players.getUsername(player) + '&e.');
        });
    
    // ========================================================================
    // /PLAYERINFO COMMAND (Admin)
    // ========================================================================
    
    Commands.register()
        .setName('playerinfo')
        .setDescription('Show player information (admin only)')
        .addRequiredStringArg('player', 'Target player name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            // Check admin permission
            if (!Permissions.has(player, 'simplescripting.admin')) {
                ctx.sendMessage('&cYou don\'t have permission to view player info.');
                return;
            }
            
            var targetName = ctx.getArgAsString('player');
            if (!targetName) {
                ctx.sendMessage('&cUsage: /playerinfo <player>');
                return;
            }
            
            var target = Players.getByNameFuzzy(targetName);
            if (!target) {
                ctx.sendMessage('&cPlayer \'' + targetName + '\' not found.');
                return;
            }
            
            var targetId = Players.getUuidString(target);
            var targetPlayerName = Players.getUsername(target);
            var location = Players.getFullLocation(target);
            var rank = Config.getPlayerRank(targetId);
            
            ctx.sendMessage('&6=== Player Info: ' + targetPlayerName + ' ===');
            ctx.sendMessage('&7UUID: &f' + targetId);
            ctx.sendMessage('&7Rank: &f' + rank);
            
            if (location) {
                var posStr = Utils.formatPosition(location.x, location.y, location.z);
                ctx.sendMessage('&7Location: &f' + posStr);
                ctx.sendMessage('&7World: &f' + location.worldName);
                ctx.sendMessage('&7Rotation: &fpitch=' + Math.round(location.pitch) + ', yaw=' + Math.round(location.yaw));
            }
        });
    
    // ========================================================================
    // /PLAYERS COMMAND (list online players)
    // ========================================================================
    
    Commands.register()
        .setName('players')
        .setDescription('List online players')
        .setHandler(function(ctx) {
            var onlinePlayers = Players.getOnlinePlayers();
            
            if (!onlinePlayers || onlinePlayers.length === 0) {
                ctx.sendMessage('&eNo players online.');
                return;
            }
            
            var names = [];
            for (var i = 0; i < onlinePlayers.length; i++) {
                names.push(Players.getUsername(onlinePlayers[i]));
            }
            
            ctx.sendMessage('&6=== Online Players (' + names.length + ') ===');
            ctx.sendMessage('&f' + names.join(', '));
        });
    
    Logger.info('Admin commands loaded: /simplescripting, /tp, /tphere, /playerinfo, /players');
})();
