package com.hosttale.simplescripting.mod.model;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class JsModManifestValidator {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_-]+$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+.*$");
    private static final String ENTRYPOINT_OUTSIDE_ERROR = "Entrypoint must stay within the mod directory.";

    private JsModManifestValidator() {
    }

    public static List<String> validate(Path manifestPath, JsModManifest manifest) {
        List<String> errors = new ArrayList<>();
        Path modRoot = manifestPath.getParent();
        Path normalizedRoot = modRoot == null ? null : modRoot.toAbsolutePath().normalize();
        Path resolvedEntrypoint = null;
        if (manifest == null) {
            errors.add("Manifest is missing or could not be parsed.");
            return errors;
        }

        if (manifest.getId() == null || manifest.getId().isBlank()) {
            errors.add("Missing required field 'id'.");
        } else if (!ID_PATTERN.matcher(manifest.getId()).matches()) {
            errors.add("Invalid 'id'. Use lowercase letters, numbers, hyphens or underscores only.");
        }

        if (manifest.getName() == null || manifest.getName().isBlank()) {
            errors.add("Missing required field 'name'.");
        }

        if (manifest.getVersion() == null || manifest.getVersion().isBlank()) {
            errors.add("Missing required field 'version'.");
        } else if (!SEMVER_PATTERN.matcher(manifest.getVersion()).matches()) {
            errors.add("Version should follow semantic versioning (e.g. 1.2.3).");
        }

        String entrypointValue = manifest.getEntrypointOrDefault();
        resolvedEntrypoint = resolveEntrypoint(modRoot, normalizedRoot, entrypointValue, errors);

        if (manifest.getRequiredAssetPacks().stream().anyMatch(String::isBlank)) {
            errors.add("requiredAssetPacks contains blank entries.");
        }

        if (manifest.getPermissions().stream().anyMatch(String::isBlank)) {
            errors.add("permissions contains blank entries.");
        }

        if (manifest.getDependencies().stream().anyMatch(String::isBlank)) {
            errors.add("dependencies contains blank entries.");
        }
        if (manifest.getDependencies().stream().anyMatch(dep -> !ID_PATTERN.matcher(dep).matches())) {
            errors.add("dependencies must match id pattern (lowercase letters, numbers, hyphens, underscores).");
        }
        if (manifest.getDependencies().contains(manifest.getId())) {
            errors.add("dependencies must not include the mod's own id.");
        }

        if (errors.isEmpty() && resolvedEntrypoint != null) {
            if (!resolvedEntrypoint.toFile().exists()) {
                errors.add("Entrypoint file not found: " + entrypointValue);
            } else {
                try {
                    Path rootReal = normalizedRoot == null ? null : normalizedRoot.toRealPath();
                    Path entryReal = resolvedEntrypoint.toRealPath();
                    if (rootReal != null) {
                        ensureWithinRoot(rootReal, entryReal, errors);
                    }
                } catch (IOException e) {
                    errors.add("Entrypoint file could not be read: " + entrypointValue);
                }
            }
        }

        return errors;
    }

    private static Path resolveEntrypoint(Path modRoot, Path normalizedRoot, String entrypointValue, List<String> errors) {
        if (entrypointValue.contains("..")) {
            errors.add("Entrypoint must not contain parent directory navigation ('..').");
        }
        try {
            Path entrypointPath = Path.of(entrypointValue);
            if (entrypointPath.isAbsolute()) {
                errors.add("Entrypoint must be a relative path inside the mod directory.");
                return null;
            }
            if (normalizedRoot == null || modRoot == null) {
                return null;
            }
            Path candidate = modRoot.resolve(entrypointPath).toAbsolutePath().normalize();
            if (ensureWithinRoot(normalizedRoot, candidate, errors)) {
                return candidate;
            }
            return null;
        } catch (InvalidPathException e) {
            errors.add("Entrypoint is not a valid path: " + entrypointValue);
            return null;
        }
    }

    private static boolean ensureWithinRoot(Path root, Path candidate, List<String> errors) {
        if (!candidate.startsWith(root)) {
            if (!errors.contains(ENTRYPOINT_OUTSIDE_ERROR)) {
                errors.add(ENTRYPOINT_OUTSIDE_ERROR);
            }
            return false;
        }
        return true;
    }
}
