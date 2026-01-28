const math = require("./util/math");

function onEnable() {
  log.info("import-demo enabled; 2 + 3 = " + math.sum(2, 3));

  events.on("PlayerChat", function(event) {
    var username = event.getPlayer().getUsername();
    log.info("[import-demo] chat from " + username + " said: " + event.getMessage());
  });
}

function onDisable() {
  log.info("import-demo shutting down");
}
