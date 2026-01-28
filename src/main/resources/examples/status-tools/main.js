const fmt = require("./util/format");

var heartbeatTask = null;

function onEnable() {
  log.info("status-tools enabled (commands: /who, /worldinfo)");

  // Simple chat responder
  events.on("PlayerChat", function(evt) {
    var msg = evt.getMessage();
    if (!msg) return;
    if (msg.trim().toLowerCase() === "!who") {
      sendWho(evt.getPlayer());
      evt.cancel();
    }
  });

  // /who: list online players
  commands.register("who", function(ctx) {
    sendWho(ctx.sender());
  });

  // /worldinfo: show worlds with player counts
  commands.register("worldinfo", function(ctx) {
    var stats = worldStats();
    if (stats.length === 0) {
      ctx.reply("No worlds are loaded.");
      return;
    }
    stats.forEach(function(entry) {
      ctx.reply(
        ui.join(
          ui.color(fmt.pad(entry.name, 12), "#00ffc8"),
          ui.color(" players: ", "#cccccc"),
          ui.color(String(entry.count), "#ffd166"),
          ui.color(" [", "#666666"),
          ui.color(entry.players.join(", "), "#ffffff"),
          ui.color("]", "#666666")
        )
      );
    });
  });

  // Shared service exposing status info
  SharedServices.expose("status-tools", {
    online: function() {
      return players.names();
    },
    worlds: function() {
      return worldStats();
    }
  });

  // Heartbeat broadcast every 60s
  heartbeatTask = server.runRepeating(0, 60000, function() {
    var stats = worldStats();
    var summary = stats.map(function(w) { return w.name + "=" + w.count; }).join(" | ");
    net.broadcast(
      ui.join(
        ui.color("[Status] ", "#00ffc8"),
        ui.color("Online: ", "#cccccc"),
        ui.color(String(players.count()), "#ffd166"),
        ui.color(" | ", "#666666"),
        ui.color(summary || "no worlds", "#ffffff")
      )
    );
  });
}

function onDisable() {
  if (heartbeatTask && !heartbeatTask.cancelled()) {
    heartbeatTask.cancel();
  }
  heartbeatTask = null;
  log.info("status-tools disabled");
}

function sendWho(target) {
  var names = players.names();
  var line = names.length === 0 ? ui.color("No players online.", "#ffb347") :
    ui.join(
      ui.color("Online (" + names.length + "): ", "#00ffc8"),
      ui.color(names.join(", "), "#ffffff")
    );
  var prefix = ui.color("[Who] ", "#6666ff");
  var message = ui.join(prefix, line);
  if (target) target.sendMessage(message);
  else net.broadcast(message);
}

function worldStats() {
  var list = worlds.list();
  return list.map(function(name) {
    var world = worlds.get(name);
    var ppl = world ? world.playerNames() : [];
    return { name: name, count: ppl.length, players: ppl };
  });
}
