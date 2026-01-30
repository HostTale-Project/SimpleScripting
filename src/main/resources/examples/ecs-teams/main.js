// ECS Teams example:
// - Team A can build only at x < 0
// - Team B can build only at x > 0
// - /team a|b|none assigns/removes team components

function onEnable() {
  const TeamA = ecs.registerComponent("team_a");
  const TeamB = ecs.registerComponent("team_b");
  const teamQueue = [];

  // Command: /team a|b|none
  commands.register("team", (ctx) => {
    if (!ctx.isPlayer()) {
      ctx.reply("Players only.");
      return;
    }
    const arg = (ctx.args()[0] || "").trim().toLowerCase();
    let choice;
    if (arg.startsWith("a") || arg === "team_a") {
      choice = "a";
    } else if (arg.startsWith("b") || arg === "team_b") {
      choice = "b";
    } else if (!arg || arg === "none" || arg === "clear" || arg === "reset") {
      choice = "none";
    } else {
      ctx.reply("Usage: /team a|b|none");
      return;
    }
    const player = ctx.sender();
    const ref = player.getEntityRef();
    teamQueue.push({ ref, choice, responder: ctx.sender() });
    ctx.reply("Queuing team change to " + (choice === "none" ? "no team" : "Team " + choice.toUpperCase()) + "...");
  }, { description: "Set your team: /team a|b|none" });

  // Apply queued team changes on the store thread
  ecs.registerEntityTickingSystem({
    name: "js-team-queue",
    parallel: false,
    tick(dt, idx, chunk, store, cmd) {
      if (idx !== 0 || teamQueue.length === 0) return;
      while (teamQueue.length) {
        const job = teamQueue.shift();
        if (!job || !job.ref || !job.ref.isValid || !job.ref.isValid()) continue;
        cmd.tryRemoveComponent(job.ref, TeamA);
        cmd.tryRemoveComponent(job.ref, TeamB);
        if (job.choice === "a") {
          const compA = ecs.createComponent(TeamA);
          cmd.putComponent(job.ref, TeamA, compA);
          if (job.responder) job.responder.sendMessage("Assigned to Team A (build at x < 0).");
        } else if (job.choice === "b") {
          const compB = ecs.createComponent(TeamB);
          cmd.putComponent(job.ref, TeamB, compB);
          if (job.responder) job.responder.sendMessage("Assigned to Team B (build at x > 0).");
        } else {
          if (job.responder) job.responder.sendMessage("Cleared team.");
        }
      }
    },
  });

  // Helper to find PlayerHandle from a Ref
  function playerFromRef(ref) {
    const list = players.all();
    for (let i = 0; i < list.length; i++) {
      if (list[i].getEntityRef && list[i].getEntityRef() === ref) {
        return list[i];
      }
    }
    return null;
  }

  // Enforce build limits on block placement using ECS event hook (runs on store thread)
  ecs.registerEntityEventSystem({
    name: "js-team-build-limits",
    event: "PlaceBlockEvent",
    query: ecs.queryOr([TeamA], [TeamB]),
    handle(evt, ref, store, cmd) {
      const target = evt.getTargetBlock && evt.getTargetBlock();
      if (!target || !target.getX) return;
      const x = target.getX();

      const onA = !!store.getComponent(ref, TeamA);
      const onB = !!store.getComponent(ref, TeamB);
      if (!onA && !onB) return; // unassigned: allow

      const violatesA = onA && x >= 0;
      const violatesB = onB && x <= 0;
      if (violatesA || violatesB) {
        if (evt.setCancelled) evt.setCancelled(true);
        const p = playerFromRef(ref);
        if (p) {
          p.sendMessage(ui.color("You cannot build on this side for your team.", "#ff5555"));
          p.sendTitle("Entered enemy zone", "You cannot build here");
        }
      }
    },
  });

  // Prevent breaking on the wrong side
  ecs.registerEntityEventSystem({
    name: "js-team-break-guard",
    event: "BreakBlockEvent",
    query: ecs.queryOr([TeamA], [TeamB]),
    handle(evt, ref, store, cmd) {
      const target = evt.getTargetBlock && evt.getTargetBlock();
      if (!target || !target.getX) return;
      const x = target.getX();
      const onA = !!store.getComponent(ref, TeamA);
      const onB = !!store.getComponent(ref, TeamB);
      if (!onA && !onB) return;
      const violatesA = onA && x >= 0;
      const violatesB = onB && x <= 0;
      if (violatesA || violatesB) {
        if (evt.setCancelled) evt.setCancelled(true);
        const p = playerFromRef(ref);
        if (p) {
          p.sendMessage(ui.color("You cannot break blocks on this side for your team.", "#ff5555"));
          p.sendTitle("Entered enemy zone", "You cannot break here");
        }
      }
    },
  });

  // Prevent using blocks (interact) on the wrong side
  ecs.registerEntityEventSystem({
    name: "js-team-use-guard",
    event: "UseBlockEvent",
    query: ecs.queryOr([TeamA], [TeamB]),
    handle(evt, ref, store, cmd) {
      const target = evt.getTargetBlock && evt.getTargetBlock();
      if (!target || !target.getX) return;
      const x = target.getX();
      const onA = !!store.getComponent(ref, TeamA);
      const onB = !!store.getComponent(ref, TeamB);
      if (!onA && !onB) return;
      const violatesA = onA && x >= 0;
      const violatesB = onB && x <= 0;
      if (violatesA || violatesB) {
        if (evt.setCancelled) evt.setCancelled(true);
        const p = playerFromRef(ref);
        if (p) {
          p.sendMessage(ui.color("You cannot use blocks on this side for your team.", "#ff5555"));
          p.sendTitle("Entered enemy zone", "You cannot use blocks here");
        }
      }
    },
  });
}
