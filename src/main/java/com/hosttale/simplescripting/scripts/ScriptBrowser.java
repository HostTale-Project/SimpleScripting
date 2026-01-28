package com.hosttale.simplescripting.scripts;

import com.hosttale.simplescripting.mod.JsModManager;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Small helper for listing, reading, writing, and reloading script files under the mods directory.
 */
public final class ScriptBrowser {

    private final Path modsRoot;
    private final JsModManager modManager;
    private final HytaleLogger logger;

    public ScriptBrowser(Path modsRoot, JsModManager modManager, HytaleLogger logger) {
        this.modsRoot = modsRoot.toAbsolutePath().normalize();
        this.modManager = modManager;
        this.logger = logger.getSubLogger("script-browser");
    }

    public Path getModsRoot() {
        return modsRoot;
    }

    public List<ScriptEntry> list(String folder) {
        Path target;
        try {
            target = resolveFolder(folder);
        } catch (IllegalArgumentException e) {
            logger.atWarning().log("Invalid folder path '%s': %s", folder, e.getMessage());
            return Collections.emptyList();
        }
        if (!Files.isDirectory(target)) {
            return Collections.emptyList();
        }

        List<ScriptEntry> folders = new ArrayList<>();
        List<ScriptEntry> scripts = new ArrayList<>();

        try (Stream<Path> stream = Files.list(target)) {
            stream.forEach(path -> {
                String name = path.getFileName().toString();
                String relative = toRelative(path);
                if (Files.isDirectory(path)) {
                    folders.add(new ScriptEntry(name, relative, true));
                } else if (name.endsWith(".js")) {
                    scripts.add(new ScriptEntry(name, relative, false));
                }
            });
        } catch (IOException e) {
            logger.atWarning().log("Failed to list scripts in %s: %s", target, e.getMessage());
            return Collections.emptyList();
        }

        Comparator<ScriptEntry> byName = Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT));
        folders.sort(byName);
        scripts.sort(byName);

        List<ScriptEntry> ordered = new ArrayList<>(folders.size() + scripts.size());
        ordered.addAll(folders);
        ordered.addAll(scripts);
        return ordered;
    }

    public String read(String relativePath) throws IOException {
        Path file = resolveFile(relativePath);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("File not found: " + relativePath);
        }
        return Files.readString(file);
    }

    public void write(String relativePath, String content) throws IOException {
        Path file = resolveFile(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, Objects.requireNonNullElse(content, ""), StandardCharsets.UTF_8);
    }

    public ReloadResult reloadContainingMod(String relativePath) {
        Optional<String> modId = extractModId(relativePath);
        if (modId.isEmpty()) {
            return ReloadResult.failure("Could not determine mod id for " + relativePath);
        }
        if (!modManager.getLoadedMods().containsKey(modId.get())) {
            return ReloadResult.failure("Mod '" + modId.get() + "' is not loaded.");
        }
        boolean ok = modManager.reloadMod(modId.get());
        return ok ? ReloadResult.success(modId.get()) : ReloadResult.failure("Reload failed for mod '" + modId.get() + "'.");
    }

    public int reloadAllMods() {
        return modManager.reloadAll();
    }

    private Optional<String> extractModId(String relativePath) {
        try {
            Path resolved = resolvePath(relativePath);
            Path rel = modsRoot.relativize(resolved);
            if (rel.getNameCount() == 0) {
                return Optional.empty();
            }
            return Optional.of(rel.getName(0).toString());
        } catch (Exception e) {
            logger.atWarning().log("Failed to extract mod id from %s: %s", relativePath, e.getMessage());
            return Optional.empty();
        }
    }

    private Path resolveFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return modsRoot;
        }
        return resolvePath(folder);
    }

    private Path resolveFile(String relativePath) {
        Path resolved = resolvePath(relativePath);
        if (!resolved.getFileName().toString().endsWith(".js")) {
            throw new IllegalArgumentException("Only .js files can be viewed or edited.");
        }
        return resolved;
    }

    private Path resolvePath(String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("Path is required.");
        }
        Path candidate = modsRoot.resolve(relativePath).normalize().toAbsolutePath();
        if (!candidate.startsWith(modsRoot)) {
            throw new IllegalArgumentException("Path must stay within mods directory.");
        }
        return candidate;
    }

    private String toRelative(Path path) {
        return modsRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    public record ScriptEntry(String name, String path, boolean isFolder) {
    }

    public record ReloadResult(boolean success, String modId, String error) {
        public static ReloadResult success(String modId) {
            return new ReloadResult(true, modId, null);
        }

        public static ReloadResult failure(String error) {
            return new ReloadResult(false, null, error);
        }
    }
}
