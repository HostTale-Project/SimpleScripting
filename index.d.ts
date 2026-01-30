// Type definitions for SimpleScripting JS mods

interface ModManifest {
  id: string;
  name: string;
  version: string;
  entrypoint?: string;
  preload?: boolean;
  dependencies?: string[];
  requiredAssetPacks?: string[];
  permissions?: string[];
  description?: string;
}

interface ConsoleBridge {
  info(message: string): void;
  warn(message: string): void;
  error(message: string): void;
}

interface SharedServicesApi {
  /**
   * Expose a plain JS object to other mods.
   * Returns false if the name is already claimed by another mod.
   */
  expose(name: string, apiObject: Record<string, any>): boolean;

  /**
   * Call a method on another mod's exposed service.
   * args can be a single value or an array of arguments.
   */
  call(serviceName: string, methodName: string, args?: any[] | any): any;
}

type DbValue = string | number | boolean | Uint8Array | number[] | null;

interface DbExecuteResult {
  changes: number;
  lastInsertRowid?: number;
}

interface DatabaseApi {
  execute(sql: string, params?: DbValue[]): DbExecuteResult;
  query<T = any>(sql: string, params?: DbValue[]): T[];
  queryOne<T = any>(sql: string, params?: DbValue[]): T | null;
  transaction<T>(fn: () => T): T;
}

interface StorageApi {
  db: DatabaseApi;
}

interface TaskHandle {
  id: string;
  cancel(): boolean;
  cancelled(): boolean;
}

interface ServerApi {
  name(): string;
  runLater(delayMs: number, handler: () => void): TaskHandle;
  runRepeating(initialDelayMs: number, periodMs: number, handler: () => void): TaskHandle;
  shutdown(reason?: string): void;
  isBooted(): boolean;
}

type PluginEventName =
  | "boot"
  | "shutdown"
  | "playerConnect"
  | "playerDisconnect"
  | "playerReady"
  | "playerChat"
  | "playerInteract"
  | "breakBlock"
  | "placeBlock"
  | "useBlock"
  | "allWorldsLoaded"
  | "startWorld"
  | "addWorld"
  | "removeWorld";

interface EventsApi {
  on(eventName: PluginEventName | string, handler: (event: any) => void): string;
  once(eventName: PluginEventName | string, handler: (event: any) => void): string;
  off(handle: string): void;
  clear(): void;
  knownEvents(): PluginEventName[];
}

interface PlayerChatEventWrapper {
  type: "playerChat";
  getSender(): PlayerHandle;
  getPlayer(): PlayerHandle;
  getPlayerRef(): PlayerHandle;
  getTargets(): PlayerHandle[];
  getMessage(): string;
  setMessage(message: string): void;
  isCancelled(): boolean;
  cancel(): void;
}

interface GenericEventWrapper {
  type: string;
  describe(): string;
}

interface PlayerHandle {
  getUsername(): string;
  getId(): string;
  getLanguage(): string;
  setLanguage(language: string): void;
  isOnline(): boolean;
  sendMessage(text: MessageLike): void;
  sendTitle(title: MessageLike, subtitle?: MessageLike, options?: { important?: boolean; durationSeconds?: number; fadeInSeconds?: number; fadeOutSeconds?: number; zone?: string }): void;
  hideTitle(fadeOutSeconds?: number): void;
  kick(reason?: string): void;
  getWorldName(): string;
  getEntityRef(): EntityRef;
}

interface WorldHandle {
  getName(): string;
  isLoaded(): boolean;
  players(): PlayerHandle[];
  playerNames(): string[];
  sendMessage(text: MessageLike): void;
}

interface CommandContext {
  isPlayer(): boolean;
  sender(): PlayerHandle | null;
  senderName(): string;
  args(): string[];
  rawInput(): string;
  reply(text: MessageLike): void;
}

interface CommandsApi {
  register(
    name: string,
    handler: (context: CommandContext) => void,
    options?: { description?: string; permission?: string; allowExtraArgs?: boolean }
  ): string;
  unregister(handle: string): void;
  clear(): void;
}

interface PlayersApi {
  all(): PlayerHandle[];
  names(): string[];
  find(username: string): PlayerHandle | null;
  require(username: string): PlayerHandle;
  count(): number;
  message(username: string, text: MessageLike): boolean;
  broadcast(text: MessageLike): void;
  disconnect(username: string, reason?: string): boolean;
}

