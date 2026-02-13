// Type definitions for SimpleScripting JS mods

interface ModManifest {
  id: string;
  name: string;
  version: string;
  entrypoint?: string;
  preload?: boolean;
  enabled?: boolean;
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

/**
 * Extension Events API - interact with events from extension plugins.
 * Extensions can emit custom events that JS mods can listen to.
 * Example events:
 *   - economy:ready (payload: provider name string)
 *   - economy:balance-changed (payload: {playerUuid, amount, type})
 */
interface ExtensionEventsApi {
  /**
   * Listen to an extension event.
   * @param eventName Event name to listen for (e.g., "economy:ready")
   * @param handler Function to call when event fires
   * @returns Handle ID for unregistering
   */
  on(eventName: string, handler: (payload: any) => void): string;

  /**
   * Emit an extension event.
   * @param eventName Event name
   * @param payload Optional event payload
   */
  emit(eventName: string, payload?: any): void;

  /**
   * Unregister an event listener.
   * @param handleId Handle ID returned by on()
   */
  off(handleId: string): void;

  /**
   * Clear all event listeners registered by this mod.
   */
  clear(): void;

  /**
   * Check if an event has any listeners.
   * @param eventName Event name to check
   * @returns true if listeners are registered
   */
  hasListeners(eventName: string): boolean;
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
  runCommand(commandLine: string): void;
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

/**
 * Base class for all entity wrappers.
 * Provides common entity reference management.
 */
interface EntityHandle {
  /** Get the underlying ECS entity reference */
  getEntityRef(): EntityRef;
  /** Check if the entity reference is still valid */
  isValid(): boolean;
  /** Get a string representation of the entity ID for logging */
  getEntityId(): string;
}

/**
 * Wrapper for LivingEntity providing inventory access capabilities.
 * Extends EntityHandle to provide common entity functionality.
 */
interface LivingEntityHandle extends EntityHandle {
  /** Get the inventory of this living entity */
  getInventory(): ItemContainerHandle | null;
  /** Check if this living entity has an inventory */
  hasInventory(): boolean;
}

/**
 * Wrapper for NPC entities.
 * Placeholder for future NPC API implementation.
 */
interface NpcHandle extends LivingEntityHandle {
  // Future methods: getDialogue(), getTrades(), getAI(), spawn(), despawn(), etc.
}

/**
 * Wrapper for player entities providing messaging, titles, and player-specific functionality.
 * Extends LivingEntityHandle to inherit inventory access capabilities.
 */
interface PlayerHandle extends LivingEntityHandle {
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
  // Inherited from LivingEntityHandle: getInventory(), hasInventory()
  // Inherited from EntityHandle: getEntityRef(), isValid(), getEntityId()
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
  /**
   * Execute a callback on the world thread.
   * Necessary for ECS operations (like ecs.getPosition) from scheduler threads.
   * @param worldName The world name, or null for default world
   * @param callback Function to execute on the world thread
   */
  runOnWorldThread(worldName: string | null, callback: () => void): void;
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
// 34 events organized by priority groups (matching EventCatalog.java)
type EcsEventName = 
  // Priority 1: Server Lifecycle (3 events)
  | "BootEvent"
  | "ServerShutdownEvent"
  | "PrepareUniverseEvent"
  // Priority 2: Inventory Events (6 events)
  | "LivingEntityInventoryChangeEvent"
  | "DropItemEvent"
  | "DropItemEvent$PlayerRequest"
  | "DropItemEvent$Drop"
  | "InteractivelyPickupItemEvent"
  | "SwitchActiveSlotEvent"
  | "CraftRecipeEvent"
  | "CraftRecipeEvent$Pre"
  | "CraftRecipeEvent$Post"
  | "PlayerCraftEvent"
  // Priority 3: Entity Events (3 events)
  | "EntityEvent"
  | "EntityRemoveEvent"
  | "LivingEntityUseBlockEvent"
  // Priority 4: Player Lifecycle (6 events)
  | "AddPlayerToWorldEvent"
  | "DrainPlayerFromWorldEvent"
  | "PlayerSetupConnectEvent"
  | "PlayerSetupDisconnectEvent"
  | "PlayerRefEvent"
  | "PlayerEvent"
  // Priority 5: Player Interaction (2 events)
  | "PlayerMouseButtonEvent"
  | "PlayerMouseMotionEvent"
  // Priority 6: Block Events (4 events)
  | "BreakBlockEvent"
  | "PlaceBlockEvent"
  | "UseBlockEvent"
  | "UseBlockEvent$Pre"
  | "UseBlockEvent$Post"
  | "DamageBlockEvent"
  // Priority 7: World Events (4 events)
  | "ChunkSaveEvent"
  | "ChunkUnloadEvent"
  | "MoonPhaseChangeEvent"
  | "WorldLoadEvent"
  // Priority 8: Permissions (3 events)
  | "PermissionEvent$CheckIndividualPermission"
  | "PermissionEvent$CheckPermissions"
  | "PermissionEvent$PermissionsChanged"
  // Priority 9: Misc (3 events)
  | "ChangeGameModeEvent"
  | "DiscoverZoneEvent"
  | "DiscoverZoneEvent$Display";

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
  invokeWorldEvent(target: PlayerHandle | EntityRef | any, event: any): void;
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

// === Inventory API (Phase 1) ===

/**
 * Handle for Hytale ItemStack.
 * Represents a stack of items with quantity, durability, and metadata.
 * Most modification methods return NEW instances - the original is never modified.
 */
interface ItemStackHandle {
  // Properties (read-only)
  readonly itemId: string;
  readonly quantity: number;
  readonly durability: number;
  readonly maxDurability: number;
  readonly broken: boolean;
  readonly unbreakable: boolean;
  readonly empty: boolean;
  readonly valid: boolean;
  readonly blockKey: string | null;

