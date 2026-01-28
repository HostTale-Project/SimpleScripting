package com.hosttale.simplescripting.mod.runtime.db;

import com.hypixel.hytale.logger.HytaleLogger;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-mod SQLite handle with guardrails and lifecycle control.
 */
public final class ModDatabase implements AutoCloseable {

    private static final int DEFAULT_MAX_ROWS = intProperty("simplescripting.db.maxRows", 10_000);
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = intProperty("simplescripting.db.queryTimeoutSeconds", 5);
    private static final int DEFAULT_BUSY_TIMEOUT_MS = intProperty("simplescripting.db.busyTimeoutMs", 5000);
    private static final long DEFAULT_MAX_BYTES = longProperty("simplescripting.db.maxBytes", 50L * 1024 * 1024);

    private final String modId;
    private final Path databaseFile;
    private final HytaleLogger logger;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(() -> false);

    private final int maxRows;
    private final int queryTimeoutSeconds;
    private final int busyTimeoutMs;
    private final long maxBytes;
    private Connection connection;
    private boolean closed;

    public ModDatabase(String modId, HytaleLogger logger) {
        this(modId, logger, DatabasePaths.databaseFile(modId), DEFAULT_MAX_ROWS, DEFAULT_QUERY_TIMEOUT_SECONDS, DEFAULT_BUSY_TIMEOUT_MS, DEFAULT_MAX_BYTES);
    }

    ModDatabase(String modId,
                HytaleLogger logger,
                Path databaseFile,
                int maxRows,
                int queryTimeoutSeconds,
                int busyTimeoutMs,
                long maxBytes) {
        this.modId = modId;
        this.logger = logger.getSubLogger("db");
        this.databaseFile = databaseFile;
        this.maxRows = Math.max(1, maxRows);
        this.queryTimeoutSeconds = Math.max(1, queryTimeoutSeconds);
        this.busyTimeoutMs = Math.max(100, busyTimeoutMs);
        this.maxBytes = maxBytes;
    }

    public UpdateResult execute(String sql, List<Object> params) {
        try {
            SqlValidator.validate(sql);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mod " + modId + ": " + e.getMessage(), e);
        }
        lock.lock();
        try {
            Connection conn = ensureConnection();
            boolean startedTxn = false;
            boolean previousAutoCommit = true;
            if (!inTransaction.get()) {
                previousAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                startedTxn = true;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                configureStatement(stmt, params);
                int changes = stmt.executeUpdate();
                OptionalLong lastInsert = readLastInsertRowId(stmt);
                enforceSizeLimit();
                if (startedTxn) {
                    conn.commit();
                }
                return new UpdateResult(changes, lastInsert);
            } catch (Exception e) {
                if (startedTxn) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollback) {
                        logger.atSevere().log("Rollback failed for mod %s: %s", modId, rollback.getMessage());
                    }
                }
                throw e;
            } finally {
                if (startedTxn) {
                    try {
                        conn.setAutoCommit(previousAutoCommit);
                    } catch (SQLException reset) {
                        logger.atWarning().log("Failed to reset auto-commit for mod %s: %s", modId, reset.getMessage());
                    }
                }
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("DB error for mod " + modId + ": " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public List<Map<String, Object>> query(String sql, List<Object> params, int requestedLimit) {
        try {
            SqlValidator.validate(sql);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mod " + modId + ": " + e.getMessage(), e);
        }
        int limit = Math.min(requestedLimit <= 0 ? maxRows : requestedLimit, maxRows);
        lock.lock();
        try {
            Connection conn = ensureConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                configureStatement(stmt, params);
                stmt.setMaxRows(limit + 1);
                try (ResultSet rs = stmt.executeQuery()) {
                    return readRows(rs, limit);
                }
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("DB error for mod " + modId + ": " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public <T> T inTransaction(Callable<T> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Transaction callback is required.");
        }
        lock.lock();
        try {
            Connection conn = ensureConnection();
            if (inTransaction.get()) {
                throw new IllegalStateException("Nested transactions are not supported.");
            }
            inTransaction.set(true);
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = callback.call();
                enforceSizeLimit();
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollback) {
                    logger.atSevere().log("Rollback failed for mod %s: %s", modId, rollback.getMessage());
                }
                throw new IllegalStateException("Transaction failed for mod " + modId + ": " + e.getMessage(), e);
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException reset) {
                    logger.atWarning().log("Failed to reset auto-commit for mod %s: %s", modId, reset.getMessage());
                }
                inTransaction.remove();
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("DB error for mod " + modId + ": " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            logger.atWarning().log("Failed to close DB for mod %s: %s", modId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private Connection ensureConnection() throws SQLException, IOException {
        if (closed) {
            throw new IllegalStateException("Database for mod " + modId + " is closed.");
        }
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        Files.createDirectories(databaseFile.getParent());
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(busyTimeoutMs);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile, config.toProperties());
        return connection;
    }

    private void configureStatement(PreparedStatement stmt, List<Object> params) throws SQLException {
        stmt.setQueryTimeout(queryTimeoutSeconds);
        if (params == null || params.isEmpty()) {
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value == null) {
                stmt.setObject(i + 1, null);
            } else if (value instanceof byte[] bytes) {
                stmt.setBytes(i + 1, bytes);
            } else if (value instanceof Boolean b) {
                stmt.setBoolean(i + 1, b);
            } else if (value instanceof Number number) {
                stmt.setObject(i + 1, number);
            } else {
                stmt.setObject(i + 1, value.toString());
            }
        }
    }

    private OptionalLong readLastInsertRowId(PreparedStatement stmt) throws SQLException {
        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys != null && keys.next()) {
                long id = keys.getLong(1);
                if (id > 0) {
                    return OptionalLong.of(id);
                }
            }
        }
        try (Statement query = stmt.getConnection().createStatement();
             ResultSet rs = query.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id > 0) {
                    return OptionalLong.of(id);
                }
            }
        }
        return OptionalLong.empty();
    }

    private List<Map<String, Object>> readRows(ResultSet rs, int limit) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        while (rs.next()) {
            if (rows.size() >= limit) {
                throw new IllegalStateException("Query exceeded max rows (" + limit + "). Add LIMIT or paginate results.");
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value;
                if (meta.getColumnType(i) == Types.BLOB) {
                    value = rs.getBytes(i);
                } else {
                    value = rs.getObject(i);
                }
                String label = meta.getColumnLabel(i);
                if (label == null || label.isBlank()) {
                    label = meta.getColumnName(i);
                }
                row.put(label, value);
            }
            rows.add(row);
        }
        return rows;
    }

    private void enforceSizeLimit() throws IOException {
        if (maxBytes <= 0) {
            return;
        }
        if (!Files.exists(databaseFile)) {
            return;
        }
        long size = Files.size(databaseFile);
        if (size > maxBytes) {
            throw new IllegalStateException("Database for mod " + modId + " exceeds max size of " + maxBytes + " bytes (current " + size + ").");
        }
    }

    private static int intProperty(String key, int fallback) {
        try {
            String raw = System.getProperty(key);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long longProperty(String key, long fallback) {
        try {
            String raw = System.getProperty(key);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public record UpdateResult(int changes, OptionalLong lastInsertRowId) {
    }
}
