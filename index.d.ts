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

interface EventsApi {
  on(eventName: string, handler: (event: any) => void): string;
  once(eventName: string, handler: (event: any) => void): string;
  off(handle: string): void;
  clear(): void;
  knownEvents(): string[];
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
  kick(reason?: string): void;
  getWorldName(): string;
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
declare function require(path: string): any;

/**
 * Optional lifecycle hooks invoked by the loader if defined.
 */
declare function onEnable(): void;
declare function onDisable(): void;
declare function onReload(): void;
