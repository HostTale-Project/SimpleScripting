// Welcome Rewards - Simple welcome system with starter kit and playtime tracking

var STARTER_KIT = [
    { itemId: 'Wood_Beech_Trunk', quantity: 64 },
    { itemId: 'Rock_Stone_Cobble_Half', quantity: 32 },
    { itemId: 'Tool_Pickaxe_Wood', quantity: 1 },
    { itemId: 'Food_Bread', quantity: 16 }
];

var playtimeTrackers = {}; // playerName -> { joinTime, totalSeconds }

function onEnable() {
    db.execute('CREATE TABLE IF NOT EXISTS player_data (name TEXT PRIMARY KEY, first_join INTEGER, total_playtime INTEGER DEFAULT 0)');
    
    var rows = db.query('SELECT name, total_playtime FROM player_data');
    for (var i = 0; i < rows.length; i++) {
        playtimeTrackers[rows[i].name] = {
            joinTime: null,
            totalSeconds: rows[i].total_playtime
        };
    }
    
    events.on('playerready', function(event) {
        var player = event.player;
        var name = player.getUsername();
        
        players.broadcast(ui.join(ui.color('+ ', '#00FF00'), name + ' joined the server'));
        
        var result = db.query('SELECT first_join FROM player_data WHERE name = ?', [name]);
        
        if (result.length === 0) {
            db.execute('INSERT INTO player_data (name, first_join, total_playtime) VALUES (?, ?, ?)', 
                [name, Date.now(), 0]);
            
            player.sendMessage(ui.color('=== Welcome to the server! ===', '#FFD700'));
            player.sendMessage("Here's a starter kit to help you begin:");
            
            giveStarterKit(player);
            playtimeTrackers[name] = { joinTime: Date.now(), totalSeconds: 0 };
        } else {
            player.sendMessage(ui.color('Welcome back, ' + name + '!', '#FFFF00'));
            
            if (!playtimeTrackers[name]) {
                var pt = db.query('SELECT total_playtime FROM player_data WHERE name = ?', [name]);
                playtimeTrackers[name] = {
                    joinTime: Date.now(),
                    totalSeconds: pt.length > 0 ? pt[0].total_playtime : 0
                };
            } else {
                playtimeTrackers[name].joinTime = Date.now();
            }
        }
    });
    
    events.on('playerdisconnect', function(event) {
        var player = event.player;
        var name = player.getUsername();
        
        if (playtimeTrackers[name] && playtimeTrackers[name].joinTime) {
            var sessionTime = Math.floor((Date.now() - playtimeTrackers[name].joinTime) / 1000);
            playtimeTrackers[name].totalSeconds += sessionTime;
            db.execute('UPDATE player_data SET total_playtime = ? WHERE name = ?',
                [playtimeTrackers[name].totalSeconds, name]);
            playtimeTrackers[name].joinTime = null;
        }
        
        players.broadcast(ui.join(ui.color('- ', '#FF0000'), name + ' left the server'));
    });
    
    commands.register('playtime', function(context) {
        var player = context.sender();
        var args = context.args();
        var targetName = args.length > 0 ? args[0] : player.getUsername();
        
        var result = db.query('SELECT total_playtime FROM player_data WHERE name = ?', [targetName]);
        
        if (result.length === 0) {
            context.reply(ui.color('Player ' + targetName + ' not found!', '#FF0000'));
            return;
        }
        
        var totalSeconds = result[0].total_playtime;
        
        if (playtimeTrackers[targetName] && playtimeTrackers[targetName].joinTime) {
            var currentSession = Math.floor((Date.now() - playtimeTrackers[targetName].joinTime) / 1000);
            totalSeconds += currentSession;
        }
        
        var hours = Math.floor(totalSeconds / 3600);
        var minutes = Math.floor((totalSeconds % 3600) / 60);
        
        context.reply(ui.join(ui.color('Playtime for ' + targetName + ': ', '#00FFFF'), hours + 'h ' + minutes + 'm'));
    });
    
    commands.register('online', function(context) {
        var onlinePlayers = players.all();
        context.reply(ui.color('=== Online Players (' + onlinePlayers.length + ') ===', '#FFD700'));
        for (var i = 0; i < onlinePlayers.length; i++) {
            context.reply('  ' + onlinePlayers[i].getUsername());
        }
    });
    
    server.runRepeating(60000, 60000, function() {
        var now = Date.now();
        for (var name in playtimeTrackers) {
            if (playtimeTrackers[name].joinTime) {
                var sessionTime = Math.floor((now - playtimeTrackers[name].joinTime) / 1000);
                var total = playtimeTrackers[name].totalSeconds + sessionTime;
                db.execute('UPDATE player_data SET total_playtime = ? WHERE name = ?', [total, name]);
            }
        }
    });
    
    console.info('Welcome Rewards enabled - /playtime, /online');
}

function giveStarterKit(player) {
    var inv = player.getInventory();
    if (!inv) {
        player.sendMessage('Error: Could not access inventory!');
        return;
    }
    
    for (var i = 0; i < STARTER_KIT.length; i++) {
        var item = STARTER_KIT[i];
        var stack = inventory.createStack(item.itemId, item.quantity);
        var result = inv.addItem(stack);
        if (result.success) {
            player.sendMessage(ui.color('+ ' + item.quantity + 'x ' + item.itemId, '#00FF00'));
        }
    }
}

function onDisable() {
    var now = Date.now();
    for (var name in playtimeTrackers) {
        if (playtimeTrackers[name].joinTime) {
            var sessionTime = Math.floor((now - playtimeTrackers[name].joinTime) / 1000);
            var total = playtimeTrackers[name].totalSeconds + sessionTime;
            db.execute('UPDATE player_data SET total_playtime = ? WHERE name = ?', [total, name]);
        }
    }
    console.info('Welcome Rewards disabled');
}
