# SimpleScripting

SimpleScripting is a Hytale server plugin that enables server-side JavaScript scripting, allowing you to create custom commands, handle events, and build server features without writing Java code.

## Features

- **JavaScript Scripting** - Write server-side scripts using Mozilla Rhino JavaScript engine
- **Custom Commands** - Create and register custom commands with arguments, aliases, and permissions
- **Event System** - Listen to player events (join, quit, death, chat, movement, etc.)
- **Teleportation API** - Safe teleportation with async chunk preloading
- **Database API** - Simple key-value storage persisted to JSON files
- **Player Management** - Look up players, get positions, manage ranks
- **Scheduler** - Run delayed and repeating tasks
- **Permissions** - Check player permissions via Hytale's native permission system
- **Hot Reload** - Reload all scripts without restarting the server

## Quick Start

1. Build the plugin: `./gradlew build`
2. Place the JAR from `build/libs/` in your server's plugins folder
3. Start the server - the plugin creates `universe/SimpleScripting/mods/` automatically
4. Place your `.js` files in the `mods/` folder
5. Scripts in `lib/` subfolder are loaded first (for shared utilities)

## Directory Structure

```
universe/SimpleScripting/
├── mods/                    # Your JavaScript files
│   ├── lib/                 # Library files (loaded first)
│   │   ├── utils.js         # Common utilities
│   │   └── config.js        # Configuration system
│   ├── homes.js             # Home system
│   ├── warps.js             # Warp system
│   └── ...
├── db/                      # Database files (auto-created)
└── assets/                  # Assets folder
    ├── config/              # Config files
    ├── lang/                # Language files
    └── ui/                  # UI definitions (future)
```

## JavaScript API Reference

### Commands API

Register custom commands:

```javascript
Commands.register()
    .setName('mycommand')
    .setDescription('My custom command')
    .addRequiredStringArg('name', 'Player name')
    .addOptionalIntArg('count', 'Number of items')
    .addAlias('mc')
    .setHandler(function(ctx) {
        var name = ctx.getArgAsString('name');
        var count = ctx.getArgAsInt('count') || 1;
        ctx.sendMessage('Hello ' + name + '! Count: ' + count);
    });
```

**Argument Types:**
- `addRequiredStringArg(name, description)`
- `addOptionalStringArg(name, description)`
- `addRequiredIntArg(name, description)`
- `addOptionalIntArg(name, description)`
- `addRequiredDoubleArg(name, description)`
- `addOptionalDoubleArg(name, description)`

**Command Context (ctx):**
- `ctx.getArgAsString('name')` - Get string argument
- `ctx.getArgAsInt('name')` - Get integer argument
- `ctx.getArgAsDouble('name')` - Get double argument
- `ctx.sendMessage('text')` - Send message to command sender
- `ctx.isPlayer()` - Check if sender is a player
- `ctx.getPlayer()` - Get player entity (if sender is a player)

### Events API

Listen to player events:

```javascript
// Player joins the server
Events.on('playerJoin', function(event) {
    var player = Players.getByUuid(event.playerUuid);
    MessageHelper.broadcast(Players.getUsername(player) + ' joined!');
});

// Player quits
Events.on('playerQuit', function(event) {
    Logger.info('Player quit: ' + event.playerUuid);
});

// Player dies
Events.on('playerDeath', function(event) {
    var player = Players.getByUuid(event.playerUuid);
    // Save death location for /back command
});

// Player respawns
Events.on('playerRespawn', function(event) {
    // Welcome back message
});

// Player moves (0.1 block threshold)
Events.on('playerMove', function(event) {
    // event.x, event.y, event.z - new position
});

// Player changes world
Events.on('playerChangeWorld', function(event) {
    // event.fromWorld, event.toWorld
});

// Player chats (cancellable)
Events.on('playerChat', function(event) {
    // event.message
    // event.cancel() - prevent the message
});

// Player runs a command (cancellable)
Events.on('playerCommand', function(event) {
    // event.command - the command string
    // event.cancel() - prevent execution
});

// Server tick
Events.on('tick', function(event) {
    // Called every server tick
});

// Listen once
Events.once('playerJoin', function(event) {
    // Called only for the first join
});

// Remove listener
var handler = function(event) { /* ... */ };
Events.on('playerJoin', handler);
Events.off('playerJoin', handler);
```

### Players API

Find and manage players:

```javascript
// Find players
var player = Players.getByName('Steve');        // Exact match
var player = Players.getByNameFuzzy('stev');    // Fuzzy match
var player = Players.getByUuid('uuid-string');

// Get all online players
var players = Players.getOnlinePlayers();

// Get player info
var name = Players.getUsername(player);
var uuid = Players.getUuidString(player);

// Get player position
var pos = Players.getPosition(player);
// pos.x, pos.y, pos.z

var rot = Players.getRotation(player);
// rot.pitch, rot.yaw

var loc = Players.getFullLocation(player);
// loc.x, loc.y, loc.z, loc.pitch, loc.yaw, loc.worldName

// Get player's world
var world = Players.getWorld(player);
```

