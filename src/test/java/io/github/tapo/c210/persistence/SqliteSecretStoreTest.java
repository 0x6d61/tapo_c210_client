package io.github.tapo.c210.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteSecretStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsTheCameraPassword() throws Exception {
        try (var database = SqliteDatabase.open(tempDir.resolve("camera.db"))) {
            new SqliteProfileRepository(database).save(profile());
            var store = new SqliteSecretStore(database);

            store.save("profile-1", "camera-password");

            assertEquals(java.util.Optional.of("camera-password"), store.load("profile-1"));
        }
    }

    @Test
    void deletesTheCameraPassword() throws Exception {
        try (var database = SqliteDatabase.open(tempDir.resolve("camera.db"))) {
            new SqliteProfileRepository(database).save(profile());
            var store = new SqliteSecretStore(database);
            store.save("profile-1", "camera-password");

            store.delete("profile-1");

            assertTrue(store.load("profile-1").isEmpty());
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
