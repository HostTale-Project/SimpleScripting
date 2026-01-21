package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.mod.model.JsModManifest;
import com.hosttale.simplescripting.mod.model.JsModManifestReader;
import com.hosttale.simplescripting.mod.model.JsModManifestValidator;
import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class JsModManager {

    private final Path modsRoot;
    private final HytaleLogger logger;
    private final SharedServiceRegistry sharedServiceRegistry = new SharedServiceRegistry();
    private final Map<String, JsModInstance> loadedMods = new ConcurrentHashMap<>();

    public JsModManager(Path modsRoot, HytaleLogger logger) {
        this.modsRoot = modsRoot;
        this.logger = logger.getSubLogger("js-mods");
    }

    public void discoverAndLoadMods() {
        try {
            Files.createDirectories(modsRoot);
        } catch (IOException e) {
            logger.atSevere().log("Failed to create mods directory %s: %s", modsRoot, e.getMessage());
            return;
        }

        try (Stream<Path> paths = Files.list(modsRoot)) {
            List<JsModDefinition> definitions = paths
                    .filter(Files::isDirectory)
                    .map(this::createDefinition)
                    .flatMap(Optional::stream)
                    .toList();

            List<JsModDefinition> ordered = resolveLoadOrder(definitions);
            ordered.forEach(this::loadDefinition);
        } catch (IOException e) {
            logger.atSevere().log("Unable to list mods at %s: %s", modsRoot, e.getMessage());
        }
    }

    public boolean reloadMod(String id) {
        JsModInstance existing = loadedMods.get(id);
        if (existing == null) {
            logger.atWarning().log("Cannot reload unknown mod id '%s'.", id);
            return false;
        }
        try {
            sharedServiceRegistry.removeOwner(id);
            existing.reload();
            return true;
        } catch (IOException e) {
            logger.atSevere().log("Failed to reload mod %s: %s", id, e.getMessage());
            return false;
        }
    }

    public void disableAll() {
        loadedMods.values().forEach(instance -> {
            sharedServiceRegistry.removeOwner(instance.getDefinition().getManifest().getId());
            instance.disable();
        });
        loadedMods.clear();
    }

    public Map<String, JsModInstance> getLoadedMods() {
        return Collections.unmodifiableMap(loadedMods);
    }

    private Optional<JsModDefinition> createDefinition(Path modDir) {
        Path manifestPath = modDir.resolve("mod.json");
        if (!manifestPath.toFile().exists()) {
            logger.atInfo().log("Skipping %s (no mod.json found).", modDir.getFileName());
            return Optional.empty();
        }

        Optional<JsModManifest> manifestOpt = readManifest(manifestPath);
        if (manifestOpt.isEmpty()) {
            return Optional.empty();
        }

        JsModManifest manifest = manifestOpt.get();
        List<String> errors = JsModManifestValidator.validate(manifestPath, manifest);
        if (!errors.isEmpty()) {
            logger.atWarning().log("Rejected mod at %s: %s", modDir.getFileName(), String.join("; ", errors));
            return Optional.empty();
        }

        return Optional.of(new JsModDefinition(modDir, manifest));
    }

    private void loadDefinition(JsModDefinition definition) {
        String id = definition.getManifest().getId();
        if (loadedMods.containsKey(id)) {
            logger.atWarning().log("Duplicate mod id '%s' already loaded. Skipping %s.", id, definition.getRootDirectory().getFileName());
            return;
        }

        JsModRuntime runtime = new JsModRuntime(definition, logger, sharedServiceRegistry);
        JsModInstance instance = new JsModInstance(definition, runtime, logger, sharedServiceRegistry);
        try {
            instance.loadAndEnable();
            loadedMods.put(id, instance);
        } catch (IOException e) {
            logger.atSevere().log("Failed to load mod %s: %s", id, e.getMessage());
        }
    }

    private int compareDefinitions(JsModDefinition left, JsModDefinition right) {
        int preloadLeft = left.getManifest().isPreload() ? 0 : 1;
        int preloadRight = right.getManifest().isPreload() ? 0 : 1;
        int preloadCompare = Integer.compare(preloadLeft, preloadRight);
        if (preloadCompare != 0) {
            return preloadCompare;
        }
        return left.getManifest().getId().compareToIgnoreCase(right.getManifest().getId());
    }

    private List<JsModDefinition> resolveLoadOrder(List<JsModDefinition> definitions) {
        Map<String, JsModDefinition> byId = new HashMap<>();
        definitions.forEach(def -> byId.put(def.getManifest().getId(), def));

        Set<String> invalid = new HashSet<>();
        for (JsModDefinition def : definitions) {
            for (String dep : def.getManifest().getDependencies()) {
                if (!byId.containsKey(dep)) {
                    logger.atWarning().log("Skipping %s because dependency '%s' is missing.", def.getManifest().getId(), dep);
                    invalid.add(def.getManifest().getId());
                    break;
                }
            }
        }

        List<JsModDefinition> seeds = new ArrayList<>(definitions);
        seeds.sort(this::compareDefinitions);

        List<JsModDefinition> ordered = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (JsModDefinition def : seeds) {
            dfs(def, byId, invalid, visiting, visited, ordered);
        }

        if (!invalid.isEmpty()) {
            invalid.forEach(id -> logger.atWarning().log("Mod %s was not loaded due to dependency issues.", id));
        }

        return ordered;
    }

    private void dfs(JsModDefinition def,
                    Map<String, JsModDefinition> byId,
                    Set<String> invalid,
                    Set<String> visiting,
                    Set<String> visited,
                    List<JsModDefinition> ordered) {
        String id = def.getManifest().getId();
        if (invalid.contains(id) || visited.contains(id)) {
            return;
        }
        if (visiting.contains(id)) {
            logger.atWarning().log("Detected dependency cycle at mod %s. Skipping.", id);
            invalid.add(id);
            return;
        }

        visiting.add(id);
        List<String> deps = new ArrayList<>(def.getManifest().getDependencies());
        deps.sort(String::compareToIgnoreCase);
        for (String depId : deps) {
            JsModDefinition dep = byId.get(depId);
            if (dep == null || invalid.contains(depId)) {
                invalid.add(id);
                logger.atWarning().log("Skipping %s because dependency '%s' failed to load.", id, depId);
                visiting.remove(id);
                return;
            }
            dfs(dep, byId, invalid, visiting, visited, ordered);
            if (invalid.contains(depId)) {
                invalid.add(id);
                logger.atWarning().log("Skipping %s because dependency '%s' failed to load.", id, depId);
                visiting.remove(id);
                return;
            }
        }
        visiting.remove(id);
        visited.add(id);
        if (!invalid.contains(id)) {
            ordered.add(def);
        }
    }

    private Optional<JsModManifest> readManifest(Path manifestPath) {
        try {
            return Optional.of(JsModManifestReader.read(manifestPath));
        } catch (IOException e) {
            logger.atSevere().log("Failed to read manifest at %s: %s", manifestPath, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.atSevere().log("Unexpected error parsing manifest at %s: %s", manifestPath, e.getMessage());
            return Optional.empty();
        }
    }
}
