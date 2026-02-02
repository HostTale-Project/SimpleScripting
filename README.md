# SimpleScripting

Server-side JavaScript for Hytale with per-mod isolation, lifecycle hooks, and opt-in cross-mod services.

## What it does
- Loads JS mods from `<server-root>/mods/SimpleScripting/mods-js/<mod-id>/`.
- Validates `mod.json` (schema below) and reports actionable errors.
- Runs each mod in its own Rhino context/scope; hooks `onEnable/onDisable/onReload` are optional.
- Tracks shared services so mods can expose and call APIs intentionally.
- Seeds example mods on first run (`hello-world`, `shared-provider`, `shared-consumer`, `import-demo`, `utility-tools`, `status-tools`).
- Provides helpers to bootstrap mods (`/createmod <mod-id>`) and refresh types (`/updatetypes <mod-id>`).
- Injects JS facades for `server`, `events`, `commands`, `players`, `worlds`, `net`, `assets`, `ui`, and `log` so mods do not need Java imports.
- Supports `require("./other-file.js")` to split mods into multiple files within the mod folder.

## Build & install
```
./gradlew build -PhytaleHome=./.hytale-home
```
Copy `build/libs/*.jar` into your server plugins folder. On first startup the plugin creates `<server-root>/mods/SimpleScripting/mods-js/` and installs the examples.

## Mod layout
```
mods/SimpleScripting/mods-js/
  <mod-id>/
    mod.json       # manifest
    main.js        # default entrypoint (can be renamed via manifest)
```

## Manifest schema (`mod.json`)
```json
{
  "id": "my-mod",                 // required, [a-z0-9_-]+
  "name": "My Mod",               // required
  "version": "1.0.0",             // required, semver-like
  "entrypoint": "main.js",        // optional, defaults to main.js
  "preload": false,               // optional, load earlier when order is free
  "dependencies": ["other-mod"],  // optional, ensures these mods load first
  "requiredAssetPacks": [],       // optional
  "permissions": [],              // optional
  "description": "What this mod does"
}
```
Validation rejects missing required fields, bad ids, non-semver versions, `..` in entrypoint, missing entrypoint file, blank/invalid dependency names, and self-dependencies.

## Entry script shape
```javascript
function onEnable() {
  console.info("Enabled");
}

function onDisable() {
  console.info("Disabled");
}

function onReload() {
  console.info("Reload");
}
```
Hooks are optional; they are invoked if defined.

## JS API surface (no Java imports needed)
- `server`: `runLater/runRepeating`, `shutdown(reason?)`, `isBooted`, `name`.
- `events`: `on/once/off/clear`, resolves common events by name (e.g. `PlayerChat`, `BreakBlock`, `Boot`). `events.knownEvents()` lists the built-ins.
- `commands`: `register("modid:cmd", handler, { description, permission, allowExtraArgs })` returns a handle for `unregister`.
- `players`: JS wrappers only (`PlayerHandle` with username/id/language/message/kick/isOnline`); `all/find/require/names/count/message/broadcast/disconnect`.
- `worlds`: `list/get/getDefaultWorld/message/hasWorld` returning `WorldHandle` (name, players, sendMessage).
- `net`: high-level `broadcast/send/kick/warn` (no raw packets exposed).
- `assets`: placeholder wrapper (raw registry intentionally not exposed yet).
- `ui`: build colored text via `ui.raw/ui.color/ui.join`; `sendMessage/broadcast/reply` accept these objects (no native `Message` exposed).
- `economy`: multi-provider economy integration for player balances (`balance/withdraw/deposit/has/isAvailable/getName`); optional - economy features are available when VaultUnlocked and/or EliteEssentials plugin is installed separately on the server.
- `log`/`console`: info/warn/error; references are read-only so mods cannot clobber each other.
- Event payloads are wrapped (not raw Hytale objects). Example: `PlayerChat` provides `getPlayer()/getMessage()/setMessage()/cancel()`.
- Commands use the native parser under the hood; JS handlers get `CommandContext` + parsed args (command token removed) via `ctx.args()`/`ctx.rawInput()`.

### Imports between JS files
Use `require("./relative-path")` to pull another `.js` file from the same mod directory (no `..` allowed). Modules get `exports`/`module` automatically:
```javascript
// util/math.js
exports.sum = function(a, b) { return a + b; };

// main.js
const math = require("./util/math");
log.info("2 + 3 = " + math.sum(2, 3));
```

## Wrapper conventions & naming
- Methods on the injected facades use `camelCase`; tests enforce this (`WrapperConventionsTest`).
- Command ids should be namespaced: `modid:command` (auto-prefixed if missing).
- Event names use simple forms (e.g. `PlayerChat`, `BreakBlock`, `Boot`); see `events.knownEvents()`.
- IDs/paths stay within the mod folder; `require` rejects `..` traversal and absolute paths.
- Keep per-mod resources registered via the facades; disable/reload unregisters events/commands/tasks automatically.

## Shared services (opt-in cross-mod APIs)
- Expose from a mod:
  ```javascript
  SharedServices.expose("greetings", {
    greet: function(name) { return "Hello " + name; }
  });
  ```
- Consume from another mod:
  ```javascript
  var result = SharedServices.call("greetings", "greet", ["Traveler"]);
  console.info("Got: " + result);
  ```
- Declare `dependencies` in `mod.json` to ensure providers load before consumers. Services are cleared when a mod reloads/disables. Names are first-come; duplicates by other mods are rejected.

## Bundled examples
- `hello-world`: basic lifecycle logging.
- `shared-provider`: exposes a `greetings` service.
- `shared-consumer`: depends on `shared-provider` and calls its service.
- `import-demo`: shows `require("./util/math.js")` and listening for `PlayerChat`.
- `utility-tools`: `/msg`, `/ut-announce`, `/tpa`, chat filter.
- `status-tools`: `/who`, `/worldinfo`, chat `!who`, shared status service, repeating broadcasts.
Inspect them under `src/main/resources/examples/` (they are copied to `mods-js/` on first run).

## Commands
- `/createmod <mod-id>`: scaffolds a new mod folder from the template (mod.json, main.js, index.d.ts).
- `/updatetypes <mod-id>`: copies the latest `index.d.ts` into an existing mod (use after plugin upgrades).

## Debugging Hytale APIs quickly
- Jar path (Gradle cache): `~/.gradle/caches/modules-2/files-2.1/com.hypixel.hytale/hytale-server/1.0.2/<hash>/hytale-server-1.0.2.jar`
- List classes: `jar tf ~/.gradle/.../hytale-server-1.0.2.jar | rg 'server/core/plugin'`
- Inspect signatures: `javap -classpath ~/.gradle/.../hytale-server-1.0.2.jar com.hypixel.hytale.server.core.plugin.JavaPlugin`

## Notes
- Each mod is sandboxed to its own Rhino scope; globals do not leak.
- Keep names unique; duplicate mod ids are skipped.
- Shared services are intentionally opt-in; only data/functions you expose are reachable by other mods.
