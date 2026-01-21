package com.hosttale.simplescripting.mod.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class JsModManifestValidator {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_-]+$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+.*$");

    private JsModManifestValidator() {
    }

    public static List<String> validate(Path manifestPath, JsModManifest manifest) {
        List<String> errors = new ArrayList<>();
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

        if (manifest.getEntrypointOrDefault().contains("..")) {
            errors.add("Entrypoint must not contain parent directory navigation ('..').");
        }

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

        if (errors.isEmpty()) {
            Path entrypoint = manifestPath.getParent().resolve(manifest.getEntrypointOrDefault());
            if (!entrypoint.toFile().exists()) {
                errors.add("Entrypoint file not found: " + manifest.getEntrypointOrDefault());
            }
        }

        return errors;
    }
}