  // Metadata access
  getMetadata(): Record<string, any> | null;
  getMetadataValue(key: string): any;
  hasMetadata(key?: string): boolean;

  // Modification methods (return new instances)
  withQuantity(quantity: number): ItemStackHandle | null;
  withDurability(durability: number): ItemStackHandle;
  withIncreasedDurability(delta: number): ItemStackHandle;
  withMaxDurability(maxDurability: number): ItemStackHandle;
  withRestoredDurability(durability: number): ItemStackHandle;
  withMetadata(metadata: Record<string, any>): ItemStackHandle;
  withMetadata(key: string, value: any): ItemStackHandle;

  // Convenience methods
  damage(amount: number): ItemStackHandle;
  repair(amount: number): ItemStackHandle;
  fullyRepair(): ItemStackHandle;

  // Comparison
  isStackableWith(other: ItemStackHandle): boolean;
  isSameItemType(other: ItemStackHandle): boolean;
  isEquivalentType(other: ItemStackHandle): boolean;

  // Serialization
  toObject(): {
    itemId: string;
    quantity: number;
    durability: number;
    maxDurability: number;
    broken: boolean;
    unbreakable: boolean;
    empty: boolean;
    valid: boolean;
    blockKey?: string;
    metadata?: Record<string, any>;
  };
  toString(): string;
}

/**
 * Result of an inventory transaction operation.
 * Indicates success/failure and provides details about the operation.
 */
interface TransactionResultHandle {
  // Status
  readonly success: boolean;
  readonly message: string | null;

  // Items involved
  readonly remainder: ItemStackHandle | null;
  readonly slotBefore: ItemStackHandle | null;
  readonly slotAfter: ItemStackHandle | null;
  readonly slot: number | null;

  // String representation
  toString(): string;
}

/**
 * Handle for Hytale ItemContainer.
 * Represents a collection of item slots (hotbar, storage, armor, etc.).
 */
interface ItemContainerHandle {
  // Properties
  readonly capacity: number;
  readonly empty: boolean;

  // Slot operations
  getItem(slot: number): ItemStackHandle | null;
  setItem(slot: number, item: ItemStackHandle | null): TransactionResultHandle;
  addToSlot(slot: number, item: ItemStackHandle): TransactionResultHandle;
  removeFromSlot(slot: number): ItemStackHandle | null;
  removeFromSlot(slot: number, quantity: number): TransactionResultHandle;
  clearSlot(slot: number): TransactionResultHandle;

  // Container-wide operations
  addItem(item: ItemStackHandle): TransactionResultHandle;
  addItems(items: ItemStackHandle[]): TransactionResultHandle[];
  canAddItems(items: ItemStackHandle[]): boolean;
  removeItem(item: ItemStackHandle): TransactionResultHandle;
  canRemoveItem(item: ItemStackHandle): boolean;
  clear(): TransactionResultHandle;

  // Searching and querying
  count(predicate: (item: ItemStackHandle, slot: number) => boolean): number;
  findSlot(predicate: (item: ItemStackHandle, slot: number) => boolean): number;
  findSlots(predicate: (item: ItemStackHandle, slot: number) => boolean): number[];
  containsStackable(item: ItemStackHandle): boolean;
  contains(itemId: string, quantity?: number): boolean;
  getQuantity(itemId: string): number;
  has(itemId: string): boolean;

  // Iteration methods
  forEach(callback: (item: ItemStackHandle, slot: number) => void): void;
  map<T>(callback: (item: ItemStackHandle, slot: number) => T): T[];
  filter(predicate: (item: ItemStackHandle, slot: number) => boolean): ItemStackHandle[];
  getAll(): ItemStackHandle[];
  getAllSlots(): Record<number, ItemStackHandle>;

  // String representation
  toString(): string;
}

/**
 * Options for creating an ItemStack.
 */
interface CreateStackOptions {
  itemId: string;
  quantity?: number;
  durability?: number;
  maxDurability?: number;
  metadata?: Record<string, any>;
}

/**
 * Inventory API for creating and managing ItemStacks.
 */
interface InventoryApi {
  // Factory methods
  createStack(itemId: string): ItemStackHandle;
  createStack(itemId: string, quantity: number): ItemStackHandle;
  createStack(options: CreateStackOptions): ItemStackHandle;
  emptyStack(): ItemStackHandle;

  // Utility methods
  isEmpty(stack: ItemStackHandle | null): boolean;
  areStackable(a: ItemStackHandle | null, b: ItemStackHandle | null): boolean;
  areSameType(a: ItemStackHandle | null, b: ItemStackHandle | null): boolean;
}

declare const modManifest: ModManifest;
declare const console: ConsoleBridge;
declare const log: ConsoleBridge;
declare const SharedServices: SharedServicesApi;
/** Extension event bus - listen to and emit events from extension plugins */
declare const extensions: ExtensionEventsApi;
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
declare const inventory: InventoryApi;
declare function require(path: string): any;

/**
 * Optional lifecycle hooks invoked by the loader if defined.
 */
declare function onEnable(): void;
declare function onDisable(): void;
declare function onReload(): void;
