function onEnable() {
  migrateSchema();
  commands.register("database-demo:balance", balanceCommand, { description: "Show a player's coins", allowExtraArgs: true });
  commands.register("database-demo:give", giveCommand, { description: "Add coins to a player", allowExtraArgs: true });
  commands.register("database-demo:ledger", ledgerCommand, { description: "Show recent coin changes" });
  log.info("database-demo ready (storage at universe/SimpleScripting/database-demo/db/mod.sqlite)");
}

function migrateSchema() {
  db.execute("CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT)");
  var versionRow = db.queryOne("SELECT value FROM meta WHERE key = 'schema_version'");
  var version = versionRow ? Number(versionRow.value) : 0;
  if (version < 1) {
    db.execute("CREATE TABLE accounts (id TEXT PRIMARY KEY, coins INTEGER NOT NULL)");
    db.execute("CREATE TABLE ledger (id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT, target TEXT, delta INTEGER, note TEXT)");
    db.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('schema_version', '1')");
  }
}

function balanceCommand(ctx) {
  var target = ctx.args()[0] || ctx.senderName();
  var coins = getBalance(target);
  ctx.reply(target + " has " + coins + " coins.");
}

function giveCommand(ctx) {
  var args = ctx.args();
  if (args.length < 2) {
    ctx.reply("Usage: /database-demo:give <player> <amount>");
    return;
  }
  var target = args[0];
  var delta = Number(args[1]);
  if (isNaN(delta)) {
    ctx.reply("Amount must be a number.");
    return;
  }
  try {
    db.transaction(function() {
      var current = getBalance(target);
      var next = current + delta;
      db.execute(
        "INSERT INTO accounts(id, coins) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET coins = excluded.coins",
        [target, next]
      );
      db.execute(
        "INSERT INTO ledger(actor, target, delta, note) VALUES (?, ?, ?, ?)",
        [ctx.senderName(), target, delta, "give"]
      );
    });
    ctx.reply("Updated " + target + " to " + (getBalance(target)) + " coins.");
  } catch (err) {
    ctx.reply("Transaction failed: " + err);
  }
}

function ledgerCommand(ctx) {
  var rows = db.query(
    "SELECT actor, target, delta, note FROM ledger ORDER BY id DESC LIMIT 5"
  );
  if (!rows.length) {
    ctx.reply("No ledger entries yet.");
    return;
  }
  var lines = rows.map(function(row) {
    return row.actor + " -> " + row.target + " (" + row.delta + "): " + row.note;
  });
  ctx.reply(lines.join("\n"));
}

function getBalance(playerId) {
  var row = db.queryOne("SELECT coins FROM accounts WHERE id = ?", [playerId]);
  return row ? Number(row.coins) : 0;
}
