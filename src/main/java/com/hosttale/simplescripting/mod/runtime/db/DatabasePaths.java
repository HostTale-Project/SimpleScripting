package com.hosttale.simplescripting.mod.runtime.db;

import com.hypixel.hytale.server.core.universe.Universe;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

final class DatabasePaths {

    private static final Pattern MOD_ID_PATTERN = Pattern.compile("^[a-z0-9_-]+$");
    private static final String UNIVERSE_OVERRIDE = "simplescripting.universePath";

    private DatabasePaths() {
    }

    public static Path databaseFile(String modId) {
        if (modId == null || modId.isBlank() || !MOD_ID_PATTERN.matcher(modId).matches()) {
            throw new IllegalArgumentException("Invalid mod id for database: " + modId);
        }
        Path base = resolveUniversePath();
        Path modDir = base.resolve("SimpleScripting").resolve(modId).resolve("db").normalize();
        if (!modDir.startsWith(base)) {
            throw new IllegalArgumentException("Database path escaped base directory for mod " + modId);
        }
        return modDir.resolve("mod.sqlite");
    }

    private static Path resolveUniversePath() {
        String override = System.getProperty(UNIVERSE_OVERRIDE);
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        Universe universe = Universe.get();
        if (universe != null && universe.getPath() != null) {
            return universe.getPath().toAbsolutePath().normalize();
        }
        return Paths.get("universe").toAbsolutePath().normalize();
    }
}
