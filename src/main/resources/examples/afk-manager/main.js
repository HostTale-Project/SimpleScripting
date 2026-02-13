// AFK Manager - Detect and manage idle players

var AFK_TIMEOUT = 300000; // 5 minutes
var KICK_TIMEOUT = 600000; // 10 minutes
var CHECK_INTERVAL = 30000; // 30 seconds

var playerActivity = {}; // name -> { lastActivity, isAfk, position }

function onEnable() {
    events.on('playerready', function(event) {
        var player = event.player;
        worlds.runOnWorldThread(null, function() {
            var name = player.getUsername();
            var pos = ecs.getPosition(player);
            playerActivity[name] = {
                lastActivity: Date.now(),
                isAfk: false,
                position: { x: pos.x, y: pos.y, z: pos.z }
            };
        });
    });
    
    events.on('playerdisconnect', function(event) {
        delete playerActivity[event.player.getUsername()];
    });
    
    events.on('livingentityinventorychange', function(event) {
        // Track inventory changes as player activity
        // Note: getLivingEntity() returns the raw Player object for players
        var entity = event.getLivingEntity();
        if (entity && entity.getPlayerRef) {
            // This is a Player - get their display name
            var name = entity.getDisplayName();
            if (name && playerActivity[name]) {
                updateActivity(name);
            }
        }
    });
    
    server.runRepeating(CHECK_INTERVAL, CHECK_INTERVAL, function() {
        // Use world thread to safely access player positions
        worlds.runOnWorldThread(null, function() {
            checkAfkPlayers();
        });
    });
    
    commands.register('afk', function(context) {
        var player = context.sender();
        var name = player.getUsername();
        
        worlds.runOnWorldThread(null, function() {
            if (playerActivity[name]) {
                var data = playerActivity[name];
                data.isAfk = !data.isAfk;
                data.lastActivity = Date.now();
                var pos = ecs.getPosition(player);
                data.position = { x: pos.x, y: pos.y, z: pos.z };
                
                var status = data.isAfk ? 'now AFK' : 'no longer AFK';
                players.broadcast(ui.color(name + ' is ' + status, '#808080'));
            }
        });
    });
    
    commands.register('afklist', function(context) {
        var afkPlayers = [];
        
        for (var name in playerActivity) {
            if (playerActivity[name].isAfk) {
                afkPlayers.push(name);
            }
        }
        
        if (afkPlayers.length === 0) {
            context.reply(ui.color('No AFK players', '#00FF00'));
        } else {
            context.reply(ui.color('=== AFK Players (' + afkPlayers.length + ') ===', '#FFFF00'));
            for (var i = 0; i < afkPlayers.length; i++) {
                var inactiveTime = Date.now() - playerActivity[afkPlayers[i]].lastActivity;
                var minutes = Math.floor(inactiveTime / 60000);
                context.reply('  ' + afkPlayers[i] + ' (' + minutes + 'm)');
            }
        }
    });
    
    console.info('AFK Manager enabled - /afk, /afklist');
}

function updateActivity(playerName) {
    if (playerActivity[playerName]) {
        var data = playerActivity[playerName];
        if (data.isAfk) {
            data.isAfk = false;
            players.broadcast(ui.color(playerName + ' is no longer AFK', '#808080'));
        }
        data.lastActivity = Date.now();
    }
}

function checkAfkPlayers() {
    var now = Date.now();
    var onlinePlayers = players.all();
    
    for (var i = 0; i < onlinePlayers.length; i++) {
        var player = onlinePlayers[i];
        var name = player.getUsername();
        
        if (!playerActivity[name]) {
            var pos = ecs.getPosition(player);
            playerActivity[name] = {
                lastActivity: now,
                isAfk: false,
                position: { x: pos.x, y: pos.y, z: pos.z }
            };
            continue;
        }
        
        var data = playerActivity[name];
        var inactiveTime = now - data.lastActivity;
        
        // Check movement
        var currentPos = ecs.getPosition(player);
        if (data.position && currentPos) {
            var moved = Math.abs(currentPos.x - data.position.x) > 0.1 ||
                       Math.abs(currentPos.y - data.position.y) > 0.1 ||
                       Math.abs(currentPos.z - data.position.z) > 0.1;
            
            if (moved) {
                updateActivity(name);
                if (data.isAfk) {
                    data.isAfk = false;
                    players.broadcast(ui.color(name + ' is no longer AFK', '#00FF00'));
                }
                data.position = { x: currentPos.x, y: currentPos.y, z: currentPos.z };
                continue;
            }
        }
        
        // Mark AFK
        if (!data.isAfk && inactiveTime >= AFK_TIMEOUT) {
            data.isAfk = true;
            players.broadcast(ui.color(name + ' is now AFK', '#808080'));
        }
        
        // Kick if too long
        if (data.isAfk && inactiveTime >= KICK_TIMEOUT) {
            player.kick('Kicked for being AFK too long');
            players.broadcast(ui.color(name + ' was kicked for being AFK', '#FF0000'));
            delete playerActivity[name];
        }
    }
}

function onDisable() {
    console.info('AFK Manager disabled');
}
