// ECS Poison demo: adds a Poison component to a player via command and applies ticking damage.

function onEnable() {
  // Custom component stored as a simple key/value map (JsDynamicComponent under the hood).
  const Poison = ecs.registerComponent("poison");

  const poisonQueue = [];

  function createPoison(damagePerTick, tickInterval, ticksLeft) {
    const c = ecs.createComponent(Poison);
    c.set("damagePerTick", damagePerTick);
    c.set("tickInterval", tickInterval);
    c.set("ticksLeft", ticksLeft);
    c.set("elapsed", 0);
    return c;
  }

  // Command: /poison (self) or /poison <player>
  commands.register("poison", (ctx) => {
    if (!ctx.isPlayer()) {
      ctx.reply("Players only.");
      return;
    }
    const targetName = ctx.args()[0] || ctx.senderName();
    const target = players.find(targetName);
    if (!target) {
      ctx.reply("Player not found: " + targetName);
      return;
    }
    const ref = target.getEntityRef();
    if (!ref) {
      ctx.reply("No ref for player.");
      return;
    }
    poisonQueue.push({
      ref,
      damagePerTick: 3.0,
      tickInterval: 0.5,
      ticksLeft: 8,
    });
    target.sendMessage(ui.color("You have been poisoned!", "#33cc33"));
    if (ctx.sender()) {
      ctx.reply("Applied poison to " + target.getUsername());
    }
  }, { description: "Apply a poison effect to yourself or another player." });

  // System: ticks poison and applies damage. Runs in the damage gather group if available.
  ecs.registerEntityTickingSystem({
    name: "js-poison-queue",
    parallel: false,
    tick(dt, idx, chunk, store, cmd) {
      if (idx !== 0) return; // drain once per chunk tick
      if (poisonQueue.length === 0) return;
      while (poisonQueue.length > 0) {
        const job = poisonQueue.shift();
        if (!job || !job.ref || !job.ref.isValid || !job.ref.isValid()) continue;
        const poison = createPoison(job.damagePerTick, job.tickInterval, job.ticksLeft);
        cmd.putComponent(job.ref, Poison, poison);
      }
    },
  });

  ecs.registerEntityTickingSystem({
    name: "js-poison-system",
    group: ecs.damageGatherGroup() || null,
    query: [Poison],
    tick(dt, idx, chunk, store, cmd) {
      const ref = chunk.getReferenceTo(idx);
      const poison = chunk.getComponent(idx, Poison);
      if (!poison) return;

      const dmg = Number(poison.get("damagePerTick") || 0);
      const interval = Number(poison.get("tickInterval") || 1);
      let ticksLeft = Number(poison.get("ticksLeft") || 0);
      let elapsed = Number(poison.get("elapsed") || 0);

      elapsed += dt;
      if (elapsed >= interval) {
        elapsed = 0;
        ecs.applyDamage(ref, { amount: dmg, cause: "OUT_OF_WORLD" });
        ticksLeft -= 1;
      }

      poison.set("elapsed", elapsed);
      poison.set("ticksLeft", ticksLeft);

      if (ticksLeft <= 0) {
        cmd.removeComponent(ref, Poison);
      }
    },
  });
}
