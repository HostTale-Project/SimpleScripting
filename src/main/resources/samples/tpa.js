/**
 * Teleport Request System (TPA)
 * Allows players to request teleportation to each other.
 * 
 * Commands:
 *   /tpa <player>      - Request to teleport to another player
 *   /tpahere <player>  - Request another player to teleport to you
 *   /tpaccept          - Accept a teleport request
 *   /tpdeny             - Deny a teleport request
 *   /tpcancel          - Cancel your outgoing teleport request
 */

(function() {
    'use strict';
    
    // Request storage: targetPlayerId -> { fromPlayerId, fromPlayerName, toPlayer: true/false, timestamp }
    var pendingRequests = {};
    var cooldowns = Utils.createCooldownTracker('cooldowns', 'tpa');
    
    // ========================================================================
    // REQUEST MANAGEMENT
    // ========================================================================
    
    /**
     * Gets the pending request for a player.
     * @param {string} targetPlayerId - UUID of the target player
     * @returns {Object|null} The request or null
     */
    function getPendingRequest(targetPlayerId) {
        var request = pendingRequests[targetPlayerId];
        if (!request) {
            return null;
        }
        
        // Check if expired
        var timeoutMs = Config.get('tpa.timeoutSeconds') * 1000;
        var elapsed = java.lang.System.currentTimeMillis() - request.timestamp;
        
        if (elapsed > timeoutMs) {
            delete pendingRequests[targetPlayerId];
            return null;
        }
        
        return request;
    }
    
    /**
     * Creates a new teleport request.
     * @param {string} fromPlayerId - UUID of the requester
     * @param {string} fromPlayerName - Name of the requester
     * @param {string} targetPlayerId - UUID of the target
     * @param {boolean} toRequester - If true, target teleports to requester (tpahere)
     */
    function createRequest(fromPlayerId, fromPlayerName, targetPlayerId, toRequester) {
        pendingRequests[targetPlayerId] = {
            fromPlayerId: fromPlayerId,
            fromPlayerName: fromPlayerName,
            toRequester: toRequester,
            timestamp: java.lang.System.currentTimeMillis()
        };
    }
    
    /**
     * Gets the outgoing request from a player.
     * @param {string} fromPlayerId - UUID of the requester
     * @returns {Object|null} Object with targetPlayerId and request, or null
     */
    function getOutgoingRequest(fromPlayerId) {
        for (var targetId in pendingRequests) {
            if (pendingRequests.hasOwnProperty(targetId)) {
                var request = getPendingRequest(targetId);
                if (request && request.fromPlayerId === fromPlayerId) {
                    return { targetPlayerId: targetId, request: request };
                }
            }
        }
        return null;
    }
    
    /**
     * Cancels any outgoing request from a player.
     * @param {string} fromPlayerId - UUID of the requester
     * @returns {boolean} True if a request was cancelled
     */
    function cancelOutgoingRequest(fromPlayerId) {
        for (var targetId in pendingRequests) {
            if (pendingRequests.hasOwnProperty(targetId)) {
                var request = pendingRequests[targetId];
                if (request && request.fromPlayerId === fromPlayerId) {
                    delete pendingRequests[targetId];
                    return true;
                }
            }
        }
        return false;
    }
    
    // ========================================================================
    // /TPA COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('tpa')
        .setDescription('Request to teleport to another player')
        .addRequiredStringArg('player', 'Target player name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var playerName = Players.getUsername(player);
            var targetName = ctx.getArgAsString('player');
            
            if (!targetName) {
                ctx.sendMessage('&cUsage: /tpa <player>');
                return;
            }
            
            // Find target player
            var target = Players.getByNameFuzzy(targetName);
            if (!target) {
                ctx.sendMessage('&cPlayer \'' + targetName + '\' not found or not online.');
                return;
            }
            
            var targetId = Players.getUuidString(target);
            var targetPlayerName = Players.getUsername(target);
            
            // Can't teleport to yourself
            if (targetId === playerId) {
                ctx.sendMessage('&cYou cannot teleport to yourself.');
                return;
            }
            
            // Check cooldown
            var cooldownSeconds = Config.getPlayerSetting(playerId, 'tpa.cooldownSeconds');
            if (cooldowns.isOnCooldown(playerId, 'request', cooldownSeconds)) {
                var remaining = cooldowns.getRemainingSeconds(playerId, 'request', cooldownSeconds);
                ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before sending another request.');
                return;
            }
            
            // Cancel any existing outgoing request
            cancelOutgoingRequest(playerId);
            
            // Create the request
            createRequest(playerId, playerName, targetId, false);
            cooldowns.setCooldown(playerId, 'request');
            
            var timeoutSeconds = Config.get('tpa.timeoutSeconds');
            
            ctx.sendMessage('&aTeleport request sent to &f' + targetPlayerName + '&a.');
            ctx.sendMessage('&7Request expires in ' + timeoutSeconds + ' seconds.');
            
            // Notify target
            MessageHelper.send(target, '&6=== Teleport Request ===');
            MessageHelper.send(target, '&f' + playerName + '&e wants to teleport to you.');
            MessageHelper.send(target, '&7Use &f/tpaccept&7 to accept or &f/tpdeny&7 to deny.');
            MessageHelper.send(target, '&7Request expires in ' + timeoutSeconds + ' seconds.');
        });
    
    // ========================================================================
    // /TPAHERE COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('tpahere')
        .setDescription('Request another player to teleport to you')
        .addRequiredStringArg('player', 'Target player name')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var playerName = Players.getUsername(player);
            var targetName = ctx.getArgAsString('player');
            
            if (!targetName) {
                ctx.sendMessage('&cUsage: /tpahere <player>');
                return;
            }
            
            // Find target player
            var target = Players.getByNameFuzzy(targetName);
            if (!target) {
                ctx.sendMessage('&cPlayer \'' + targetName + '\' not found or not online.');
                return;
            }
            
            var targetId = Players.getUuidString(target);
            var targetPlayerName = Players.getUsername(target);
            
            // Can't teleport to yourself
            if (targetId === playerId) {
                ctx.sendMessage('&cYou cannot teleport to yourself.');
                return;
            }
            
            // Check cooldown
            var cooldownSeconds = Config.getPlayerSetting(playerId, 'tpa.cooldownSeconds');
            if (cooldowns.isOnCooldown(playerId, 'request', cooldownSeconds)) {
                var remaining = cooldowns.getRemainingSeconds(playerId, 'request', cooldownSeconds);
                ctx.sendMessage('&eYou must wait ' + remaining + ' seconds before sending another request.');
                return;
            }
            
            // Cancel any existing outgoing request
            cancelOutgoingRequest(playerId);
            
            // Create the request (toRequester = true)
            createRequest(playerId, playerName, targetId, true);
            cooldowns.setCooldown(playerId, 'request');
            
            var timeoutSeconds = Config.get('tpa.timeoutSeconds');
            
            ctx.sendMessage('&aTeleport request sent to &f' + targetPlayerName + '&a.');
            ctx.sendMessage('&7Request expires in ' + timeoutSeconds + ' seconds.');
            
            // Notify target
            MessageHelper.send(target, '&6=== Teleport Request ===');
            MessageHelper.send(target, '&f' + playerName + '&e wants you to teleport to them.');
            MessageHelper.send(target, '&7Use &f/tpaccept&7 to accept or &f/tpdeny&7 to deny.');
            MessageHelper.send(target, '&7Request expires in ' + timeoutSeconds + ' seconds.');
        });
    
    // ========================================================================
    // /TPACCEPT COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('tpaccept')
        .setDescription('Accept a teleport request')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            
            // Get pending request
            var request = getPendingRequest(playerId);
            if (!request) {
                ctx.sendMessage('&cYou have no pending teleport requests.');
                return;
            }
            
            // Find the requester
            var requester = Players.getByUuid(request.fromPlayerId);
            if (!requester) {
                ctx.sendMessage('&cThe player who sent the request is no longer online.');
                delete pendingRequests[playerId];
                return;
            }
            
            // Remove the request
            delete pendingRequests[playerId];
            
            var warmupSeconds = Config.getPlayerSetting(playerId, 'tpa.warmupSeconds');
            
            if (request.toRequester) {
                // Target (accepter) teleports to requester
                var requesterLocation = Players.getFullLocation(requester);
                if (!requesterLocation) {
                    ctx.sendMessage('&cCould not get the requester\'s location.');
                    return;
                }
                
                ctx.sendMessage('&aTeleport request accepted. Teleporting to &f' + request.fromPlayerName + '&a...');
                MessageHelper.send(requester, '&a' + Players.getUsername(player) + ' accepted your teleport request.');
                
                Utils.teleportWithWarmup(player, requesterLocation, warmupSeconds, request.fromPlayerName, null);
            } else {
                // Requester teleports to target (accepter)
                var targetLocation = Players.getFullLocation(player);
                if (!targetLocation) {
                    ctx.sendMessage('&cCould not get your location.');
                    return;
                }
                
                ctx.sendMessage('&aTeleport request accepted. &f' + request.fromPlayerName + '&a is teleporting to you.');
                MessageHelper.send(requester, '&a' + Players.getUsername(player) + ' accepted your teleport request. Teleporting...');
                
                Utils.teleportWithWarmup(requester, targetLocation, warmupSeconds, Players.getUsername(player), null);
            }
        });
    
    // ========================================================================
    // /TPDENY COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('tpdeny')
        .setDescription('Deny a teleport request')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            var playerName = Players.getUsername(player);
            
            // Get pending request
            var request = getPendingRequest(playerId);
            if (!request) {
                ctx.sendMessage('&cYou have no pending teleport requests.');
                return;
            }
            
            // Remove the request
            delete pendingRequests[playerId];
            
            ctx.sendMessage('&cTeleport request from &f' + request.fromPlayerName + '&c denied.');
            
            // Notify the requester
            var requester = Players.getByUuid(request.fromPlayerId);
            if (requester) {
                MessageHelper.send(requester, '&c' + playerName + ' denied your teleport request.');
            }
        });
    
    // ========================================================================
    // /TPCANCEL COMMAND
    // ========================================================================
    
    Commands.register()
        .setName('tpcancel')
        .setDescription('Cancel your outgoing teleport request')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            var playerId = Players.getUuidString(player);
            
            var outgoing = getOutgoingRequest(playerId);
            if (!outgoing) {
                ctx.sendMessage('&cYou have no pending outgoing teleport requests.');
                return;
            }
            
            // Cancel the request
            delete pendingRequests[outgoing.targetPlayerId];
            
            ctx.sendMessage('&aYour teleport request has been cancelled.');
            
            // Notify the target
            var target = Players.getByUuid(outgoing.targetPlayerId);
            if (target) {
                MessageHelper.send(target, '&e' + Players.getUsername(player) + ' cancelled their teleport request.');
            }
        });
    
    // ========================================================================
    // CLEANUP ON PLAYER QUIT
    // ========================================================================
    
    Events.on('playerQuit', function(event) {
        var playerId = event.playerUuid;
        
        // Remove any pending request targeting this player
        if (pendingRequests[playerId]) {
            delete pendingRequests[playerId];
        }
        
        // Cancel any outgoing requests from this player
        cancelOutgoingRequest(playerId);
    });
    
    Logger.info('TPA commands loaded: /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel');
})();