### Teleport API

Teleport players safely:

```javascript
// Teleport to coordinates
Teleport.teleport(player, world, x, y, z);

// Teleport with rotation
Teleport.teleportWithRotation(player, world, x, y, z, pitch, yaw);

// Teleport to another player
Teleport.teleportToPlayer(player, targetPlayer);

// Get player's current location
var loc = Teleport.getPlayerLocation(player);
// loc.x, loc.y, loc.z, loc.pitch, loc.yaw, loc.worldName
```

### Worlds API

Access world and block information:

```javascript
// Get a world by name
var world = Worlds.getWorld('default');

// Get world name
var name = Worlds.getWorldName(world);

// Get block info (async)
Worlds.getBlock(world, x, y, z, function(block) {
    // block.type - block type string
    // block.isAir - true if air
    // block.isSolid - true if solid
    // block.isLiquid - true if liquid
    // block.x, block.y, block.z - coordinates
});

// Quick checks (async)
Worlds.isAir(world, x, y, z, function(isAir) { /* ... */ });
Worlds.isSolid(world, x, y, z, function(isSolid) { /* ... */ });
Worlds.isLiquid(world, x, y, z, function(isLiquid) { /* ... */ });

// Find safe teleport Y (async)
Worlds.findSafeTeleportY(world, x, z, function(safeY) {
    if (safeY !== null) {
        // Found safe location at safeY
    }
});

// Get highest solid block Y (async)
Worlds.getHighestSolidY(world, x, z, function(y) {
    // y is the highest solid block
});
```

### DB API

Simple key-value database:

```javascript
// Save data
DB.save('myfile', 'key', 'value');

// Get data
var value = DB.get('myfile', 'key');

// Delete data
DB.delete('myfile', 'key');

// Check existence
var exists = DB.has('myfile', 'key');

// Example: Save JSON data
var homes = { home1: { x: 100, y: 64, z: 200 } };
DB.save('player_homes', playerId, JSON.stringify(homes));

// Load JSON data
var data = DB.get('player_homes', playerId);
var homes = data ? JSON.parse(data) : {};
```

### Scheduler API

Run delayed and repeating tasks:

```javascript
// Run after delay (in ticks, 20 ticks = 1 second)
var taskId = Scheduler.runLater(function() {
    Logger.info('This runs after 100 ticks');
}, 100);

// Run after delay (in milliseconds)
Scheduler.runLaterMs(function() {
    Logger.info('This runs after 5 seconds');
}, 5000);

// Run repeatedly (every N ticks)
var taskId = Scheduler.runRepeating(function() {
    Logger.info('This runs every 20 ticks');
}, 20);

// Run repeatedly (every N milliseconds)
Scheduler.runRepeatingMs(function() {
    Logger.info('This runs every second');
}, 1000);

// Cancel a task
Scheduler.cancel(taskId);

// Cancel all tasks
Scheduler.cancelAll();
```

### Permissions API

Check player permissions:

```javascript
// Check if player has permission
if (Permissions.has(player, 'simplescripting.admin')) {
    // Admin actions
}

// Check sender in command context
if (Permissions.hasSender(ctx, 'mymod.use')) {
    // Allowed
}

// Require permission (sends error message if denied)
if (!Permissions.require(player, 'mymod.admin', 'You need admin permission.')) {
    return;
}

// Check for admin
if (Permissions.isAdmin(player)) {
    // Is an admin
}

// Require player (not console)
var player = Permissions.requirePlayer(ctx);
if (!player) return;
```

### MessageHelper API

Send messages to players:

```javascript
// Send to a player
MessageHelper.send(player, 'Hello!');

// Broadcast to all players
MessageHelper.broadcast('Server announcement!');
```

### Logger API

Log messages to server console:

```javascript
Logger.info('Information message');
Logger.warning('Warning message');
Logger.error('Error message');
Logger.debug('Debug message');  // Only shown if debug enabled
```

### Colors API

Minecraft color codes:

```javascript
ctx.sendMessage(Colors.GREEN + 'Success!' + Colors.RESET);
ctx.sendMessage(Colors.RED + 'Error!' + Colors.RESET);
ctx.sendMessage(Colors.BOLD + Colors.GOLD + 'Important!' + Colors.RESET);
```

**Available Colors:**
- `Colors.BLACK`, `Colors.DARK_BLUE`, `Colors.DARK_GREEN`, `Colors.DARK_AQUA`
- `Colors.DARK_RED`, `Colors.DARK_PURPLE`, `Colors.GOLD`, `Colors.GRAY`
- `Colors.DARK_GRAY`, `Colors.BLUE`, `Colors.GREEN`, `Colors.AQUA`
- `Colors.RED`, `Colors.LIGHT_PURPLE`, `Colors.YELLOW`, `Colors.WHITE`
- `Colors.BOLD`, `Colors.ITALIC`, `Colors.UNDERLINE`, `Colors.STRIKETHROUGH`
- `Colors.RESET`

