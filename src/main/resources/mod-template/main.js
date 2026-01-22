function onEnable() {
  console.info("__MOD_NAME__ enabled");
}

function onDisable() {
  console.info("__MOD_NAME__ disabled");
}

function onReload() {
  console.info("__MOD_NAME__ reload requested");
}

// Expose optional shared API:
// SharedServices.expose("api-name", {
//   ping: function(name) { return "pong " + name; }
// });
