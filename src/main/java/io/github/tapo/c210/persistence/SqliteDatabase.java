package io.github.tapo.c210.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Owns a SQLite connection and initializes the application schema. */
public final class SqliteDatabase implements AutoCloseable {
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final Connection connection;

    private SqliteDatabase(Connection connection) {
        this.connection = connection;
    }

    /** Opens or creates a database at {@code databasePath}. */
    public static SqliteDatabase open(Path databasePath) throws SQLException {
        Objects.requireNonNull(databasePath, "databasePath must not be null");
        createParentDirectory(databasePath);

        var connection = DriverManager.getConnection(
                "jdbc:sqlite:%s".formatted(databasePath.toAbsolutePath()));
        try {
            initialize(connection);
            return new SqliteDatabase(connection);
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    Connection connection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private static void createParentDirectory(Path databasePath) throws SQLException {
        var parent = databasePath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new SQLException("Could not create the database directory", exception);
        }
    }

    private static void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER NOT NULL
                    )
                    """);

            if (schemaVersionCount(statement) == 0) {
                statement.executeUpdate(
                        "INSERT INTO schema_version(version) VALUES (" + CURRENT_SCHEMA_VERSION + ")");
            }

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS camera_profiles (
                        id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        device_id TEXT,
                        host TEXT NOT NULL,
                        onvif_port INTEGER NOT NULL DEFAULT 2020,
                        rtsp_port INTEGER NOT NULL DEFAULT 554,
                        username TEXT NOT NULL,
                        stream_quality TEXT NOT NULL DEFAULT 'HIGH',
                        last_used_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS camera_secrets (
                        profile_id TEXT PRIMARY KEY
                            REFERENCES camera_profiles(id) ON DELETE CASCADE,
                        password TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
        }
    }

    private static int schemaVersionCount(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM schema_version")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
