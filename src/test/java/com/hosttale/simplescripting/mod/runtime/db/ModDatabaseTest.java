package com.hosttale.simplescripting.mod.runtime.db;

import com.hypixel.hytale.logger.HytaleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDatabaseTest {

    @TempDir
    Path tempDir;

    private HytaleLogger logger;

    @BeforeEach
    void setUp() {
        logger = HytaleLogger.get("db-test");
        System.setProperty("simplescripting.universePath", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("simplescripting.universePath");
    }

    @Test
    void createsDatabaseUnderUniversePath() {
        ModDatabase db = new ModDatabase("example", logger);
        try {
            db.execute("CREATE TABLE test(id INTEGER PRIMARY KEY)", List.of());
            Path expected = tempDir.resolve("SimpleScripting/example/db/mod.sqlite");
            assertTrue(Files.exists(expected));
        } finally {
            db.close();
        }
    }

    @Test
    void parameterBindingAndQueryWork() {
        ModDatabase db = new ModDatabase("params", logger);
        try {
            db.execute("CREATE TABLE test(id INTEGER PRIMARY KEY, name TEXT)", List.of());
            db.execute("INSERT INTO test(name) VALUES (?)", List.of("abc"));

            List<Map<String, Object>> rows = db.query("SELECT name FROM test WHERE id = ?", List.of(1), 0);
            assertEquals(1, rows.size());
            assertEquals("abc", rows.get(0).get("name"));
        } finally {
            db.close();
        }
    }

    @Test
    void blobRoundTripPreservesBytes() {
        ModDatabase db = new ModDatabase("blob-test", logger);
        try {
            db.execute("CREATE TABLE blobs(id INTEGER PRIMARY KEY, data BLOB)", List.of());
            byte[] payload = new byte[]{1, 2, 3, (byte) 255};
            db.execute("INSERT INTO blobs(data) VALUES (?)", List.of(payload));

            List<Map<String, Object>> rows = db.query("SELECT data FROM blobs", List.of(), 0);
            assertEquals(1, rows.size());
            assertArrayEquals(payload, (byte[]) rows.get(0).get("data"));
        } finally {
            db.close();
        }
    }

    @Test
    void transactionRollsBackOnError() {
        ModDatabase db = new ModDatabase("tx", logger);
        try {
            db.execute("CREATE TABLE tx_test(id INTEGER PRIMARY KEY, name TEXT)", List.of());
            assertThrows(IllegalStateException.class, () -> db.inTransaction(() -> {
                db.execute("INSERT INTO tx_test(name) VALUES (?)", List.of("first"));
                throw new RuntimeException("boom");
            }));

            List<Map<String, Object>> rows = db.query("SELECT * FROM tx_test", List.of(), 0);
            assertTrue(rows.isEmpty());
        } finally {
            db.close();
        }
    }

    @Test
    void sqlGuardBlocksAttach() {
        ModDatabase db = new ModDatabase("guard", logger);
        try {
            assertThrows(IllegalArgumentException.class, () -> db.execute("ATTACH DATABASE 'other' AS other", List.of()));
        } finally {
            db.close();
        }
    }

    @Test
    void rowLimitIsEnforced() {
        Path dbFile = DatabasePaths.databaseFile("rowlimit");
        ModDatabase db = new ModDatabase("rowlimit", logger, dbFile, 1, 5, 5000, 1024 * 1024);
        try {
            db.execute("CREATE TABLE row_limit_test(id INTEGER)", List.of());
            db.execute("INSERT INTO row_limit_test(id) VALUES (1)", List.of());
            db.execute("INSERT INTO row_limit_test(id) VALUES (2)", List.of());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> db.query("SELECT * FROM row_limit_test", List.of(), 1));
            assertTrue(ex.getMessage().contains("max rows (1)"));
        } finally {
            db.close();
        }
    }
}