interface WorldsApi {
  list(): string[];
  get(name: string): WorldHandle | null;
  getDefaultWorld(): WorldHandle | null;
  message(worldName: string, text: MessageLike): boolean;
  hasWorld(name: string): boolean;
}

interface NetApi {
  broadcast(text: MessageLike): void;
  send(username: string, text: MessageLike): boolean;
  kick(username: string, reason?: string): boolean;
  warn(message: string): void;
}

type MessageLike = string | UiText | UiMessage;

interface UiText {
  getText(): string;
  getColor(): string | null;
  color(color: string): UiText;
}

interface UiMessage {
  getParts(): UiText[];
  concat(...more: UiText[]): UiMessage;
}

interface UiApi {
  raw(text: string): UiText;
  join(...parts: MessageLike[]): UiMessage;
  color(text: string, color: string): UiText;
}

interface AssetsApi {
  info(message: string): void;
  warnUnsupported(): void;
}

// Core ECS types (opaque placeholders for typing)
type EcsEventName = "BreakBlockEvent" | "PlaceBlockEvent" | "UseBlockEvent" | "UseBlockEvent$Pre" | "UseBlockEvent$Post" | "DamageBlockEvent" | "DropItemEvent" | "DropItemEvent$PlayerRequest" | "DropItemEvent$Drop" | "InteractivelyPickupItemEvent" | "CraftRecipeEvent" | "CraftRecipeEvent$Pre" | "CraftRecipeEvent$Post" | "SwitchActiveSlotEvent" | "ChangeGameModeEvent" | "DiscoverZoneEvent" | "DiscoverZoneEvent$Display" | "ChunkSaveEvent" | "ChunkUnloadEvent" | "MoonPhaseChangeEvent";

interface EntityStore {}

type EntityRef = Ref<EntityStore>;

interface Ref<E = EntityStore> {
  isValid(): boolean;
  getStore(): Store<E> | null;
}

interface ComponentType<E = EntityStore, T = any> {}
interface ResourceType<E = EntityStore, R = any> {}

interface CommandBuffer<E = EntityStore> {
  ensureAndGetComponent<T>(ref: Ref<E>, type: ComponentType<E, T>): T;
  getComponent<T>(ref: Ref<E>, type: ComponentType<E, T>): T | null;
  putComponent<T>(ref: Ref<E>, type: ComponentType<E, T>, component: T): void;
  removeComponent<T>(ref: Ref<E>, type: ComponentType<E, T>): void;
  tryRemoveComponent<T>(ref: Ref<E>, type: ComponentType<E, T>): void;
  invoke(event: any): void;
}

interface Store<E = EntityStore> {
  getComponent<T>(ref: Ref<E>, type: ComponentType<E, T>): T | null;
  ensureAndGetComponent<T>(ref: Ref<E>, type: ComponentType<E, T>): T;
  invoke(ref: Ref<E>, event: any): void;
  invoke(event: any): void;
}

interface ArchetypeChunk<E = EntityStore> {
  getComponent<T>(index: number, type: ComponentType<E, T>): T;
  getReferenceTo(index: number): Ref<E>;
}

interface Query<E = EntityStore> {}
interface SystemGroup<E = EntityStore> {}

interface EcsEvent {}

interface Vector3Like {
  x: number;
  y: number;
  z: number;
}

