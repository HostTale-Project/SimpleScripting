// Stats API Module - Demonstrates code organization with require()

function init() {
    db.execute('CREATE TABLE IF NOT EXISTS player_stats (' +
        'player_name TEXT PRIMARY KEY, ' +
        'joins INTEGER DEFAULT 0)');
}

function getStats(playerName) {
    var result = db.query('SELECT * FROM player_stats WHERE player_name = ?', [playerName]);
    
    if (result.length === 0) {
        return null;
    }
    
    return result[0];
}

function incrementStat(playerName, statName) {
    // Ensure player exists
    db.execute('INSERT OR IGNORE INTO player_stats (player_name) VALUES (?)', [playerName]);
    
    // Increment stat
    var sql = 'UPDATE player_stats SET ' + statName + ' = ' + statName + ' + 1 WHERE player_name = ?';
    db.execute(sql, [playerName]);
}

function getTopPlayers(statName, limit) {
    var sql = 'SELECT * FROM player_stats ORDER BY ' + statName + ' DESC LIMIT ?';
    return db.query(sql, [limit]);
}

// Export functions (use exports directly, not module.exports)
exports.init = init;
exports.getStats = getStats;
exports.incrementStat = incrementStat;
exports.getTopPlayers = getTopPlayers;
