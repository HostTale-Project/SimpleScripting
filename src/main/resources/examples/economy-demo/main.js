function onEnable() {
  console.info("Economy Demo mod enabled");

  if (!economy.isAvailable()) {
    console.warn("No economy provider available - economy features disabled");
    return;
  }

  console.info("Economy provider: " + economy.getName());

  // Register a command to demonstrate economy features
  commands.register("economy-demo:balance", function(ctx) {
    if (!ctx.isPlayer()) {
      ctx.reply("This command can only be used by players");
      return;
    }

    var player = ctx.sender();
    var uuid = player.getId();
    var balance = economy.balance(uuid);

    ctx.reply("Your balance: $" + balance);
  }, {
    description: "Check your economy balance"
  });

  commands.register("economy-demo:deposit", function(ctx) {
    if (!ctx.isPlayer()) {
      ctx.reply("This command can only be used by players");
      return;
    }

    var args = ctx.args();
    if (args.length < 1) {
      ctx.reply("Usage: /deposit <amount>");
      return;
    }

    var amount = parseFloat(args[0]);
    if (isNaN(amount) || amount <= 0) {
      ctx.reply("Invalid amount");
      return;
    }

    var player = ctx.sender();
    var uuid = player.getId();

    if (economy.deposit(uuid, amount)) {
      ctx.reply("Deposited $" + amount + ". New balance: $" + economy.balance(uuid));
    } else {
      ctx.reply("Failed to deposit $" + amount);
    }
  }, {
    description: "Deposit money into your account"
  });

  commands.register("economy-demo:withdraw", function(ctx) {
    if (!ctx.isPlayer()) {
      ctx.reply("This command can only be used by players");
      return;
    }

    var args = ctx.args();
    if (args.length < 1) {
      ctx.reply("Usage: /withdraw <amount>");
      return;
    }

    var amount = parseFloat(args[0]);
    if (isNaN(amount) || amount <= 0) {
      ctx.reply("Invalid amount");
      return;
    }

    var player = ctx.sender();
    var uuid = player.getId();

    if (!economy.has(uuid, amount)) {
      ctx.reply("Insufficient funds. Balance: $" + economy.balance(uuid));
      return;
    }

    if (economy.withdraw(uuid, amount)) {
      ctx.reply("Withdrew $" + amount + ". New balance: $" + economy.balance(uuid));
    } else {
      ctx.reply("Failed to withdraw $" + amount);
    }
  }, {
    description: "Withdraw money from your account"
  });
}

function onDisable() {
  console.info("Economy Demo mod disabled");
}