interface EcsApi {
  /** Resolve a PlayerHandle or EntityRef-like value to an ECS ref; returns null on failure. */
  toRef(target: PlayerHandle | EntityRef | any): EntityRef | null;
  getPosition(target: PlayerHandle | EntityRef): Vector3Like | null;
  setPosition(target: PlayerHandle | EntityRef, pos: Vector3Like | [number, number, number] | number, commandBuffer?: any): void;
  teleport(target: PlayerHandle | EntityRef, pos: Vector3Like | [number, number, number] | number, rot: Vector3Like | [number, number, number] | number, commandBuffer?: any): void;
  getRotation(target: PlayerHandle | EntityRef): Vector3Like | null;
  setRotation(target: PlayerHandle | EntityRef, rot: Vector3Like | [number, number, number] | number, commandBuffer?: any): void;
  getHeadRotation(target: PlayerHandle | EntityRef): Vector3Like | null;
  setHeadRotation(target: PlayerHandle | EntityRef, rot: Vector3Like | [number, number, number] | number, commandBuffer?: any): void;
  getVelocity(target: PlayerHandle | EntityRef): Vector3Like | null;
  setVelocity(target: PlayerHandle | EntityRef, vel: Vector3Like | [number, number, number] | number, commandBuffer?: any): void;
  addForce(target: PlayerHandle | EntityRef, force: Vector3Like | [number, number, number] | number, commandBuffer?: any): void;
  /** Acquire a command buffer, call fn(cmd), release it. */
  withCommandBuffer(target: PlayerHandle | EntityRef | any, fn: (cmd: any) => void): void;
  invokeEntityEvent(target: PlayerHandle | EntityRef, event: any): void;
  invokeWorldEvent(event: any): void;
  spawn(world: any, components: any[], reason?: "SPAWN" | "LOAD" | string): EntityRef;
  archetype(componentTypes: any[] | any): any;
  queryAny(): any;
  queryAll(componentTypes: any[] | any): any;
  queryNot(componentTypes: any[] | any): any;
  queryOr(a: any[] | any, b: any[] | any): any;
  registerTickableSystem(options: {
    name?: string;
    group?: any;
    tick(dt: number, storeIndex: number, store: any): void;
  }): any;
  registerRunWhenPausedSystem(options: {
    name?: string;
    group?: any;
    tick(dt: number, storeIndex: number, store: any): void;
  }): any;
  registerSystemGroup(): any;
  registerSpatialResource(structure?: any): any;
  registerEntityEventSystem(options: {
    name?: string;
    event: EcsEventName | string | any;
    query?: any[] | any;
    handle(event: any, ref: EntityRef, store: any, commandBuffer: any): void;
  }): any;
  registerWorldEventSystem(options: {
    name?: string;
    event: EcsEventName | string | any;
    handle(event: any, store: any, commandBuffer: any): void;
  }): any;
  registerEntityTickingSystem(options: {
    name?: string;
    query?: any[] | any;
    parallel?: boolean;
    /** Optional system group for ordering; accepts ecs.damageGatherGroup() etc. */
    group?: any;
    tick(dt: number, entityIndex: number, chunk: any, store: any, commandBuffer: any): void;
  }): any;
  registerRefSystem(options: {
    name?: string;
    query?: any[] | any;
    onAdd?(ref: EntityRef, addReason: any, store: any, commandBuffer: any): void;
    onRemove?(ref: EntityRef, removeReason: any, store: any, commandBuffer: any): void;
  }): any;
  registerRefChangeSystem(options: {
    name?: string;
    component: any;
    onComponentAdded?(ref: EntityRef, component: any, store: any, commandBuffer: any): void;
    onComponentSet?(ref: EntityRef, oldComponent: any, newComponent: any, store: any, commandBuffer: any): void;
    onComponentRemoved?(ref: EntityRef, component: any, store: any, commandBuffer: any): void;
  }): any;
  registerComponent(id: string, supplier?: () => any): any;
  // Overload: allow calling without supplier (defaults to dynamic component).
  registerComponent(id: string): any;
  registerResource(id: string, supplier?: () => any): any;
  createComponent(type: any): any;
  /** Map of vanilla component types keyed by simple class name. */
  components(): Record<string, any>;
  /** Map of vanilla ECS event classes keyed by simple class name. */
  events(): Record<string, any>;
  /** Map of known DamageCause objects keyed by constant name and id. */
  damageCauses(): Record<string, any>;
  /** Damage helpers */
  applyDamage(target: PlayerHandle | EntityRef, options: number | { amount: number; cause?: string | any }): void;
  /** Common system groups from the Damage module (may be null if module not loaded). */
  damageGatherGroup(): any;
  damageFilterGroup(): any;
  damageInspectGroup(): any;
}

declare const modManifest: ModManifest;
declare const console: ConsoleBridge;
declare const log: ConsoleBridge;
declare const SharedServices: SharedServicesApi;
declare const db: DatabaseApi;
declare const storage: StorageApi;
declare const server: ServerApi;
declare const events: EventsApi;
declare const commands: CommandsApi;
declare const players: PlayersApi;
declare const worlds: WorldsApi;
declare const net: NetApi;
declare const ui: UiApi;
declare const assets: AssetsApi;
declare const ecs: EcsApi;
declare function require(path: string): any;

/**
 * Optional lifecycle hooks invoked by the loader if defined.
 */
declare function onEnable(): void;
declare function onDisable(): void;
declare function onReload(): void;
