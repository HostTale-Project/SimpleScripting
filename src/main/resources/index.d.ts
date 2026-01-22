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

declare const modManifest: ModManifest;
declare const console: ConsoleBridge;
declare const SharedServices: SharedServicesApi;

/**
 * Optional lifecycle hooks invoked by the loader if defined.
 */
declare function onEnable(): void;
declare function onDisable(): void;
declare function onReload(): void;