## Included Sample Scripts

The plugin includes ready-to-use feature scripts:

### homes.js - Home System
- `/sethome [name]` - Set a home (default: "home")
- `/home [name]` - Teleport to a home
- `/delhome <name>` - Delete a home
- `/homes` - List all homes

### warps.js - Warp System
- `/setwarp <name>` - Set a server warp (admin)
- `/warp <name>` - Teleport to a warp
- `/delwarp <name>` - Delete a warp (admin)
- `/warps` - List all warps

### spawn.js - Spawn System
- `/setspawn` - Set server spawn (admin)
- `/spawn` - Teleport to spawn

### tpa.js - Teleport Requests
- `/tpa <player>` - Request to teleport to player
- `/tpahere <player>` - Request player to teleport to you
- `/tpaccept` - Accept a teleport request
- `/tpdeny` - Deny a teleport request
- `/tpcancel` - Cancel your outgoing request

### back.js - Return to Previous Location
- `/back` - Return to your previous location (especially after death)

### rtp.js - Random Teleport
- `/rtp` - Teleport to a random location
- `/randomtp`, `/wild` - Aliases

### ranks.js - Rank Management
- `/rank set <player> <rank>` - Set player rank (admin)
- `/rank get <player>` - Get player rank (admin)
- `/rank list` - List all ranks (admin)
- `/rank remove <player>` - Remove player rank (admin)
- `/myrank` - Check your own rank

### admin.js - Admin Utilities
- `/simplescripting reload` - Reload all scripts
- `/simplescripting info` - Show plugin info
- `/tp <player>` - Teleport to player (admin)
- `/tphere <player>` - Teleport player to you (admin)
- `/playerinfo <player>` - Show player info (admin)
- `/players` - List online players

## Configuration

The `lib/config.js` file contains default settings that can be modified:

```javascript
// Default settings
var DEFAULTS = {
    homes: {
        maxHomes: 3,
        cooldownSeconds: 5,
        warmupSeconds: 3
    },
    warps: {
        cooldownSeconds: 5,
        warmupSeconds: 3
    },
    spawn: {
        cooldownSeconds: 10,
        warmupSeconds: 5
    },
    tpa: {
        timeoutSeconds: 60,
        cooldownSeconds: 10,
        warmupSeconds: 3
    },
    back: {
        cooldownSeconds: 10,
        warmupSeconds: 3,
        historySize: 5
    },
    rtp: {
        minDistance: 100,
        maxDistance: 5000,
        maxAttempts: 10,
        cooldownSeconds: 300,
        warmupSeconds: 5
    }
};
```

### Rank-Based Settings

Define ranks with custom limits in `lib/config.js`:

```javascript
var RANKS = {
    default: {
        // Uses DEFAULTS
    },
    vip: {
        homes: { maxHomes: 5, cooldownSeconds: 3 }
    },
    admin: {
        homes: { maxHomes: 10, cooldownSeconds: 0, warmupSeconds: 0 }
    }
};
```

## Writing Custom Scripts

### Basic Template

```javascript
/**
 * My Custom Script
 * Description of what it does.
 */

(function() {
    'use strict';
    
    // Register a command
    Commands.register()
        .setName('mycommand')
        .setDescription('Does something cool')
        .setHandler(function(ctx) {
            var player = Utils.getPlayerFromContext(ctx);
            if (!player) return;
            
            ctx.sendMessage(Colors.GREEN + 'Hello, ' + Players.getUsername(player) + '!');
        });
    
    // Listen to events
    Events.on('playerJoin', function(event) {
        var player = Players.getByUuid(event.playerUuid);
        if (player) {
            MessageHelper.send(player, Colors.GOLD + 'Welcome to the server!');
        }
    });
    
    Logger.info('My custom script loaded!');
})();
```

### Using Utils Library

```javascript
// Get player from command context
var player = Utils.getPlayerFromContext(ctx);

// Format time
var text = Utils.formatTime(3661); // "1h 1m 1s"

// Format position
var pos = Utils.formatPosition(100.5, 64, -200.3); // "100.5, 64, -200.3"

// Create cooldown tracker
var cooldowns = Utils.createCooldownTracker('db_file', 'feature_name');
cooldowns.setCooldown(playerId, 'action');
if (cooldowns.isOnCooldown(playerId, 'action', 60)) {
    var remaining = cooldowns.getRemainingSeconds(playerId, 'action', 60);
}

// Teleport with warmup
Utils.teleportWithWarmup(player, location, warmupSeconds, 'destination name', callback);
```

## Building the Plugin

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`

## License

See LICENSE file for details.
