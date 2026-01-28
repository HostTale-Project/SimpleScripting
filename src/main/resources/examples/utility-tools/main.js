var blockedWords = ["spam", "ads", "scam"];
var pendingTeleports = {}; // targetUsername -> requesterUsername

function onEnable() {
  log.info("utility-tools enabled (commands: /msg, /ut-announce, /tpa)");

  // /msg <player> <message...>
  commands.register("msg", function(ctx) {
    var args = ctx.args();
    if (args.length < 2) {
      ctx.reply("Usage: /msg <player> <message>");
      return;
    }
    var target = players.find(args[0]);
    if (!target || !target.isOnline()) {
      ctx.reply("Player not found: " + args[0]);
      return;
    }
    var text = args.slice(1).join(" ");
    var sender = ctx.sender();
    var fromName = sender ? sender.getUsername() : "Server";
    target.sendMessage("[PM] " + fromName + ": " + text);
    if (sender) {
      sender.sendMessage("[PM -> " + target.getUsername() + "] " + text);
    } else {
      log.info("Sent PM to " + target.getUsername() + ": " + text);
    }
  }, { description: "Send a private message", allowExtraArgs: true });

  // /ut-announce <message...>
  commands.register("ut-announce", function(ctx) {
    var args = ctx.args();
    if (args.length === 0) {
      ctx.reply("Usage: /ut-announce <message>");
      return;
    }
    var text = args.join(" ");
    net.broadcast("[Announcement] " + text);
  }, { description: "Broadcast a server message", allowExtraArgs: true });

  // /tpa <player>
  commands.register("tpa", function(ctx) {
    var args = ctx.args();
    if (args.length < 1) {
      ctx.reply("Usage: /tpa <player>");
      return;
    }
    var requester = ctx.sender();
    if (!requester) {
      ctx.reply("Only players can run /tpa.");
      return;
    }
    var target = players.find(args[0]);
    if (!target || !target.isOnline()) {
      ctx.reply("Player not found: " + args[0]);
      return;
    }
    pendingTeleports[target.getUsername().toLowerCase()] = requester.getUsername();
    target.sendMessage(requester.getUsername() + " wants to teleport to you. Type /tpaaccept " + requester.getUsername() + " to accept.");
    ctx.reply("Request sent to " + target.getUsername() + ".");
  });

  // /tpaaccept <player>
  commands.register("tpaaccept", function(ctx) {
    var args = ctx.args();
    if (args.length < 1) {
      ctx.reply("Usage: /tpaaccept <player>");
      return;
    }
    var target = ctx.sender();
    if (!target) {
      ctx.reply("Only players can accept teleport requests.");
      return;
    }
    var expected = pendingTeleports[target.getUsername().toLowerCase()];
    if (!expected || expected.toLowerCase() !== args[0].toLowerCase()) {
      ctx.reply("No pending teleport request from " + args[0] + ".");
      return;
    }
    var requester = players.find(expected);
    if (!requester || !requester.isOnline()) {
      ctx.reply("Requester is no longer online.");
      delete pendingTeleports[target.getUsername().toLowerCase()];
      return;
    }
    requester.sendMessage(target.getUsername() + " accepted your teleport request. (Teleport API not wired; this is a demo.)");
    target.sendMessage("Accepted teleport request from " + requester.getUsername() + ".");
    delete pendingTeleports[target.getUsername().toLowerCase()];
  }, { description: "Accept a teleport request" });

  events.on("PlayerChat", function(evt) {
    var msg = evt.getMessage();
    if (!msg) {
      return;
    }
    var lower = msg.toLowerCase();
    for (var i = 0; i < blockedWords.length; i++) {
      if (lower.indexOf(blockedWords[i]) !== -1) {
        evt.cancel();
        evt.getPlayer().sendMessage("Your message was blocked.");
        log.warn("Blocked chat from " + evt.getPlayer().getUsername() + ": " + msg);
        return;
      }
    }
  });

  server.runLater(2000, function() {
    net.broadcast("utility-tools is active. Try /msg or /announce.");
  });
}

function onDisable() {
  log.info("utility-tools disabled");
}
