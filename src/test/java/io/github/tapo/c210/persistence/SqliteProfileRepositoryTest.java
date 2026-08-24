package io.github.tapo.c210.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteProfileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsAProfileAfterReopeningTheDatabase() throws Exception {
        var databasePath = tempDir.resolve("camera.db");
        var profile = profile();

        try (var database = SqliteDatabase.open(databasePath)) {
            new SqliteProfileRepository(database).save(profile);
        }

        try (var database = SqliteDatabase.open(databasePath)) {
            assertEquals(java.util.List.of(profile), new SqliteProfileRepository(database).list());
        }
    }

    @Test
    void deletingAProfileAlsoDeletesItsPassword() throws Exception {
        try (var database = SqliteDatabase.open(tempDir.resolve("camera.db"))) {
            var repository = new SqliteProfileRepository(database);
            var secrets = new SqliteSecretStore(database);
            repository.save(profile());
            secrets.save(profile().id(), "camera-password");

            repository.delete(profile().id());

            assertTrue(repository.list().isEmpty());
            assertTrue(secrets.load(profile().id()).isEmpty());
        }
    }

    private static CameraProfile profile() {
        return new CameraProfile(
                "profile-1",
                "Tapo C210 (192.168.1.20)",
                "onvif-device-1",
                "192.168.1.20",
                2020,
                554,
                "camera-user",
                StreamQuality.HIGH,
                Instant.parse("2026-08-24T12:00:00Z"));
    }
}
