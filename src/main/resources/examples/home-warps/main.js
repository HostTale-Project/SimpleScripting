// Home Warps - Personal homes and server warps teleportation

var MAX_HOMES = 3;

function onEnable() {
    db.execute('CREATE TABLE IF NOT EXISTS homes (player TEXT, name TEXT, world TEXT, x REAL, y REAL, z REAL, PRIMARY KEY (player, name))');
    db.execute('CREATE TABLE IF NOT EXISTS warps (name TEXT PRIMARY KEY, world TEXT, x REAL, y REAL, z REAL)');
    
    commands.register('sethome', function(context) {
        var player = context.sender();
        var args = context.args();
        var homeName = args.length > 0 ? args[0] : 'home';
        var name = player.getUsername();
        
        var count = db.query('SELECT COUNT(*) as cnt FROM homes WHERE player = ?', [name])[0].cnt;
        if (count >= MAX_HOMES) {
            var existing = db.queryOne('SELECT name FROM homes WHERE player = ? AND name = ?', [name, homeName]);
            if (!existing) {
                context.reply(ui.color('You have ' + MAX_HOMES + ' homes! Delete one with /delhome', '#FF0000'));
                return;
            }
        }
        
        var pos = ecs.getPosition(player);
        var world = player.getWorldName();
        
        db.execute('INSERT OR REPLACE INTO homes VALUES (?, ?, ?, ?, ?, ?)', 
            [name, homeName, world, pos.x, pos.y, pos.z]);
        context.reply(ui.color('Home "' + homeName + '" set!', '#00FF00'));
    });
    
    commands.register('home', function(context) {
        var player = context.sender();
        var args = context.args();
        var name = player.getUsername();
        
        if (args.length === 0) {
            var homes = db.query('SELECT name FROM homes WHERE player = ?', [name]);
            if (homes.length === 0) {
                context.reply(ui.color('No homes set. Use /sethome', '#FFFF00'));
                return;
            }
            context.reply(ui.color('=== Your Homes ===', '#FFD700'));
            for (var i = 0; i < homes.length; i++) {
                context.reply('  ' + homes[i].name);
            }
            return;
        }
        
        var homeName = args[0];
        var home = db.queryOne('SELECT * FROM homes WHERE player = ? AND name = ?', [name, homeName]);
        
        if (!home) {
            context.reply(ui.color('Home "' + homeName + '" not found!', '#FF0000'));
            return;
        }
        
        ecs.teleport(player, [home.x, home.y, home.z], [0, 0, 0]);
        context.reply(ui.color('Teleported to ' + homeName, '#00FF00'));
    });
    
    commands.register('delhome', function(context) {
        var player = context.sender();
        var args = context.args();
        
        if (args.length === 0) {
            context.reply(ui.color('Usage: /delhome <name>', '#FF0000'));
            return;
        }
        
        var homeName = args[0];
        var name = player.getUsername();
        var result = db.execute('DELETE FROM homes WHERE player = ? AND name = ?', [name, homeName]);
        
        if (result.changes > 0) {
            context.reply(ui.color('Home "' + homeName + '" deleted!', '#00FF00'));
        } else {
            context.reply(ui.color('Home "' + homeName + '" not found!', '#FF0000'));
        }
    });
    
    commands.register('setwarp', function(context) {
        var player = context.sender();
        var args = context.args();
        
        // Simple admin check - in real usage, use proper permissions
        if (player.getUsername() !== 'Admin') {
            context.reply(ui.color('No permission!', '#FF0000'));
            return;
        }
        
        if (args.length === 0) {
            context.reply(ui.color('Usage: /setwarp <name>', '#FF0000'));
            return;
        }
        
        var warpName = args[0];
        var pos = ecs.getPosition(player);
        var world = player.getWorldName();
        
        db.execute('INSERT OR REPLACE INTO warps VALUES (?, ?, ?, ?, ?)', 
            [warpName, world, pos.x, pos.y, pos.z]);
        context.reply(ui.color('Warp "' + warpName + '" created!', '#00FF00'));
    });
    
    commands.register('warp', function(context) {
        var player = context.sender();
        var args = context.args();
        
        if (args.length === 0) {
            var warps = db.query('SELECT name FROM warps');
            if (warps.length === 0) {
                context.reply(ui.color('No warps available!', '#FFFF00'));
                return;
            }
            context.reply(ui.color('=== Server Warps ===', '#FFD700'));
            for (var i = 0; i < warps.length; i++) {
                context.reply('  ' + warps[i].name);
            }
            return;
        }
        
        var warpName = args[0];
        var warp = db.queryOne('SELECT * FROM warps WHERE name = ?', [warpName]);
        
        if (!warp) {
            context.reply(ui.color('Warp "' + warpName + '" not found!', '#FF0000'));
            return;
        }
        
        ecs.teleport(player, [warp.x, warp.y, warp.z], [0, 0, 0]);
        context.reply(ui.color('Teleported to ' + warpName, '#00FF00'));
    });
    
    commands.register('delwarp', function(context) {
        var player = context.sender();
        var args = context.args();
        
        if (player.getUsername() !== 'Admin') {
            context.reply(ui.color('No permission!', '#FF0000'));
            return;
        }
        
        if (args.length === 0) {
            context.reply(ui.color('Usage: /delwarp <name>', '#FF0000'));
            return;
        }
        
        var warpName = args[0];
        var result = db.execute('DELETE FROM warps WHERE name = ?', [warpName]);
        
        if (result.changes > 0) {
            context.reply(ui.color('Warp "' + warpName + '" deleted!', '#00FF00'));
        } else {
            context.reply(ui.color('Warp "' + warpName + '" not found!', '#FF0000'));
        }
    });
    
    console.info('Home Warps enabled - /home, /warp');
}

function onDisable() {
    console.info('Home Warps disabled');
}
