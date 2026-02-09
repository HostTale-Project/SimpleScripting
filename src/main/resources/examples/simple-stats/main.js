// Simple Stats - Track player join counts and display leaderboards

var statsApi = require('./stats-api.js');

function onEnable() {
    statsApi.init();
    
    events.on('playerready', function(event) {
        statsApi.incrementStat(event.player.getUsername(), 'joins');
    });
    
    commands.register('stats', function(context) {
        var player = context.sender();
        var args = context.args();
        var targetName = args.length > 0 ? args[0] : player.getUsername();
        
        var stats = statsApi.getStats(targetName);
        
        if (!stats) {
            context.reply(ui.color('No stats found for ' + targetName, '#FF0000'));
            return;
        }
        
        context.reply(ui.color('=== Stats for ' + targetName + ' ===', '#FFD700'));
        context.reply(ui.join(ui.color('Joins: ', '#00FFFF'), stats.joins + ''));
    });
    
    commands.register('top', function(context) {
        var args = context.args();
        var stat = args.length > 0 ? args[0] : 'joins';
        
        if (stat !== 'joins') {
            context.reply(ui.color('Valid stats: joins', '#FF0000'));
            return;
        }
        
        var top = statsApi.getTopPlayers(stat, 10);
        
        context.reply(ui.color('=== Top 10 by ' + stat + ' ===', '#FFD700'));
        
        for (var i = 0; i < top.length; i++) {
            var rank = i + 1;
            var entry = top[i];
            var value = entry[stat];
            
            context.reply(ui.join(
                ui.color('#' + rank + ' ', '#FFFF00'),
                entry.player_name + ' - ' + value
            ));
        }
    });
    
    // Expose API via shared services
    SharedServices.expose('simple-stats', {
        getStats: statsApi.getStats,
        incrementStat: statsApi.incrementStat,
        getTopPlayers: statsApi.getTopPlayers
    });
    
    console.info('Simple Stats enabled - /stats, /top');
}

function onDisable() {
    console.info('Simple Stats disabled');
}
