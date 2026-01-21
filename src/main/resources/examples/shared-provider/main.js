SharedServices.expose("greetings", {
  greet: function(name) {
    return "Hello " + name + " from shared-provider!";
  },
  version: "1.0.0"
});

function onEnable() {
  console.info("shared-provider: greetings service ready");
}

function onDisable() {
  console.info("shared-provider: goodbye");
}
