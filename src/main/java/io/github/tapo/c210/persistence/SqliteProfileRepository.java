package io.github.tapo.c210.persistence;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Persists camera profile metadata in SQLite. */
public final class SqliteProfileRepository implements CameraProfileRepository {
    private final SqliteDatabase database;

    public SqliteProfileRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    /** Inserts a profile or replaces the existing profile with the same ID. */
    public void save(CameraProfile profile) throws SQLException {
        Objects.requireNonNull(profile, "profile must not be null");
        var sql = """
                INSERT INTO camera_profiles (
                    id, display_name, device_id, host, onvif_port, rtsp_port,
                    username, stream_quality, last_used_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    display_name = excluded.display_name,
                    device_id = excluded.device_id,
                    host = excluded.host,
                    onvif_port = excluded.onvif_port,
                    rtsp_port = excluded.rtsp_port,
                    username = excluded.username,
                    stream_quality = excluded.stream_quality,
                    last_used_at = excluded.last_used_at
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, profile.id());
            statement.setString(2, profile.displayName());
            if (profile.deviceId() == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, profile.deviceId());
            }
            statement.setString(4, profile.host());
            statement.setInt(5, profile.onvifPort());
            statement.setInt(6, profile.rtspPort());
            statement.setString(7, profile.username());
            statement.setString(8, profile.streamQuality().name());
            statement.setString(9, profile.lastUsedAt().toString());
            statement.executeUpdate();
        }
    }

    /** Returns profiles in display-name order. */
    public List<CameraProfile> list() throws SQLException {
        var profiles = new ArrayList<CameraProfile>();
        var sql = """
                SELECT id, display_name, device_id, host, onvif_port, rtsp_port,
                       username, stream_quality, last_used_at
                FROM camera_profiles
                ORDER BY display_name
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                profiles.add(readProfile(resultSet));
            }
        }
        return List.copyOf(profiles);
    }

    /** Deletes a profile and its related password through the foreign-key cascade. */
    public void delete(String profileId) throws SQLException {
        Objects.requireNonNull(profileId, "profileId must not be null");
        try (PreparedStatement statement = database.connection().prepareStatement(
                "DELETE FROM camera_profiles WHERE id = ?")) {
            statement.setString(1, profileId);
            statement.executeUpdate();
        }
    }

    private static CameraProfile readProfile(ResultSet resultSet) throws SQLException {
        var deviceId = resultSet.getString("device_id");
        return new CameraProfile(
                resultSet.getString("id"),
                resultSet.getString("display_name"),
                deviceId,
                resultSet.getString("host"),
                resultSet.getInt("onvif_port"),
                resultSet.getInt("rtsp_port"),
                resultSet.getString("username"),
                StreamQuality.valueOf(resultSet.getString("stream_quality")),
                Instant.parse(resultSet.getString("last_used_at")));
    }
}
