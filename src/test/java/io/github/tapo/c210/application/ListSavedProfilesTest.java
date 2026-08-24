package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSavedProfilesTest {
    @Test
    void returnsProfilesFromTheRepository() throws Exception {
        var profile = profile();
        var useCase = new ListSavedProfiles(new StubProfileRepository(List.of(profile)));

        assertEquals(List.of(profile), useCase.execute());
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

    private record StubProfileRepository(List<CameraProfile> profiles)
            implements CameraProfileRepository {
        @Override
        public void save(CameraProfile profile) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CameraProfile> list() throws SQLException {
            return profiles;
        }

        @Override
        public void delete(String profileId) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
