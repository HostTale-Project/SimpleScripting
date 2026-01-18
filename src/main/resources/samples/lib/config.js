/**
 * SimpleScripting Configuration Library
 * Centralized configuration management for all features.
 * 
 * This file is loaded after utils.js and provides configuration
 * with database persistence and sensible defaults.
 */

// ============================================================================
// GLOBAL CONFIG NAMESPACE
// ============================================================================

var Config = (function() {
    'use strict';
    
    var CONFIG_FILE = 'config';
    
    // ========================================================================
    // DEFAULT CONFIGURATION
    // ========================================================================
    
    var defaults = {
        // Home settings
        homes: {
            maxHomes: 3,           // Default max homes per player
            cooldownSeconds: 5,    // Cooldown between home teleports
            warmupSeconds: 3       // Warmup before home teleport
        },
        
        // Warp settings
        warps: {
            cooldownSeconds: 5,
            warmupSeconds: 3
        },
        
        // Spawn settings
        spawn: {
            cooldownSeconds: 10,
            warmupSeconds: 3
        },
        
        // TPA settings
        tpa: {
            cooldownSeconds: 30,
            warmupSeconds: 3,
            timeoutSeconds: 60     // How long requests stay active
        },
        
        // Back command settings
        back: {
            cooldownSeconds: 10,
            warmupSeconds: 3,
            historySize: 5         // Number of locations to remember
        },
        
        // RTP (Random Teleport) settings
        rtp: {
            cooldownSeconds: 60,
            warmupSeconds: 5,
            minDistance: 100,      // Minimum distance from spawn
            maxDistance: 5000,     // Maximum distance from spawn
            maxAttempts: 10        // Max attempts to find safe location
        },
        
        // Rank settings
        ranks: {
            defaultRankId: 'default'
        }
    };
    
    // ========================================================================
    // CONFIGURATION LOADING/SAVING
    // ========================================================================
    
    /**
     * Gets a configuration value, falling back to default if not set.
     * @param {string} path - Dot-separated path (e.g., "homes.maxHomes")
     * @returns {*} Configuration value
     */
    function get(path) {
        // Try to get from database
        var stored = DB.get(CONFIG_FILE, path);
        if (stored !== null) {
            try {
                return JSON.parse(stored);
            } catch (e) {
                // Not JSON, return as-is
                return stored;
            }
        }
        
        // Fall back to default
        return getDefault(path);
    }
    
    /**
     * Gets the default value for a configuration path.
     * @param {string} path - Dot-separated path
     * @returns {*} Default value
     */
    function getDefault(path) {
        var parts = path.split('.');
        var current = defaults;
        
        for (var i = 0; i < parts.length; i++) {
            if (current === undefined || current === null) {
                return undefined;
            }
            current = current[parts[i]];
        }
        
        return current;
    }
    
    /**
     * Sets a configuration value.
     * @param {string} path - Dot-separated path
     * @param {*} value - Value to set
     */
    function set(path, value) {
        var toStore = (typeof value === 'object') ? JSON.stringify(value) : String(value);
        DB.save(CONFIG_FILE, path, toStore);
    }
    
    /**
     * Resets a configuration value to its default.
     * @param {string} path - Dot-separated path
     */
    function reset(path) {
        DB.delete(CONFIG_FILE, path);
    }
    
    // ========================================================================
    // RANK-BASED CONFIGURATION
    // ========================================================================
    
    // Rank configuration storage
    var RANKS_FILE = 'ranks';
    
    /**
     * Gets a player's rank ID.
     * @param {string} playerId - Player UUID
     * @returns {string} Rank ID
     */
    function getPlayerRank(playerId) {
        var rank = DB.get(RANKS_FILE, 'player:' + playerId);
        return rank || get('ranks.defaultRankId');
    }
    
    /**
     * Sets a player's rank.
     * @param {string} playerId - Player UUID
     * @param {string} rankId - Rank ID
     */
    function setPlayerRank(playerId, rankId) {
        DB.save(RANKS_FILE, 'player:' + playerId, rankId);
    }
    
    /**
     * Gets a rank's configuration for a specific setting.
     * Falls back to the default rank, then to global defaults.
     * @param {string} rankId - Rank ID
     * @param {string} setting - Setting path (e.g., "homes.maxHomes")
     * @returns {*} Setting value
     */
    function getRankSetting(rankId, setting) {
        // Try rank-specific setting
        var rankSetting = DB.get(RANKS_FILE, 'rank:' + rankId + ':' + setting);
        if (rankSetting !== null) {
            try {
                return JSON.parse(rankSetting);
            } catch (e) {
                return rankSetting;
            }
        }
        
        // Try default rank setting
        var defaultRankId = get('ranks.defaultRankId');
        if (rankId !== defaultRankId) {
            var defaultSetting = DB.get(RANKS_FILE, 'rank:' + defaultRankId + ':' + setting);
            if (defaultSetting !== null) {
                try {
                    return JSON.parse(defaultSetting);
                } catch (e) {
                    return defaultSetting;
                }
            }
        }
        
        // Fall back to global default
        return get(setting);
    }
    
    /**
     * Sets a rank's configuration for a specific setting.
     * @param {string} rankId - Rank ID
     * @param {string} setting - Setting path
     * @param {*} value - Value to set
     */
    function setRankSetting(rankId, setting, value) {
        var toStore = (typeof value === 'object') ? JSON.stringify(value) : String(value);
        DB.save(RANKS_FILE, 'rank:' + rankId + ':' + setting, toStore);
    }
    
    /**
     * Gets a player's effective setting (considering their rank).
     * @param {string} playerId - Player UUID
     * @param {string} setting - Setting path
     * @returns {*} Setting value
     */
    function getPlayerSetting(playerId, setting) {
        var rankId = getPlayerRank(playerId);
        return getRankSetting(rankId, setting);
    }
    
    /**
     * Gets all defined ranks.
     * @returns {Array} Array of rank IDs
     */
    function getAllRanks() {
        var ranksData = DB.getAll(RANKS_FILE);
        if (!ranksData) {
            return [get('ranks.defaultRankId')];
        }
        
        try {
            var data = JSON.parse(ranksData);
            var ranks = [];
            var seen = {};
            
            for (var key in data) {
                if (key.indexOf('rank:') === 0) {
                    var parts = key.split(':');
                    if (parts.length >= 2 && !seen[parts[1]]) {
                        ranks.push(parts[1]);
                        seen[parts[1]] = true;
                    }
                }
            }
            
            // Ensure default rank is included
            var defaultRank = get('ranks.defaultRankId');
            if (!seen[defaultRank]) {
                ranks.unshift(defaultRank);
            }
            
            return ranks;
        } catch (e) {
            return [get('ranks.defaultRankId')];
        }
    }
    
    // ========================================================================
    // PUBLIC API
    // ========================================================================
    
    return {
        // Global config
        get: get,
        getDefault: getDefault,
        set: set,
        reset: reset,
        
        // Rank-based config
        getPlayerRank: getPlayerRank,
        setPlayerRank: setPlayerRank,
        getRankSetting: getRankSetting,
        setRankSetting: setRankSetting,
        getPlayerSetting: getPlayerSetting,
        getAllRanks: getAllRanks,
        
        // Direct access to defaults for reference
        defaults: defaults
    };
})();

// Log that config is loaded
Logger.info('Config library loaded');
