function onEnable() {
  var result = SharedServices.call("greetings", "greet", ["Traveler"]);
  if (result) {
    console.info("shared-consumer received: " + result);
  } else {
    console.warn("shared-consumer could not reach greetings service.");
  }
}

function onDisable() {
  console.info("shared-consumer: shutting down");
}
