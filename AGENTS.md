# SimpleScripting Agent Guidelines

## Project Principles
- Treat JS mods as first-class plugins: clean lifecycle (load/enable/disable/reload), isolation, and observability by default.
- Default mod root under server: `mods/SimpleScripting/mods-js/<modId>/main.js` (entrypoint overridable via manifest).
- Prefer small, composable services; avoid god objects. Keep logs terse and actionable (who/what/why).
- Never revert user changes. Keep files ASCII unless extending existing Unicode. Comments should only clarify non-obvious logic.

## Mod Format & Loader Expectations
- `mod.json` fields: `id`, `name`, `version`, optional `entrypoint` (default `main.js`), `requiredAssetPacks` (array), `permissions` (array), optional `description`.
- Validation rules: require id/name/version; ids `a-z0-9_-`; version looks like semver; entrypoint cannot traverse (`..`); no blank entries; entrypoint file must exist.
- Loader behavior: ensure `mods-js` exists; scan subfolders; reject duplicates by `id`; log why a mod is skipped; enable immediately after load; disable everything on plugin shutdown; reload = call reload hook if present, tear down context, re-evaluate, re-enable.

## Runtime & Isolation
- One Rhino `Context` + `Scriptable` scope per mod; never share scopes. Wrap hook calls with `Context.enter(context)`/`Context.exit()`.
- Inject APIs through explicit bridges (e.g., `console`, services) instead of raw Java types. Shared services must be capability-scoped so mods cannot reach each other.
- Track registrations per mod (commands, events, tasks, timers). `disable()` must unregister/cancel everything to prevent leaks or cross-mod effects.

## Code Structure (current)
- `com.hosttale.simplescripting.mod.model`: manifest POJO, reader, validator.
- `com.hosttale.simplescripting.mod`: definition, instance, manager, validation exception.
- `com.hosttale.simplescripting.mod.runtime`: per-mod Rhino runtime and hook dispatch.
- Manager owns discovery/lifecycle; runtime owns context/scope and hook invocation; keep helpers testable and side-effect light.

## Inspecting Hytale APIs
- Jar path (Gradle cache): `~/.gradle/caches/modules-2/files-2.1/com.hypixel.hytale/hytale-server/1.0.2/<hash>/hytale-server-1.0.2.jar`.
- List packages quickly: `jar tf ~/.gradle/.../hytale-server-1.0.2.jar | rg 'server/core/plugin'`.
- Inspect signatures: `javap -classpath ~/.gradle/.../hytale-server-1.0.2.jar com.hypixel.hytale.server.core.plugin.JavaPlugin` (adjust class as needed).
- Use these before wiring new commands/events/tasks to avoid guessing signatures.

## Build & Local Checks
- Satisfy `hytaleHome` with a stub when needed: `mkdir -p .hytale-home` then `./gradlew build -PhytaleHome=./.hytale-home`.
- Prefer fast feedback: compile before large refactors; add targeted tests when feasible but remove throwaway scripts before handoff.
- When new globals/APIs are exposed to JS or signatures change, update `index.d.ts` under `src/main/resources/` (and keep the root copy in sync) so `/updatetypes` distributes the latest types to mods.
