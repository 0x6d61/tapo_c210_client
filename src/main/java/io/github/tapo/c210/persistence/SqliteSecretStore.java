package io.github.tapo.c210.persistence;

import io.github.tapo.c210.application.port.SecretStore;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Persists camera passwords in the local SQLite database. */
public final class SqliteSecretStore implements SecretStore {
    private final SqliteDatabase database;

    public SqliteSecretStore(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    /** Inserts or replaces a password for an existing profile. */
    public void save(String profileId, String password) throws SQLException {
        Objects.requireNonNull(profileId, "profileId must not be null");
        Objects.requireNonNull(password, "password must not be null");
        var sql = """
                INSERT INTO camera_secrets(profile_id, password, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(profile_id) DO UPDATE SET
                    password = excluded.password,
                    updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, profileId);
            statement.setString(2, password);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    /** Loads a password when one is stored for the profile. */
    public Optional<String> load(String profileId) throws SQLException {
        Objects.requireNonNull(profileId, "profileId must not be null");
        try (PreparedStatement statement = database.connection().prepareStatement(
                "SELECT password FROM camera_secrets WHERE profile_id = ?")) {
            statement.setString(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getString(1));
                }
                return Optional.empty();
            }
        }
    }

    /** Deletes a password for the profile, if present. */
    public void delete(String profileId) throws SQLException {
        Objects.requireNonNull(profileId, "profileId must not be null");
        try (PreparedStatement statement = database.connection().prepareStatement(
                "DELETE FROM camera_secrets WHERE profile_id = ?")) {
            statement.setString(1, profileId);
            statement.executeUpdate();
        }
    }
}
