// Example: const helpers = require("./helpers.js");

function onEnable() {
  log.info("__MOD_NAME__ enabled");
  events.on("Boot", function() {
    log.info("__MOD_NAME__ saw server boot");
  });
}

function onDisable() {
  log.info("__MOD_NAME__ disabled");
}

function onReload() {
  log.info("__MOD_NAME__ reload requested");
}

// Expose optional shared API:
// SharedServices.expose("api-name", {
//   ping: function(name) { return "pong " + name; }
// });
