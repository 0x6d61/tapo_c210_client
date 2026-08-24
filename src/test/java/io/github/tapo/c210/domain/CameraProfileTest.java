package io.github.tapo.c210.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CameraProfileTest {
    @Test
    void representsAStoredCameraConnection() {
        var lastUsedAt = Instant.parse("2026-08-24T12:00:00Z");
        var profile = new CameraProfile(
                "profile-1",
                "Tapo C210 (192.168.1.20)",
                "onvif-device-1",
                "192.168.1.20",
                2020,
                554,
                "camera-user",
                StreamQuality.HIGH,
                lastUsedAt);

        assertEquals("profile-1", profile.id());
        assertEquals(StreamQuality.HIGH, profile.streamQuality());
        assertEquals(lastUsedAt, profile.lastUsedAt());
    }
}
